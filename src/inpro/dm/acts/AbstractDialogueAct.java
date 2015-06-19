package inpro.dm.acts;

public abstract class AbstractDialogueAct {

	/** Flags the 'performed' status of this dialogue act */
	protected boolean done = false;

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
