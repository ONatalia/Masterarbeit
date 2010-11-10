package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.dm.PentoDialogueAct;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class AudioActionManager extends AbstractActionManager {

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		logger.info("Setting audio path to " + this.audioPath);
		PentoDialogueAct.setPath(this.audioPath);
		logger.info("Started AudioActionManager");
	}

	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<? extends IU> edit : edits) {
			DialogueActIU iu = (DialogueActIU) edit.getIU();
			PentoDialogueAct r = iu.getAct();
			switch (edit.getType()) {
				case REVOKE: 
					break;
				case ADD: 
					assert (r.getAct() == PentoDialogueAct.Act.PROMPT) : "I can only play prompts, don't make me do anything else.";
					if (r.doThis()) {
						logger.info("Speaking: " + r.toString());
						r.perform(null, null, this.audioDispatcher);
						this.signalListeners(iu);
					}
					break;
				case COMMIT: 
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
	}
	
}
