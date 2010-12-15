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

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class TurnCompleter extends IUModule implements FrameAware {

	@S4Component(type = DispatchStream.class, mandatory = true)
	public static final String PROP_DISPATCHER = "dispatchStream";
	DispatchStream audioDispatcher;
	
	@S4Component(type = CompletionEvaluator.class, mandatory = false)
	public static final String PROP_EVALUATOR = "evaluator";
	CompletionEvaluator evaluator;
	
	@SuppressWarnings("unused")
	private static int OUTPUT_BUFFER_DELAY = 150;
	
	private IUList<WordIU> committedWords;
	private SysInstallmentIU fullInstallment;
	
	/** statically set av pairs for digits/numbers * /
	static {
		Map<String, List<AVPair>> avPairs = new HashMap<String, List<AVPair>>();
		avPairs.put("null", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(0))));
		avPairs.put("eins", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(1))));
		avPairs.put("zwei", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(2))));
		avPairs.put("zwo", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(2))));
		avPairs.put("drei", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(3))));
		avPairs.put("vier", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(4))));
		avPairs.put("fünf", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(5))));
		avPairs.put("fuenf", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(5))));
		avPairs.put("sechs", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(6))));
		avPairs.put("sieben", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(7))));
		avPairs.put("acht", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(8))));
		avPairs.put("neun", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(9))));
		avPairs.put("zehn", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(10))));
		avPairs.put("elf", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(11))));
		avPairs.put("zwölf", Collections.<AVPair>singletonList(new AVPair("num", Integer.valueOf(12))));
		WordIU.setAVPairs(avPairs);
	} */
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		audioDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
		evaluator = (CompletionEvaluator) ps.getComponent(PROP_EVALUATOR);
//		String fullText = "nordwind und sonne einst stritten sich nordwind und sonne wer von ihnen beiden wohl der stärkere wäre als ein wanderer der in einen warmen mantel gehüllt war des weges daherkam sie wurden einig dass derjenige für den stärkeren gelten sollte der den wanderer zwingen würde seinen mantel abzunehmen";
		String fullText = "nordwind und sonne: einst stritten sich nordwind und sonne, wer von ihnen beiden wohl der stärkere wäre; als ein wanderer der in einen warmen mantel gehüllt war, des weges daherkam; sie wurden einig, dass derjenige für den stärkeren gelten sollte, der den wanderer zwingen würde, seinen mantel abzunehmen";
//		String fullText = "der nordwind blies mit aller macht; aber je mehr er blies, desto fester hüllte sich der wanderer in seinen mantel ein; endlich gab der nordwind den kampf auf; nun erwärmte die sonne die luft mit ihren freundlichen strahlen, und schon nach wenigen augenblicken zog der wanderer seinen mantel aus; da musste der nordwind zugeben, dass die sonne von ihnen beiden der stärkere war";
		fullInstallment = new SysInstallmentIU(fullText);
		committedWords = new IUList<WordIU>();
	}
	
	@SuppressWarnings("unchecked") // casts from IU to WordIU 
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		//TODO: on commit: remove committed words from fullUtterance, re-TTS the installmentIU
		EditMessage<WordIU> edit = (edits.isEmpty()) ? null : (EditMessage<WordIU>) lastElement(edits);
		List<WordIU> inputWords = (List<WordIU>) ius;
		// we only ever do something on the addition of a word which is not a silence
		if (edit != null && edit.getType().equals(EditType.ADD) && !edit.getIU().isSilence()) {
			List<WordIU> fullInput = new ArrayList<WordIU>(committedWords);
			fullInput.addAll(inputWords);
			if (shouldFire(fullInput)) {
				doComplete(fullInput, fullInstallment);
			}
		} else if (edit != null && edit.getType().equals(EditType.COMMIT)) {
			committedWords.addAll(inputWords);
		}
	}
	
	/** 
	 * determine whether we should complete the utterance based on the words heard so far
	 * currently, the algorithm decides to *always* (repeatedly) fire when the expected prefix is recognized  
	 */
	private boolean shouldFire(List<WordIU> words) {
		// always fire if there were at least 3 words and the expected prefix is matched
		return (nonSilWords(words) >= 3 && fullInstallment.matchesPrefix(words));
	}
	
	/** return the number of non silent words in the given word list */
	private int nonSilWords(List<WordIU> words) {
		int count = 0;
		for (WordIU word : words)
			if (!word.isSilence())
				count++;
		return count;
	}
	
	
	private void doComplete(List<WordIU> input, SysInstallmentIU fullInstallment) {
		double extrapolatedTime = extrapolateStart(input, fullInstallment);
		evaluator.newOnsetResult(lastElement(input), extrapolatedTime, this.currentFrame);
		// TODO: actually output the completion, not only log it to the evaluator
	}

	/** extrapolate the start time of the next word from a list of previously spoken words */ 
//	private double extrapolateStart(List<WordIU> prefix, SysInstallmentIU fullInstallment) {
//		// this implementation assumes that all words are (more or less) of equal length,
//		// allowing it to only look at word starts and extrapolate the next word start based on this
//		List<Double> wordStarts = getWordStarts(prefix);
//		// start out with a very simple approach: start(last word) - start(first word) / (number of words - 1)
//		return getDuration(prefix) / prefix.size()
//		return wordStarts.get(wordStarts.size() - 1) + (lastElement(wordStarts) - wordStarts.get(0)) / (wordStarts.size() - 1);
//	}
	private double extrapolateStart(List<WordIU> prefix, SysInstallmentIU fullInstallment) {
		/*
		 * this implementation uses a duration model based on MaryTTS
		 * 
		 * we compare the duration of a TTSed utterance with the spoken words 
		 * (except for the most recent word, which is likely still being spoken).
		 * From this we can deduce a speech rate relative to the TTS (i.e. faster
		 * or slower) and use this as a factor to the TTS's most recent word's 
		 * duration, which should give the duration of the currently spoken word
		 */
		WordIU currentlySpokenWord = lastElement(prefix);
		List<WordIU> completedUserPrefix = new ArrayList<WordIU>(prefix);
		completedUserPrefix.remove(currentlySpokenWord);
		List<WordIU> fullTtsPrefix = fullInstallment.getPrefix(prefix);
		List<WordIU> completedTTSPrefix = new ArrayList<WordIU>(fullTtsPrefix);
		WordIU currentlyTTSedWord = lastElement(fullTtsPrefix);
		completedTTSPrefix.remove(currentlyTTSedWord);
		double  ttsDuration = getDurationWithoutPauses(completedTTSPrefix);
		double userDuration = getDurationWithoutPauses(completedUserPrefix);
		return currentlySpokenWord.startTime() + (currentlyTTSedWord.duration() * userDuration / ttsDuration);
	}

	/** time spanned by all words */
	@SuppressWarnings("unused")
	private double getDuration(List<WordIU> words) {
		return lastElement(words).endTime() - words.get(0).startTime();
	}
	
	private double getDurationWithoutPauses(List<WordIU> words) {
		double dur = 0;
		for (WordIU word : words) {
			if (!word.isSilence())
				dur += word.duration();
		}
		return dur;
	}

	/** extract the start times of given words (ignoring silence) */ 
	@SuppressWarnings("unused")
	private List<Double> getWordStarts(List<WordIU> ius) {
		List<Double> wordStarts = new ArrayList<Double>(ius.size());
		for (WordIU word : ius)
			if (!word.isSilence())
				wordStarts.add(word.startTime());
		return wordStarts;
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
