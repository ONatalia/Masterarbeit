package inpro.domains.greifarm;


/** 
 * enumeration of the different actions in our domain 
 * along with handy test operations and a reversing operation
 */
public enum ActionType {
	/** continue a move-action into the same direction */
	CONTINUE, 
	/** move to the left */
	LEFT, 
	/** move to the right */
	RIGHT, 
	/** continue a move-action into the reverse direction */
	REVERSE, 
	/** stop a motion */
	STOP, 
	/** release the load */
	DROP;
	
	boolean isMotion() {
		return this.isImplicitDirection() || this.isExplicitDirection();
	}
	
	boolean isImplicitDirection() {
		return this.equals(CONTINUE) || this.equals(REVERSE);
	}
	
	boolean isExplicitDirection() {
		return this.equals(LEFT) || this.equals(RIGHT);
	}
	
	/** return the reverse of this action, if this exists */
	ActionType reverseDirection() {
		switch (this) {
		case LEFT: return RIGHT;
		case RIGHT: return LEFT;
		default: return this;
		}
	}
}

