package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.FrameAware;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.evaluation.CompletionEvaluator;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU.FuzzyMatchResult;
import org.cocolab.inpro.incremental.util.ResultUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;

public class TurnCompleter extends IUModule implements FrameAware {

	@S4Component(type = DispatchStream.class, mandatory = false)
	public static final String PROP_DISPATCHER = "dispatchStream";
	
	@S4Component(type = CompletionEvaluator.class, mandatory = false)
	public static final String PROP_EVALUATOR = "evaluator";
	
	private List<CompletionAdapter> compAdapters;
	
	@S4String(defaultValue = "eins zwei drei vier fünf sechs sieben")
	public static final String PROP_FULL_UTTERANCE = "fullUtterance";
	
	/** in seconds, very rough estimate */
	private static double OUTPUT_BUFFER_DELAY = 0.050;
	
	private SysInstallmentIU fullInstallment;
	
	/** history of words that have previously been recognized */
	private IUList<WordIU> committedWords;
	/** full user input at one point in time: words that have been committed + currently hypothesized words */
	List<WordIU> fullInput;
	/** fuzzy match of the full input against the expected full installment */
	FuzzyMatchResult fmatch;
	
	/** words from fullInstallment that have been output are stored here */
	private HashSet<WordIU> wordsTriggered = new HashSet<WordIU>();
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String fullUtterance = ps.getString(PROP_FULL_UTTERANCE);
		fullInstallment = new SysInstallmentIU(fullUtterance);
		committedWords = new IUList<WordIU>();
		
		compAdapters = new ArrayList<CompletionAdapter>();
		CompletionEvaluator evaluator = (CompletionEvaluator) ps.getComponent(PROP_EVALUATOR);
		if (evaluator != null) {
			compAdapters.add(new CompletionEvalAdapter(evaluator));
		}
		DispatchStream audioDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
		if (audioDispatcher != null) {
			compAdapters.add(new CompletionPlayer(audioDispatcher));
		}

	}
	
	@SuppressWarnings("unchecked") // casts from IU to WordIU 
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// we will only generate completions for the last element of the edit list
		// because when words are added with a follower, the first word is likely to be over already 
		EditMessage<WordIU> edit = (edits.isEmpty()) ? null : (EditMessage<WordIU>) lastElement(edits);
		List<WordIU> inputWords = (List<WordIU>) ius;
		// we only ever do something on the addition of a word which is not a silence
		if (edit != null && edit.getType().equals(EditType.ADD) && !edit.getIU().isSilence()) {
			fullInput = new ArrayList<WordIU>(committedWords);
			fullInput.addAll(inputWords);
			StringBuilder wordsSoFar = new StringBuilder();
			for (Iterator<WordIU> wIt = fullInput.iterator(); wIt.hasNext();) {
				WordIU word = wIt.next();
				if (!word.isSilence()) {
					wordsSoFar.append(word.toPayLoad());
					if (wIt.hasNext()) {
						wordsSoFar.append(" ");
					}
				}
			}
			logger.info("new installment " + wordsSoFar.toString());
			fullInstallment = new SysInstallmentIU(wordsSoFar.toString());
			fmatch = fullInstallment.fuzzyMatching(fullInput, 0.2, 2);
			if (fmatch.matches() && shouldFire()) {
				doComplete();
			}
		} else if (edit != null && edit.getType().isCommit()) {
			//on commit: remove committed words from fullUtterance, re-TTS the installmentIU
			committedWords.addAll(inputWords);
		}
	}
	
	/** determine whether we should complete the utterance based on the words heard so far */
	private boolean shouldFire() {
		// always fire if there were at least 3 words and we haven't yet fired for this word
		return (!hasFiredForTrigger() && WordIU.removeSilentWords(fullInput).size() > 2);
	}
	
	/**
	 * find out whether we have already fired for a word, to avoid stuttering; 
	 * initial experiments shows that the first prediction for a word is as  
	 * good as any later prediction so there's no value in trying repeatedly   
	 */
	private boolean hasFiredForTrigger() {
		List<WordIU> prefix = fmatch.getPrefix();
		WordIU triggerWord = (prefix != null && prefix.size() > 0) ? prefix.get(prefix.size() - 1) : null;
		if (triggerWord == null || wordsTriggered.contains(triggerWord)) {
			return true;
		} else {
			wordsTriggered.add(triggerWord);
			return false;
		}
	}
	
	private void doComplete() {
		double estimatedSpeechRate = estimateSpeechRate();
		double estimatedStartTime = extrapolateStart(estimatedSpeechRate);
		// this is the logic that decides what the completion should be (it could very well be different!)
		List<WordIU> remainder = fmatch.getRemainder();
		WordIU nextWord = remainder.get(0);
		// now generate the completion this consists of two steps: 
		// a) deep-copy and scale the SysInstallmentIU
		SysInstallmentIU output = new SysInstallmentIU(Collections.<WordIU>singletonList(nextWord));
		output.scaleDeepCopyAndStartAtZero(estimatedSpeechRate);
		
		// c) synthesize and play the completion
		for (CompletionAdapter ca : compAdapters) {
			ca.newCompletion(estimatedStartTime, output);
		}
	}
	
	/** extrapolate the start time of the next word from a list of previously spoken words */ 
