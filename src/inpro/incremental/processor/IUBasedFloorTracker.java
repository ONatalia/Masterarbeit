package inpro.incremental.processor;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.util.ResultUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


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
	
	TimeOutThread timeOutThread;

	/** @see inpro.incremental.processor.AbstractFloorTracker#newProperties(edu.cmu.sphinx.util.props.PropertySheet) */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		risingTimeout = ps.getInt(PROP_RISING_TIMEOUT);
		anyProsodyTimeout = ps.getInt(PROP_ANY_TIMEOUT);
		useProsody = ps.getBoolean(PROP_USE_PROSODY);
		logger.info("started iubasedfloortracker");
	}

	/**
	 * set a timeout to throw a NO_INPUT signal unless something is spoken
	 * within the next given number of milliseconds
	 * @param milliseconds the number of milliseconds until the timeout 
	 */
	public void installInputTimeout(int milliseconds) {
		installNewThread(new ExpectingInputTimeOutThread(milliseconds));
	}
	
	/** 
	 * handles creation (and destruction) of time-out threads. 
	 * Threads are created on commit (i.e. when VAD thinks that the
	 * user is silent) and when a <sil> word is added (i.e. when the
	 * recognizer thinks, that the user is silent);
	 * running threads are destroyed on add of a non-silence word
	 * (i.e. when VAD notices that the user speaks and additionally 
	 * the recognizer thinks that this is not silence)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty()) {
			if (isNotInInput() && !edits.get(0).getType().isCommit()) { 
				// signal start of speech when input starts
				signalListeners(InternalState.IN_INPUT, Signal.START);
			}
			if (hasNewNonSilWord((List<EditMessage<WordIU>>) edits)){
				killThread();
			}
			WordIU potentiallyFinalWord = getPotentiallyFinalWord((List<WordIU>) ius, (List<EditMessage<WordIU>>) edits);
			boolean isNewFinalWord = potentiallyFinalWord != null 
								&& (timeOutThread == null
								    || !(timeOutThread instanceof WordTimeOutThread)
									|| (potentiallyFinalWord != ((WordTimeOutThread) timeOutThread).endingWord));
			if (isNewFinalWord) {
				// this conflicts with the logging in the timeoutthread's constructor
				//logToTedView("found the potentially final word " + potentiallyFinalWord);
				//assert timeOutThread == null : "There shall be no timeout threads during speech";
				installNewThread(new WordTimeOutThread(potentiallyFinalWord));
			}
		}
	}
	
	/** true if a new non-silence word was added. */
	private boolean hasNewNonSilWord(List<EditMessage<WordIU>> edits) {
		boolean hasWord = false;
		Iterator<EditMessage<WordIU>> it = edits.iterator();
		while (it.hasNext()) {
			EditMessage<WordIU> edit = it.next();
			hasWord = edit.getType() == EditType.ADD && !edit.getIU().isSilence();
			if (hasWord)
				break;
		}
		return hasWord;
	}
	
	/** true if the list of IUs ends in &lt;sil&gt; or if the list of IUs has been committed */
	private WordIU getPotentiallyFinalWord(List<WordIU> ius, 
			List<EditMessage<WordIU>> edits) {
		// on commit, we return the last non-silent word right away
		if (edits.get(edits.size() - 1).getType().isCommit())
			return lastNonSilentWord(ius);
		// if the list ends in <sil>, we also return the last nonsilent word:
		if (ius.size() > 0 && ius.get(ius.size() - 1).isSilence()) {
			return lastNonSilentWord(ius);
		}
		// otherwise, there's no potentially final word
		return null;
	}
	
	/** the last nonsilent word from words, or null if none exists */
	private WordIU lastNonSilentWord(List<WordIU> words) {
		int lastIndex = words.size();
		WordIU endingWord = null;
		do {
			lastIndex--;
			endingWord = words.get(lastIndex);
		} while (lastIndex > 0 && endingWord != null && endingWord.isSilence());
		if (lastIndex < 0)
			return null;
		else 
			return endingWord;
	}
	
	/** install a new timeout thread */
	private void installNewThread(TimeOutThread tot) {
		killThread();
		timeOutThread = tot;
		timeOutThread.start();
	}
	
	/** kill a currently running timeout thread */
	private void killThread() {
		if (timeOutThread != null) {
			timeOutThread.kill();
			timeOutThread = null;
		}
	}
	
	/**
	 * supports classes for timing out and sending signals 
	 * @author timo
	 */
	private abstract class TimeOutThread extends Thread {
	
		/** if set, this timeout thread will not do anything when its timer runs out */
		private boolean killbit = false;
		
		/** create thread with a given name */
		public TimeOutThread(String name) {
			super(name);
		}
	
		protected void sleepSafely(long timeout) {
			if (timeout < 3) // ignore microsleeps
				return;
			logger.debug("going to sleep for " + timeout + " milliseconds");
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.debug("waking up after " + timeout + " milliseconds of sleep");
		}
		
		private void kill() {
			killbit = true;
			if (!killbit)
				logToTedView(this.hashCode() + "killed timeout thread " + this.getName());
		}
		
		protected boolean shouldDie() {
			if (killbit) {
				logger.debug("killed during timeout");
				return true;
			}
			// every de-referencing of timeOutThread should first set the kill bit
			assert timeOutThread == this : "I'm running without a killbit, even though floortracker doesn't reference me";
			return false;
		}
		
		protected synchronized void signal(Signal s) {
			logToTedView(this.getName() + " sending " + s);
			if (shouldDie())
				return;
			killbit = true;
			signalListeners(InternalState.NOT_AWAITING_INPUT, s);	
		}
		
		@Override
		public abstract void run();
		
	}

	/**
	 * times out and sends signals on an utterance-final word.
	 * this waits for a while (@see risingTimeout) before checking
	 * whether the word's pitch was rising. If it is, we send off
	 * a signal. Otherwise we wait a little longer (@see anyProsodyTimeout) 
	 * and only then send a signal.
	 */
	private class WordTimeOutThread extends TimeOutThread {
		
		/** the word that we'll inspect for rising pitch */
		WordIU endingWord;

		public WordTimeOutThread(WordIU endingWord) {
			super("timeout thread for " + endingWord.getWord());
			this.endingWord = endingWord;
			logToTedView(this.getName() + "starting timeout for \"" + endingWord.getWord() + "\"");
		}

		private Signal findBoundaryTone() {
			// do something with the following: 
			logger.debug("turnFinalWord is " + endingWord);
			if (endingWord.hasProsody()) {
				return endingWord.pitchIsRising() ?  Signal.EOT_RISING : Signal.EOT_NOT_RISING;
			}
			return Signal.EOT_NOT_RISING;
		}
		
		@Override
		public void run() {
			int timeoutStart = (int) (endingWord.endTime() * ((int) ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
			int timeout = getTime() - timeoutStart + risingTimeout;
			sleepSafely(timeout);
			if (shouldDie())
				return;
			if (useProsody) {
				Signal s = findBoundaryTone();
				if (s.equals(Signal.EOT_RISING)) {
					signal(s);
					return;
				} else {
					logToTedView(this.getName() + " found " + s + " in word " + endingWord + ";\n sleeping again");
				}
			}
			sleepSafely(anyProsodyTimeout);
			if (shouldDie())
				return;
			Signal s = findBoundaryTone();
			signal(s);
		}
	}
	
	private class ExpectingInputTimeOutThread extends TimeOutThread {
		
		int timeout;

		public ExpectingInputTimeOutThread(int milliseconds) {
			super("input expecting timeout (" + milliseconds + " ms)");
			this.timeout = milliseconds;
		}

		@Override
		public void run() {
			sleepSafely(timeout);
			if (shouldDie())
				return;
			signal(Signal.NO_INPUT);
		}
		
	}

}
