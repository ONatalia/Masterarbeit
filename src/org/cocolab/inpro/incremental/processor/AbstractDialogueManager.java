package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
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

public abstract class AbstractDialogueManager extends IUModule implements AbstractFloorTracker.Listener, AbstractActionManager.Listener {

	/** Flag for whether the DM is currently updating itself */
	boolean updating = false;
	/** Queue for left-buffer EditMessages for post-update processing */
	protected List<EditMessage<IU>> leftBufferQueue = new ArrayList<EditMessage<IU>>();
	/** Queue for performed DialogueActs for post-update processing */
	protected List<DialogueActIU> doneQueue = new ArrayList<DialogueActIU>();
	/** Queue for incoming floor signals for post-update processing */
	protected List<AbstractFloorTracker.Signal> floorSignalQueue = new ArrayList<AbstractFloorTracker.Signal>();

	/**
	 * Listens for floor changes and updates the InformationState
	 * Must call postUpdate() if calling this method.
	 */
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
		if (this.updating) {
			this.floorSignalQueue.add(signal);
		}
	}
	
	/**
	 * Queues edits for later processing.
	 * Must call postUpdate() if calling this method.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (this.updating) {
			this.leftBufferQueue.addAll((List<EditMessage<IU>>) edits);
		}	
	}

	/**
	 * Listens to an action manager.
	 * Queues all done IUs for post-update processing.
	 * Must call postUpdate() if calling this method.
	 */
	public void done(DialogueActIU iu) {
		if (this.updating) {
			this.doneQueue.add(iu);
		}
	}

	/**
	 * To call when updates are done. Implements the queue/dequeue logic.
	 * Must be called after/at end of done(), floor() and leftBufferUpdate().
	 */
	protected void postUpdate() {
		this.updating = false;
		// Work through queued items - copy current queues, apply them, repeat with any items that may have been queued in the meantime.
		List<EditMessage<IU>> localLeftBufferQueue = new ArrayList<EditMessage<IU>>(this.leftBufferQueue);
		if (!localLeftBufferQueue.isEmpty()) {
			this.leftBufferUpdate(null, this.leftBufferQueue);
			this.leftBufferQueue.removeAll(localLeftBufferQueue);
			if (!this.leftBufferQueue.isEmpty()) {
				this.postUpdate();
			}			
		}
		List<AbstractFloorTracker.Signal> localFloorSignalQueue = new ArrayList<AbstractFloorTracker.Signal>(this.floorSignalQueue);
		if (!localFloorSignalQueue.isEmpty()) {
			for (AbstractFloorTracker.Signal signal : localFloorSignalQueue) {
				this.floor(signal, null);
			}
			this.floorSignalQueue.removeAll(localFloorSignalQueue);
			if (!this.floorSignalQueue.isEmpty()) {
				this.postUpdate();
			}			
		}
		List<DialogueActIU> localDoneQueue = new ArrayList<DialogueActIU>(this.doneQueue);
		if (!localDoneQueue.isEmpty()) {
			for (DialogueActIU iu : localDoneQueue) {
				this.done(iu);
				this.doneQueue.remove(iu);
			}
			if (!this.doneQueue.isEmpty()) {
				System.err.println(this.doneQueue.toString());
				this.postUpdate();
			}			
		}
	}
	
}