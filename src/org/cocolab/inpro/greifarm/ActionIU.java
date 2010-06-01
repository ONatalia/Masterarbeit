package org.cocolab.inpro.greifarm;

import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;

public class ActionIU extends IU {
	
	private static final Logger logger = Logger.getLogger(ActionIU.class);

	private ActionType type = ActionType.STOP;
	private boolean precedesPause = false;
	// store the extent of this action
	private Modifier modifier = Modifier.NORMAL;
	/** store the start position to be able to go back later */
	/** store the planned goal position */
	protected double goalPosition;
	
	protected static GreifarmController greifarm;
	
	private ActionIU() {
		super(null, null);
	}
	
	@SuppressWarnings("unchecked")
	ActionIU(ActionIU sll, List<? extends IU> groundingWords, ActionType type, Modifier strengthModifier) {
		super(sll, (List<IU>) groundingWords);
		// we'll get this from the predecessor, it's only required for StartActionIU
		this.type = type;
		// an action may actually depend on the previous action:
		// "weiter links" should be handled just like "weiter", iff
		// "weiter" was already interpreted as direction:left and 
		// there was no *pause*/commitment between the actions 
		// we set strength to 0 for such "links"/"rechts" action; otherwise we
		// revert the "weiter" action and leave strength unchanged
		if (type.isExplicitDirection() && sll != null && !sll.precedesPause && sll.type == ActionType.CONTINUE) {
			// if sll="weiter", and this="rechts", we must make sure that it's not part of "links. weiter rechts"
			// if that is the case, we have to revert the action associated with "weiter" and 
			if (sll.realizedDirection() != type) {  
				sll.update(EditType.REVOKE);
				this.modifier = strengthModifier;
			} else {
				this.modifier = Modifier.NONE;
			}
		} else {
			this.modifier = strengthModifier;
		}
		// setup goalPosition
		switch (realizedDirection()) { 
		case LEFT: goalPosition = greifarm.getGoalPosition(-modifier.getDistance()); break;
		case RIGHT: goalPosition = greifarm.getGoalPosition(modifier.getDistance()); break;
		case DROP: // will be handled in execute() break;
		case STOP: // will be handled in execute() break;
		default: // ignore CONTINUE and REVERSE if they can't be resolved
			goalPosition = greifarm.getCurrentPosition();  
		}
		logger.debug("new action: " + toString());
//		logger.debug("with goal position " + goalPosition);
		if (groundingWords != null)
			logger.debug("delay: " + (creationTime - groundingWords.get(groundingWords.size() - 1).getCreationTime()));
		execute();
	}
	
	/* this is called upon initial execution only */
	private void execute() {
		logger.debug("executing " + this);
		switch (type) {
		case DROP: 
			greifarm.drop(); 
			break;
		case STOP: 
			greifarm.stop(); 
			this.goalPosition = greifarm.getCurrentPosition();
			break;
		default: greifarm.moveTo(goalPosition);
		}
	}
	
	/* this can potentially be called multiple times (at least for moving right/left) */
	private void reexecute() {
		logger.debug("re-executing " + this);
		switch (type) {
		case DROP: 
			greifarm.drop();
			logger.info("I'm not sure you wanted me to repeat the dropping, but that's what drop is all about.");
			break;
		case STOP: // on re-execution, stopping means to go back to where we originally stopped 
		default: greifarm.moveTo(goalPosition);
		}
		
	}
	
	// this tries to resolve CONTINUE and REVERSE, leaving only explicit directions
	public ActionType realizedDirection() {
		if (type.isImplicitDirection()) {
			if (sameLevelLink != null) {
				if (type == ActionType.CONTINUE) {
					return predecessor().realizedDirection();
				} else { // REVERSE
					return predecessor().realizedDirection().reverseDirection();
				}
			} 
		} else if (type == ActionType.STOP) {
			if (sameLevelLink != null) {
				return predecessor().realizedDirection();
			}
		}
		return type;
	}
	
	private ActionIU predecessor() {
		return (ActionIU) sameLevelLink;
	}
	
	public ActionType getType() {
		return type;
	}
	
	public boolean isWeak() {
		return modifier == Modifier.WEAK || modifier == Modifier.NONE;
	}
	
	public void precedesPause(boolean precedesPause) {
	//	logger.debug(toString() + " now precedes pause: " + precedesPause);
		this.precedesPause = precedesPause;
	}
	
	@Override
	public String toPayLoad() {
		return "Action " + type + " with strength " + modifier;
	}
	
	@Override
	public void update(EditType edit) {
		if (edit == EditType.REVOKE) {
			// TODO: revert the action
			logger.debug("reverted: " + toString());
			switch (type) {
			case DROP: 
				logger.info("I cannot revert dropping. Sorry for that..."); 
				break;
			case LEFT: // reverting any of the move actions means going back to where we started from
			case RIGHT:
			case CONTINUE:
			case REVERSE:
			case STOP: // reverting a stop action means going back to what we previously wanted
				if (predecessor() != null) // && !predecessor().precedesPause)
					predecessor().reexecute(); 
				break;
			default: 
				// TODO/FIXME: revocation of other actions are ignored for now
			}
		} else {
			logger.debug("ignoring edit " + edit);
		}
	}

	protected static class StartActionIU extends ActionIU {

		StartActionIU(GreifarmController greifarm) {
			super();
			ActionIU.greifarm = greifarm;
			this.goalPosition = greifarm.getCurrentPosition();
		}
		
		@Override
		public ActionType getType() {
			return ActionType.STOP;
		}
	}

}

