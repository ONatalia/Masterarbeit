package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.domains.turncompleter.CompletionEvaluator;
import org.cocolab.inpro.incremental.FrameAware;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU.FuzzyMatchResult;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;

public class TurnCompleter extends IUModule implements FrameAware {

	@S4Component(type = DispatchStream.class, mandatory = true)
	public static final String PROP_DISPATCHER = "dispatchStream";
	DispatchStream audioDispatcher;
	
	@S4Component(type = CompletionEvaluator.class, mandatory = false)
	public static final String PROP_EVALUATOR = "evaluator";
	CompletionEvaluator evaluator;
	
	@S4String(defaultValue = "eins zwei drei vier fünf sechs sieben")
	public static final String PROP_FULL_UTTERANCE = "fullUtterance";
	
	@SuppressWarnings("unused")
	private static int OUTPUT_BUFFER_DELAY = 150;
	
	private IUList<WordIU> committedWords;
	private SysInstallmentIU fullInstallment;
	
	/** full input at one point in time */
	List<WordIU> fullInput;
	/** fuzzy match of the full input against the expected full installment */
	FuzzyMatchResult fmatch;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		audioDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
		evaluator = (CompletionEvaluator) ps.getComponent(PROP_EVALUATOR);
//		String fullText = "nordwind und sonne: einst stritten sich nordwind und sonne, wer von ihnen beiden wohl der stärkere wäre; als ein wanderer der in einen warmen mantel gehüllt war, des weges daherkam; sie wurden einig, dass derjenige für den stärkeren gelten sollte, der den wanderer zwingen würde, seinen mantel abzunehmen";
//		String fullText = "der nordwind blies mit aller macht; aber je mehr er blies, desto fester hüllte sich der wanderer in seinen mantel ein; endlich gab der nordwind den kampf auf; nun erwärmte die sonne die luft mit ihren freundlichen strahlen, und schon nach wenigen augenblicken zog der wanderer seinen mantel aus; da musste der nordwind zugeben, dass die sonne von ihnen beiden der stärkere war";
		String fullUtterance = ps.getString(PROP_FULL_UTTERANCE);
		fullInstallment = new SysInstallmentIU(fullUtterance);
		committedWords = new IUList<WordIU>();
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
			fmatch = fullInstallment.fuzzyMatching(fullInput, 0.2, 2);
			if (shouldFire()) {
				doComplete();
			}
		} else if (edit != null && edit.getType().equals(EditType.COMMIT)) {
			//on commit: remove committed words from fullUtterance, re-TTS the installmentIU
			committedWords.addAll(inputWords);
		}
	}
	
	/** 
	 * determine whether we should complete the utterance based on the words heard so far
	 * currently, the algorithm decides to *always* (repeatedly) fire when the expected prefix is recognized  
	 */
	private boolean shouldFire() {
		// always fire if there were at least 3 words and the expected prefix is matched
		//System.err.println(fullInstallment.fuzzyMatchesPrefix(words, 0.02, 2) + "" + words);
		return (WordIU.removeSilentWords(fullInput).size() > 2 && fmatch.matches());
	}
	
	private void doComplete() {
		double estimatedSpeechRate = estimateSpeechRate();
		double extrapolatedTime = extrapolateStart();
		List<WordIU> completion = fmatch.getRemainder();
		WordIU nextWord = completion.get(0);
		double nextWordEndEstimate = extrapolatedTime + nextWord.duration() * estimatedSpeechRate;
		evaluator.newOnsetResult(lastElement(fullInput), extrapolatedTime, this.currentFrame, nextWord, nextWordEndEstimate);
		// TODO: actually output the completion, not only log it to the evaluator
		// this consists of two steps: 
		// a) deep-copy and scale the SysInstallmentIU
		// b) think hard about how best to do pitch-scaling
		// c) synthesize and play the completion
	}

	/** extrapolate the start time of the next word from a list of previously spoken words */ 
//	private double extrapolateStart(List<WordIU> prefix, SysInstallmentIU fullInstallment) {
//		// this implementation assumes that all words are (more or less) of equal length,
//		// allowing it to only look at word starts and extrapolate the next word start based on this
//		List<Double> wordStarts = getWordStarts(prefix);
//		// start out with a very simple approach: start(last word) - start(first word) / (number of words - 1)
//		return getDuration(prefix) / prefix.size()
//	}
	private double extrapolateStart() {
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
		return currentlySpokenWord.startTime() + (currentlyTTSedWord.duration() * estimateSpeechRate());
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

	int currentFrame = 0;

	@Override
	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
	/** utility which unfortunately is not part of java.util. */
	private <T> T lastElement(List<T> list) {
		return list.get(list.size() - 1);
	}
	
}
