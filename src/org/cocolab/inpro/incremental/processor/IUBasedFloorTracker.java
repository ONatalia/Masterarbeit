package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Integer;

public class IUBasedFloorTracker extends AbstractFloorTracker {
	
	@S4Integer(defaultValue = 0)
	public final static String PROP_RISING_TIMEOUT = "risingTimeout";
	private int risingTimeout;
	
	@S4Integer(defaultValue = 700)
	public final static String PROP_ANY_TIMEOUT = "anyProsodyTimeout";
	private int anyProsodyTimeout;
	
	@S4Boolean(defaultValue = true)
	public final static String PROP_USE_PROSODY = "useProsody";
	private boolean useProsody;
	
	private static Logger logger = Logger.getLogger(IUBasedFloorTracker.class);
	
	TimeOutThread timeOutThread;

	/* @see org.cocolab.inpro.incremental.processor.AbstractFloorTracker#newProperties(edu.cmu.sphinx.util.props.PropertySheet) */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		risingTimeout = ps.getInt(PROP_RISING_TIMEOUT);
		anyProsodyTimeout = ps.getInt(PROP_ANY_TIMEOUT);
		useProsody = ps.getBoolean(PROP_USE_PROSODY);
	}


	/* (non-Javadoc)
	 * @see org.cocolab.inpro.incremental.processor.AbstractFloorTracker#hypChange(java.util.Collection, java.util.List)
	 */
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && edits.size() > 0) {
			logger.debug(edits);			
			if (edits.get(0).getType() == EditType.ADD){
				logger.debug("found an add -- this means that any timeout thread should be killed");
				if (timeOutThread != null)
					timeOutThread.killbit = true;
				timeOutThread = null;
			}
			if (edits.get(edits.size() - 1).getType() == EditType.COMMIT) {
				logger.debug("found a commit!");
				assert timeOutThread == null : "There shall be no timeout threads during speech";
				@SuppressWarnings("unchecked")
				List<WordIU> words = (List<WordIU>) ius;
				WordIU endingWord = words.get(words.size() - 1);
				while (endingWord.isSilence()) {
					endingWord = (WordIU) endingWord.getSameLevelLink();
				}
				if (endingWord != null) {
					timeOutThread = new TimeOutThread(endingWord);
					timeOutThread.start();
				}
			}
		}
	}

	
	private class TimeOutThread extends Thread {

		/** if set, this timeout thread will not do anything when its timer runs out */
		boolean killbit = false;
		
		WordIU endingWord;
		
		public TimeOutThread(WordIU endingWord) {
			this.endingWord = endingWord;
		}

		private void sleepSafely(long timeout) {
			if (timeout < 0)
				return;
			logger.debug("going to sleep for " + timeout + " milliseconds");
			try {
				Thread.sleep(risingTimeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.debug("waking up after " + timeout + " milliseconds of sleep");
		}
		
		private boolean shouldDie() {
			if (killbit) {
				logger.debug("killed during timeout");
				return true;
			}
			assert timeOutThread == this : "I'm running without a killbit, even though floortracker doesn't reference me";
			return false;
		}

		private Signal findBoundaryTone() {
			// do something with the following: 
			logger.debug("turnFinalWord is " + endingWord);
			if (endingWord.hasProsody()) {
				return endingWord.pitchIsRising() ?  Signal.EOT_RISING : Signal.EOT_FALLING;
			}
			return Signal.EOT_ANY;
		}
		
		@Override
		public void run() {
			sleepSafely(risingTimeout);
			if (shouldDie())
				return;
			if (useProsody) {
				Signal s = findBoundaryTone();
				if (s == Signal.EOT_RISING) {
					signalListeners(InternalState.NOT_AWAITING_INPUT, s);
					return;
				}
			}
			sleepSafely(anyProsodyTimeout);
			if (shouldDie())
				return;
			Signal s = findBoundaryTone();
			signalListeners(InternalState.NOT_AWAITING_INPUT, s);
		}
		
	}

}
