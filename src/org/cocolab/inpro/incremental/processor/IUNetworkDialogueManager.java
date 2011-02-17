package org.cocolab.inpro.incremental.processor;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.dm.isu.IUNetworkDomainUtil;
import org.cocolab.inpro.dm.isu.IUNetworkUpdateEngine;
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.ContribIU;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

public class IUNetworkDialogueManager extends AbstractDialogueManager implements AudioActionManager.Listener {

	/** The lexical semantics mapping configuration variables */
	@S4String(mandatory = true)
	/** The lexical semantics configuration property string */
	public final static String PROP_LEX_SEMANTICS = "lexicalSemantics";
	/** The internal state listener configuration */
	@S4ComponentList(type = PushBuffer.class)
	/** The state listeners configuration property string */
	public final static String PROP_STATE_LISTENERS = "stateListeners";
	/** The list of state listeners */
	protected List<PushBuffer> stateListeners;
	/** The domain utility configuration */
	@S4Component(type = IUNetworkDomainUtil.class)
	/** The domain utility configuration property string */
	public final static String PROP_DOMAIN = "domain";
	/** The domain utility wrapping information state creation */
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
		this.stateListeners = ps.getComponentList(PROP_STATE_LISTENERS, PushBuffer.class);
		this.updateEngine = new IUNetworkUpdateEngine(this.domain);
		this.updateEngine.setLogger(this.logger);
		this.updateAll();
		logger.info("DM started");
	}

	/**
	 * Updates the IS with ADDed or REVOKEd WordIUs.
	 * Calls {@link updateAll()} to update listeners.
	 */
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		super.leftBufferUpdate(ius, edits);
		if (this.updating)
			return;
		this.updating = true;
		boolean reset = false;
		for (EditMessage<? extends IU> edit : edits) {
			if (!((WordIU) edit.getIU()).isSilence()) {
				switch (edit.getType()) {
				case ADD: {
					// Just apply the rules with each new word that isn't a hard reset)
					if (((WordIU) edit.getIU()).getAVPairs() != null && 
					!((WordIU) edit.getIU()).getAVPairs().get(0).equals(new AVPair("reset:true"))) {
						this.updateEngine.applyRules((WordIU) edit.getIU());
					}
					break;
				}
				case REVOKE: {
					// Ditto, but make sure to revoke word first
					edit.getIU().revoke();
					this.updateEngine.applyRules((WordIU) edit.getIU());
					break;
				}
				case COMMIT: {
					if (((WordIU) edit.getIU()).getAVPairs() != null && 
							((WordIU) edit.getIU()).getAVPairs().get(0).equals(new AVPair("reset:true"))) {
						reset = true;
					}
					break;
				}
				}
			}
		}
		if (reset)
			this.reset();
		this.updateAll();
	}
	
	/**
	 * Update state listeners and right buffer, then call
	 * {@link super.postUpdate()} to release locks. 
	 */
	protected void updateAll() {
		this.updateStateListeners();
		this.logToTedView("State:\n" + this.updateEngine.getInformationState().toString());
		this.rightBuffer.setBuffer(this.updateEngine.getNewEdits());
		super.postUpdate();
	}
	
	/**
	 * Updates internal state listeners.
	 */
	private void updateStateListeners() {
		IUList<ContribIU> newList = new IUList<ContribIU>();
		List<EditMessage<ContribIU>> contributionEdits = this.updateEngine.getInformationState().getContributions().diff(newList);
		contributionEdits = newList.diff(this.updateEngine.getInformationState().getContributions());
		for (PushBuffer listener : this.stateListeners) {
			listener.hypChange(this.updateEngine.getInformationState().getContributions(), contributionEdits);
		}
	}

	public void reset() {
		logger.info("Resetting DM.");
		super.postUpdate();
		this.leftBufferQueue.clear();
		this.updateEngine = new IUNetworkUpdateEngine(this.domain);
		this.updateEngine.setLogger(this.logger);
		for (PushBuffer listener : this.iulisteners) {
			listener.reset();
		}
	}

	public void done(DialogueActIU iu) {}

}