//	private double extrapolateStart(List<WordIU> prefix, SysInstallmentIU fullInstallment) {
//	// start out with a very simple approach: start(last word) - start(first word) / (number of words - 1)
//		// this implementation assumes that all words are (more or less) of equal length,
//		// allowing it to only look at word starts and extrapolate the next word start based on this
//		List<Double> wordStarts = getWordStarts(prefix);
//		return getDuration(prefix) / prefix.size()
//	}
	private double extrapolateStart(double estimatedSpeechRate) {
		/* this implementation uses a duration model based on MaryTTS
		 * 
		 * we compare the duration of a TTSed utterance with the spoken words 
		 * (except for the most recent word, which is likely still being spoken).
		 * From this we can deduce a speech rate relative to the TTS (i.e. faster
		 * or slower) and use this as a factor to the TTS's most recent word's 
		 * duration, which should give the duration of the currently spoken word
		 */
		WordIU currentlySpokenWord = lastElement(fullInput);
		List<WordIU> fullTtsPrefix = fmatch.getPrefix();
		WordIU currentlyTTSedWord = lastElement(fullTtsPrefix);
		return currentlySpokenWord.startTime() + (currentlyTTSedWord.duration() * estimatedSpeechRate);
	}
	
	/** estimate the correction factor relative to the TTS's speech rate */
	private double estimateSpeechRate() {
		WordIU currentlySpokenWord = lastElement(fullInput);
		List<WordIU> completedUserPrefix = new ArrayList<WordIU>(fullInput);
		completedUserPrefix.remove(currentlySpokenWord);
		List<WordIU> fullTtsPrefix = fmatch.getPrefix();
		List<WordIU> completedTTSPrefix = new ArrayList<WordIU>(fullTtsPrefix);
		WordIU currentlyTTSedWord = lastElement(fullTtsPrefix);
		completedTTSPrefix.remove(currentlyTTSedWord);
		double  ttsDuration = getDurationWithoutPauses(completedTTSPrefix);
		double userDuration = getDurationWithoutPauses(completedUserPrefix);
		return userDuration / ttsDuration;
	}

	/** time spanned by all non-silent words */
	private double getDurationWithoutPauses(List<WordIU> words) {
		double dur = 0;
		for (WordIU word : words) {
			if (!word.isSilence())
				dur += word.duration();
		}
		return dur;
	}

	public interface CompletionAdapter {
		public void newCompletion(double estimatedStartTime, SysInstallmentIU completion);
	}
	
	private class CompletionEvalAdapter implements CompletionAdapter {
		CompletionEvaluator ce;

		CompletionEvalAdapter(CompletionEvaluator ce) {
			this.ce = ce;
		}
		
		@Override
		public void newCompletion(double estimatedStartTime,
				SysInstallmentIU completion) {
			WordIU nextWord = completion.getWords().get(0);
			double nextWordEndEstimate = estimatedStartTime + nextWord.duration();
			ce.newOnsetResult(lastElement(fullInput), estimatedStartTime, currentFrame, nextWord, nextWordEndEstimate);
		}
	}
	
	/**
	 * completions will only be played if they don't start with a pause
	 */
	private class CompletionPlayer implements CompletionAdapter {
		DispatchStream audioDispatcher;
		public CompletionPlayer(DispatchStream audioDispatcher) {
			this.audioDispatcher = audioDispatcher;
		}

		@Override
		public void newCompletion(double estimatedStartTime, SysInstallmentIU completion) {
			double holdingTime = estimatedStartTime - currentFrame * ResultUtil.FRAME_TO_SECOND_FACTOR;
			WordIU nextWord = completion.getWords().get(0);
			if (!nextWord.isSilence()) {
				InstallmentPlayer ip = new InstallmentPlayer(holdingTime, completion);
				ip.start();
			}
		}
	
		/* * * * * * * * * * * * * * * * * * *
		 * InstallmentPlayer for timed output
		 * * * * * * * * * * * * * * * * * * */
		/** outputs a given system installment after the given holding time has passed */
		class InstallmentPlayer extends Thread {
			SysInstallmentIU output;
			/** in seconds */
			double holdingTime; 
			boolean aborted = false;
			public InstallmentPlayer(double holdingTime, SysInstallmentIU output) {
				this.holdingTime = holdingTime;
				this.output = output;
			}
			
			/** if this is called before run() completes, then no output will be sent to the speakers */
			@SuppressWarnings("unused")
			public void abort() {
				this.aborted = true;
			}
			
			/** synthesize output and initiate playback after holdingTime has passed */
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				if (holdingTime < OUTPUT_BUFFER_DELAY) {
					logger.info("oups, I'm already starting late by " + holdingTime);
				}
				output.synthesize(); 
				// TODO: actually play after the holding time is over
				long duration = System.currentTimeMillis() - startTime; // in milliseconds
				logger.info(output.getWords().get(0).toPayLoad() + " took " + duration + " milliseconds");
				holdingTime -= duration * 0.001;
				if (holdingTime < OUTPUT_BUFFER_DELAY) {
					logger.info("oups, after synthesis I'm late by " + holdingTime);
				} else {
					try {
						Thread.sleep((long) ((holdingTime - OUTPUT_BUFFER_DELAY) * 1000));
					} catch (InterruptedException e) {
						logger.info("interrupted while sleeping.");
					}
				}
				if (!aborted) {
					audioDispatcher.playStream(output.getAudio(), true);
				}
			}
		}
	}


	/* * * * * * * * * * * * * * * *
	 * implementation of FrameAware
	 * * * * * * * * * * * * * * * */
	int currentFrame = 0;
	@Override
	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
	/* * * * * * * * * * *
	 * utility operations
	 * * * * * * * * * * */
	/** utility which unfortunately is not part of java.util. */
	private <T> T lastElement(List<T> list) {
		return list.get(list.size() - 1);
	}
	
}
