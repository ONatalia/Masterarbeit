package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.domains.turncompleter.CompletionEvaluator;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class TurnCompleter extends IUModule {

	@S4Component(type = DispatchStream.class, mandatory = true)
	public static final String PROP_DISPATCHER = "dispatchStream";
	DispatchStream audioDispatcher;
	
	@S4Component(type = CompletionEvaluator.class, mandatory = false)
	public static final String PROP_EVALUATOR = "evaluator";
	CompletionEvaluator evaluator;
	
	private static int OUTPUT_BUFFER_DELAY = 150;
	
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
		String fullText = "nordwind und sonne einst stritten sich nordwind und sonne wer von ihnen beiden wohl der stärkere wäre als ein wanderer der in einen warmen mantel gehüllt war des weges daherkam sie wurden einig dass derjenige für den stärkeren gelten sollte der den wanderer zwingen würde seinen mantel abzunehmen";
		fullInstallment = new SysInstallmentIU(fullText);
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
			if (shouldFire(inputWords)) {
				WordIU word = edit.getIU();
				System.err.println("going to fire after " + word.getWord());
				doComplete(inputWords, fullInstallment);
			}
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
		WordIU currentWord = lastElement(input);
		// let's analyze the word-beginnings (but only for non-silence words)
		List<Double> wordStarts = getWordStarts(input);
		double extrapolatedTime = extrapolateNext(wordStarts);
		// getAge() counts in milliseconds, the other values are in seconds
		System.err.println("expecting next word at (s) " + extrapolatedTime);
		System.err.println(currentWord.startTime());
		System.err.println(currentWord.getAge());
		long whenToStart1 = (long) (1000 * (extrapolatedTime - currentWord.startTime())) - currentWord.getAge();
		System.err.println("assuming input+output buffer delay (ms) " + OUTPUT_BUFFER_DELAY);
		whenToStart1 -= OUTPUT_BUFFER_DELAY;
		int whenToStart = (int) Math.max(0, whenToStart1);
		try {
			if (whenToStart > 0) {
				System.err.println("I'm " + whenToStart + " ms early, inserting silence.");
				audioDispatcher.playSilence(whenToStart, true);
			}
			audioDispatcher.playFile("file:/home/timo/inpro/experimente/039_finisher/fuenfsechssiebenacht.wav", false);
			System.err.println("now (whenToStart1 was " + whenToStart1 + " ms)");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	/** extrapolate the start time of the next word from a list of previous word starts */ 
	private double extrapolateNext(List<Double> wordStarts) {
		// start out with a very simple approach: start(last word) - start(first word) / (number of words - 1) 
		return wordStarts.get(wordStarts.size() - 1) + (wordStarts.get(wordStarts.size() - 1) - wordStarts.get(0)) / (wordStarts.size() - 1);
	}

	/** extract the start times of given words (ignoring silence) */ 
	private List<Double> getWordStarts(List<WordIU> ius) {
		List<Double> wordStarts = new ArrayList<Double>(ius.size());
		for (WordIU word : ius)
			if (!word.isSilence())
				wordStarts.add(word.startTime());
		return wordStarts;
	}

	/** utility which unfortunately is not part of java.util. */
	private <T> T lastElement(List<T> list) {
		return list.get(list.size() - 1);
	}
	
}
