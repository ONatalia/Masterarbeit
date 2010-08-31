package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

/**
 * An abstract DM. Has basic provisions for action management and floor tracking.
 * @author timo, okko
 */

public class AbstractDialogueManager extends IUModule implements AbstractFloorTracker.Listener, AbstractActionManager.Listener {

	/**
	 * Calculates changes from the previous SemIU and updates the InformationState.
	 */
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
	}

	/**
	 * Listens for floor changes and updates the InformationState
	 */
	@Override
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {}

	/**
	 * Listens to an action manager.
	 */
	@Override
	public void done(DialogueActIU iu) {}

}