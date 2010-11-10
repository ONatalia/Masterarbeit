package org.cocolab.inpro.dm.acts;

import org.cocolab.inpro.nlu.AVPair;

public abstract class AbstractDialogueAct {

	/**
	 * This dialogue act's argument structure
	 */
	ArgumentStruct arguments;
	/**
	 * Flags the 'performed' status of this dialogue act 
	 */
	boolean done;
	
	/**
	 * Gets the argument for this RNLAs Act.
	 * @return argument
	 */
	public ArgumentStruct getArgument() {
		return this.arguments;
	}

	/**
	 * Setter for whether this act has been performed.
	 * Successful only of not done already.
	 * @return success of this action.
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
	 * Attempts re-setting (replacing) the arguments of this dialogue from an AVPair.
	 * Succeeds if the current value of either argument equals the attribute of
	 * the AVPair and results in replacement by its value.
	 * @param avp the AVPair to reset from.
	 */
	public boolean setArgument(AVPair avp) {
		if (this.arguments == null)
			return false;
		return this.arguments.resetArgumentfromAVPair(avp);
	}

	/**
	 * Builds and returns a string representation of this RNLA.
	 */
	abstract public String toString();

}
