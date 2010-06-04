package org.cocolab.inpro.greifarm;

import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;

/** this encapsulates natural language understanding */
public class NLU {

	private final Logger logger = Logger.getLogger(NLU.class);
	private final Deque<WordIU> unusedWords;
	private final Deque<ActionIU> performedActions;

	public NLU(Deque<WordIU> unusedWords, Deque<ActionIU> performedActions) {
		this.unusedWords = unusedWords;
		this.performedActions = performedActions;
	}
	
	/**
	 * after each hypothesis, we try to find one (or more) "commands",
	 * which we define as "(contentlessWord | modifierWord)* actionWord",
	 * i.e. a sequence of words leading up to and including an actionWord.
	 * the action depends on the actionWord's ActionType, and
	 * the *last* modifierWord's ActionStrength
	 */
	public void incrementallyUnderstandUnusedWords() {
		//logger.debug(unusedWords);
		ActionStrength strengthModifier = ActionStrength.NORMAL;
		List<WordIU> groundingWords = new ArrayList<WordIU>();
		for (Iterator<WordIU> it = unusedWords.iterator(); it.hasNext();) {
			WordIU word = it.next();
			// get all the words leading up to the current words
			groundingWords.add(word);
			if (isActionWord(word)) {
				logger.debug("found action word " + word.getWord());
				logger.debug("all words contained in this action: " + groundingWords);
				logger.debug("action type is " + actionType(word));
				// get SLL (which is also relevant to determine the ActionIU's behaviour
				ActionIU sll = performedActions.peekLast(); // may be null
				// create new Action IU depending on actionType(iu), previous actions
				// and directionModifier.
				ActionIU action = new ActionIU(sll, groundingWords, actionType(word), strengthModifier);
				groundingWords = new ArrayList<WordIU>();
				performedActions.addLast(action);
				strengthModifier = ActionStrength.NORMAL; // reset to NORMAL for a possibly following action
			} else if (isModifierWord(word)) {
				strengthModifier = getModifierValue(word, strengthModifier);
				logger.debug("set strength to " + strengthModifier + " due to " + word.getWord());
			} else {
				// ignore all other words
			}
		}
	}

	/** 
	 * on commit, we try a little harder to interpret any remaining words of the utterance:
	 * if no explicit action word is available, we simply assume that CONTINUE was meant
	 * and interpret only the WEAK, NORMAL, and STRONG modifiers;;
	 * also, and only on commit, we execute a MAX action, if there have been
	 * at least three consecutive NORMAL or STRONG actions
	 */
	public void understandUnusedWordsOnCommit() {
		ActionStrength strengthModifier = ActionStrength.NONE;
		if (unusedWords.size() > 0) {
			for (Iterator<WordIU> it = unusedWords.iterator(); it.hasNext();) {
				WordIU iu = it.next();
				if (isModifierWord(iu)) {
					strengthModifier = getModifierValue(iu, strengthModifier);
					logger.debug("set strength to " + strengthModifier + " due to " + iu.getWord());
				}
			}
			if (strengthModifier.isNormalDistance()) {
				ActionIU sll = performedActions.peekLast();
				ActionIU action = new ActionIU(sll, new ArrayList<WordIU>(unusedWords), ActionType.CONTINUE, strengthModifier);
				performedActions.addLast(action);
				unusedWords.clear();
			}
		}
		addRepetitiveMaxAction();
	}
	
	private boolean isActionWord(WordIU iu) {
		return iu.getAVPairs() != null 
			&& iu.getAVPairs().size() > 0 
			&& iu.getAVPairs().get(0) != null
			&& iu.getAVPairs().get(0).getAttribute() != null
			&& iu.getAVPairs().get(0).getAttribute().equals("act");
	}
	
	private boolean isModifierWord(WordIU iu) {
		return iu.getAVPairs() != null 
		&& iu.getAVPairs().size() > 0 
		&& iu.getAVPairs().get(0) != null
		&& iu.getAVPairs().get(0).getAttribute() != null
		&& iu.getAVPairs().get(0).getAttribute().equals("modifier");
	}
	
	/** 
	 * if people say a direction many times in a row, we initiate
	 * a MAX-action in that direction, in order to speed things up
	 * (and to force them to use the STOP command more often... )  
	 */
	private void addRepetitiveMaxAction() {
		final int CONSECUTIVE_ACTIONS = 3;
		ActionIU ultimateAction = performedActions.getLast();
		if (ultimateAction.isWeak()) {
			logger.debug("previous action was weak, not adding max action");
			return;
		}
		ActionType ultimateDirection = ultimateAction.realizedDirection();
		ActionIU actionIterator = ultimateAction;
		int counter = 0;
		while (actionIterator != null && // check that there was an action
			actionIterator.getType().isMotion() &&
			!actionIterator.isWeak() &&
			actionIterator.realizedDirection().isExplicitDirection() && // check that it was a directional action
			(actionIterator.realizedDirection() == ultimateDirection) && // check that the direction is right
			counter < CONSECUTIVE_ACTIONS) // no need to check beyond 3 
		{
			actionIterator = (ActionIU) actionIterator.getSameLevelLink();
			counter++;
		}
		if (counter >= 3) {
			logger.debug("adding max action");
			ActionIU action = new ActionIU(ultimateAction, null, ActionType.CONTINUE, ActionStrength.MAX);
			performedActions.addLast(action);	
		}
	}
	
	/** 
	 * we only interpret the very last modifier word; that is, things like
	 * "sehr weit" are handled just like "weit"
	 * 
	 * "ganz weit nach" should really be interpreted like "ganz nach", not like "weit nach"
	 * at the same time, "ganz bisschen" *should* be interpreted like "bisschen"
	 * in other words: max followed by strong should remain strong.
	 * @param word the word to be interpreted
	 * @param strengthModifier the previous modifier value
	 */
	private ActionStrength getModifierValue(WordIU word, ActionStrength strengthModifier) {
		assert isModifierWord(word);
		AVPair sem = word.getAVPairs().get(0);
		if (sem.getValue().equals("weak")) return ActionStrength.WEAK;
		if (sem.getValue().equals("regular")) return ActionStrength.NORMAL;
		if (sem.getValue().equals("strong"))
			return (strengthModifier == ActionStrength.MAX) ? ActionStrength.MAX : ActionStrength.STRONG; 
		if (sem.getValue().equals("max")) return ActionStrength.MAX; 
		throw new RuntimeException("panic: " + sem.getValue());
	}

	private ActionType actionType(WordIU iu) {
		assert isActionWord(iu);
		AVPair sem = iu.getAVPairs().get(0);
		if (sem.getValue().equals("left")) return ActionType.LEFT;
		if (sem.getValue().equals("right")) return ActionType.RIGHT;
		if (sem.getValue().equals("stop")) return ActionType.STOP;
		if (sem.getValue().equals("drop")) return ActionType.DROP;
		if (sem.getValue().equals("reverse")) return ActionType.REVERSE;
		if (sem.getValue().equals("continue")) return ActionType.CONTINUE;
		throw new RuntimeException("panic: " + sem.getValue());
	}
}



