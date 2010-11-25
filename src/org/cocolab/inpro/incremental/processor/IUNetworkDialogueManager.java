package org.cocolab.inpro.incremental.processor;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.dm.isu.IUNetworkDomainUtil;
import org.cocolab.inpro.dm.isu.IUNetworkUpdateEngine;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.ContribIU;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

// TODO: try what-from-to-on-at agenda IU dialogue without clarifications.

public class IUNetworkDialogueManager extends AbstractDialogueManager implements AudioActionManager.Listener {

	/** The lexical semantics mapping configuration variables */
	@S4String(mandatory = true)
	public final static String PROP_LEX_SEMANTICS = "lexicalSemantics";
	/** The internal state listener configuration */
	@S4ComponentList(type = IUModule.class)
	public final static String PROP_STATE_LISTENERS = "stateListeners";
	protected List<IUModule> stateListeners;
	@S4Component(type = IUNetworkDomainUtil.class)
	public final static String PROP_DOMAIN = "domain";
	protected IUNetworkDomainUtil domain;

	
	/**
	 * Update Engine holding rules, information state and interfaces
	 * for new input/output IUs.
	 */
	private IUNetworkUpdateEngine updateEngine;

	/**
	 * Sets up the DM with lexical semantics and state listeners.
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String lexicalSemanticsPath = ps.getString(PROP_LEX_SEMANTICS);
		try {
			WordIU.setAVPairs(AVPairMappingUtil.readAVPairs(new URL(lexicalSemanticsPath)));
		} catch (IOException e) {
			e.printStackTrace();
			logger.fatal("Could not set WordIU's AVPairs from file " + lexicalSemanticsPath);
		}
		this.domain = (IUNetworkDomainUtil) ps.getComponent(PROP_DOMAIN);
		this.stateListeners = ps.getComponentList(PROP_STATE_LISTENERS, IUModule.class);
		this.updateEngine = new IUNetworkUpdateEngine(this.domain);
		this.logToTedView("Initial State:\n" + this.updateEngine.getInformationState().toString());
		logger.info(this.updateEngine.getInformationState().toString());
	}

	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		super.leftBufferUpdate(ius, edits);
		if (this.updating)
			return;
		this.updating = true;
		for (EditMessage<? extends IU> edit : edits) {
			switch (edit.getType()) {
			case ADD: {
				// Just apply the rules with each new word
				this.updateEngine.applyRules((WordIU) edit.getIU());
				break;
			}
			case REVOKE: {
				// Ditto, but make sure to revoke word first
				edit.getIU().revoke();
				this.updateEngine.applyRules((WordIU) edit.getIU());
				break;
			}
			case COMMIT: break; //TODO: commit all grin IUs on the IS?
			}
		}
		this.postUpdate();
	}
	
	/**
	 * Update state listeners and right buffer, then call
	 * super.postUpdate() to release locks. 
	 */
	protected void postUpdate() {
		this.rightBuffer.setBuffer(this.updateEngine.getOutput());
		this.logToTedView("New State:\n" + this.updateEngine.getInformationState().toString());
		IUList<ContribIU> newList = new IUList<ContribIU>();
		List<EditMessage<ContribIU>> contributionEdits = this.updateEngine.getInformationState().getContributions().diff(newList);
		contributionEdits = newList.diff(this.updateEngine.getInformationState().getContributions());
		for (IUModule listener : this.stateListeners) {
			listener.hypChange(this.updateEngine.getInformationState().getContributions(), contributionEdits);
		}
		super.postUpdate();
	}

	public void done(DialogueActIU iu) {
		super.done(iu);
		if (this.updating)
			return;
		this.updating = true;
		this.logToTedView("New State:\n" + this.updateEngine.getInformationState().toString());
		super.postUpdate();
	}

}