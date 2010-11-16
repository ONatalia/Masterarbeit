package org.cocolab.inpro.incremental.processor;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.dm.isu.IUNetworkUpdateEngine;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;

public class NumbersDialogueManager extends AbstractDialogueManager implements AbstractFloorTracker.Listener, AbstractActionManager.Listener {

	/** The ActionManager component configuration variables */
	@S4Component(type = AudioActionManager.class, mandatory = true)
	public static final String PROP_AM = "actionManager";
	/** The ActionManager component */
	private AudioActionManager am;
	/** The lexical semantics mapping configuration variables */
	@S4String(mandatory = true)
	public final static String PROP_LEX_SEMANTICS = "lexicalSemantics";

	/**
	 * Update Engine holding rules, information state and interfaces
	 * for new input/output IUs.
	 */
	private IUNetworkUpdateEngine updateEngine = new IUNetworkUpdateEngine();

	/**
	 * Sets up the DM.
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		this.am = (AudioActionManager) ps.getComponent(PROP_AM);
		String lexicalSemanticsPath = ps.getString(PROP_LEX_SEMANTICS);
		try {
			WordIU.setAVPairs(AVPairMappingUtil.readAVPairs(new URL(lexicalSemanticsPath)));
		} catch (IOException e) {
			e.printStackTrace();
			logger.fatal("Could not set WordIU's AVPairs from file " + lexicalSemanticsPath);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		System.err.println(ius.toString());
		System.err.println(edits.toString());
//		super.leftBufferUpdate(ius, edits);
//		if (this.updating) {
//			return;
//		}
//		this.updating = true;
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
			case COMMIT: break; //TODO: maybe commit all grin IUs on the IS?
			}
		}
		IUList<DialogueActIU> newOutput = new IUList<DialogueActIU>();
		newOutput.addAll(this.updateEngine.getNewOutputAndClear());
		System.err.println("AM should now be performing output:");
		System.err.println(newOutput.toString());
//		super.postUpdate();
	}

	public void done(DialogueActIU iu) {
//		super.done(iu);
//		if (this.updating)
//			return;
//		super.postUpdate();
	}

	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
//		super.floor(signal, floorManager);
//		if (this.updating)
//			return;
//		switch(signal){
//		case START: {
//			this.am.shutUp();
//		}
//		}
//		super.postUpdate();
	}

}
