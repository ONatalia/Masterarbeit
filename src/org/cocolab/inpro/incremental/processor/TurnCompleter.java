package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class TurnCompleter extends IUModule {

	@S4Component(type = DispatchStream.class, mandatory = true)
	public static final String PROP_DISPATCHER = "dispatchStream";
	DispatchStream audioDispatcher;
	
	private static int OUTPUT_BUFFER_DELAY = 150;
	
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
	}
	
	@SuppressWarnings("unchecked") // cast of edit list to WordIUs 
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		EditMessage<WordIU> edit = (edits.isEmpty()) ? null : (EditMessage<WordIU>) edits.get(edits.size() - 1);
		if (edit != null && edit.getType().equals(EditType.ADD)) {
			WordIU word = edit.getIU();
//				if (word.hasAVPairs()) {
//					AVPair avp = word.getAVPairs().get(0);
//					int value = (Integer) avp.getValue();
//					if (value == 4) { // whenever we hear the word "four"
//						doComplete((List<WordIU>) ius, "fünf sechs sieben acht");
//					}
//				}
			if (nonSilWords((List<WordIU>) ius) == 6 && !word.isSilence()) {
				System.err.println("going to fire after " + word.getWord());
				doComplete((List<WordIU>) ius, "fünf sechs sieben acht");
			}
		}
	}
	
	private int nonSilWords(List<WordIU> words) {
		int count = 0;
		for (WordIU word : words) {
			if (!word.isSilence())
				System.err.print("\t" + word);
				count++;
		}
		System.err.println();
		return count;
	}
	
	private void doComplete(List<WordIU> input, String completion) {
		WordIU currentWord = input.get(input.size() - 1);
		// let's analyze the word-beginnings (but only for non-silence words
		List<Double> wordStarts = getWordStarts(input);
		double extrapolatedTime = extrapolateNext(wordStarts);
		// getAge() counts in milliseconds, the other values are in seconds
		System.err.println("expecting next word at (s) " + extrapolatedTime);
		System.err.println(currentWord.startTime());
		System.err.println(currentWord.getAge());
		long whenToStart1 = (long) (1000 * (extrapolatedTime - currentWord.startTime())) - currentWord.getAge();
		System.err.println("assuming output buffer delay (ms) " + OUTPUT_BUFFER_DELAY);
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

}
