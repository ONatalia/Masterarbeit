package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

public class IUBasedFloorTracker extends AbstractFloorTracker {
	
	private static Logger logger = Logger.getLogger(IUBasedFloorTracker.class);
	
	TimeOutThread timeOutThread;

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
				assert timeOutThread == null;
				@SuppressWarnings("unchecked")
				List<WordIU> words = (List<WordIU>) ius;
				final WordIU endingWord = words.get(words.size() - 1);
				timeOutThread = new TimeOutThread(endingWord);
				timeOutThread.start();
			}
		}
	}

	
	private class TimeOutThread extends Thread {

		private static final int FIRST_TIME_OUT = 200;
		private static final int SECOND_TIME_OUT = 200;
		private static final int THIRD_TIME_OUT = 300;
		
		/** if set, this timeout thread will not do anything when its timer runs out */
		boolean killbit = false;
		
		WordIU endingWord;
		
		public TimeOutThread(WordIU endingWord) {
			this.endingWord = endingWord;
		}

		private void sleepSafely(long timeout) {
			logger.debug("going to sleep for " + timeout + " milliseconds");
			try {
				Thread.sleep(FIRST_TIME_OUT);
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
			sleepSafely(FIRST_TIME_OUT);
			if (shouldDie())
				return;
			Signal s = findBoundaryTone();
			if (s == Signal.EOT_RISING) {
				signalListeners(InternalState.NOT_AWAITING_INPUT, s);
				return;
			}
			sleepSafely(SECOND_TIME_OUT);
			if (shouldDie())
				return;
			s = findBoundaryTone();
			if (s == Signal.EOT_RISING || s == Signal.EOT_FALLING) {
				signalListeners(InternalState.NOT_AWAITING_INPUT, s);
				return;
			}
			sleepSafely(THIRD_TIME_OUT);
			if (shouldDie())
				return;
			s = findBoundaryTone();
			signalListeners(InternalState.NOT_AWAITING_INPUT, s);
		}
		
	}

}
