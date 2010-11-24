package org.cocolab.inpro.dm.acts;

public abstract class AbstractDialogueAct {

	/** Flags the 'performed' status of this dialogue act */
	protected boolean done = false;

	/**
	 * Setter for whether this act has been performed.
	 * Successful only if not done already.
	 * @return success of this method
	 */
	public boolean doThis() {
		if (this.done) {
			return false;
		}
		this.done = true;
		return this.done;
	}

	/**
	 * Setter to undo this act.
	 * Successful only if done already.
	 * @return success of this method
	 */
	public boolean undoThis() {
		if (this.done) {
			this.done = false;
			return true;
		}
		return false;
	}

	/**
	 * Returns whether this act has been performed.
	 * @return true if so
	 */
	public boolean isDone() {
		return this.done;
	}
	
	/**
	 * Builds and returns a string representation of this dialogue act.
	 */
	abstract public String toString();

}
