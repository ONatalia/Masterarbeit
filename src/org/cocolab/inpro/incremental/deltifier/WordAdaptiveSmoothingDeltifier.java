package org.cocolab.inpro.incremental.deltifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.TextualWordIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

/**
 * TODO/possible extensions:
 * - make incremental smoothing depend on currently salient actions
 *   (left is more likely after left, stop is more likely while moving)
 */

public class WordAdaptiveSmoothingDeltifier extends SmoothingDeltifier {
	
	/**
	 * more interesting assignment of smoothing factors
	 * depending on whether something is an "urgent word",
	 * a "stay-safe word", or just any other word  
	 */
	@Override
	protected int getSmoothingFactor(EditMessage<WordIU> edit) {
		if (isStaySafeWord(edit))
			return 20; // 200ms
		else if (isUrgentWord(edit))
			return 0; // immediately
		else // and all other words should get the regular smoothing factor
			return smoothing;
	}
	
	/**
	 * determines the edits that should get a high safety-threshold
	 * words with action:drop should get a high smoothing factor,
	 */
	protected boolean isStaySafeWord(EditMessage<WordIU> edit) {
		return (edit.getIU().getAVPairs() != null && 
				edit.getIU().getAVPairs().size() > 0 &&
				edit.getIU().getAVPairs().get(0).getValue().equals("drop"));
	}
	
	/**
	 * determines the words that should be handled with priority 
	 * words with action:stop should get a very low smoothing factor for the ADD message 
	 */
	protected boolean isUrgentWord(EditMessage<WordIU> edit) {
		return (edit.getType() == EditType.ADD &&
				edit.getIU().getAVPairs() != null && 
				edit.getIU().getAVPairs().size() > 0 &&
				edit.getIU().getAVPairs().get(0).getValue().equals("stop"));
	}
	
	/** 
	 * apply edits from smoothingQueue if their counter has run out, 
	 * add them to edit list and update wordIUs
	 * <p>
	 * in contrast to the overridden operation, this looks at *all* enqueued 
	 * items (but stopping if it reaches a stay-safe-word),
	 * and if a counter has run out it enqueues also words preceding this word,
	 * for which the counter has not run out.
	 * 
	 * in other words: "jetzt stop" will be released as soon as "stop" goes off,  
	 * even though "jetzt" may not have gone off yet. 
	 * 
	 */
	@Override
	protected void applySmoothingQueueToOutputWords() {
		ListIterator<SmoothingCounter> smoothIter = smoothingQueue.listIterator();
		restoreOutputLists();
		while (smoothIter.hasNext()) {
			SmoothingCounter sc = smoothIter.next();
			if (sc.count <= 0) {
				List<EditMessage<WordIU>> editsToBeApplied = determineEditsToBeApplied(smoothIter);
				applyEditsToOutputLists(editsToBeApplied);
			} else if (sc.countStart > smoothing) { // if there's a stay-safe-word in the list, break
				break;
			} else { 
				// nothing
			}
		}
	}
	
	/**
	 * determine the edits from the smoothingQueue that should be applied if
	 * a counter has run out
	 * @param smoothIter
	 * @return all the items in the smoothingQueue that *precede* the item for 
	 * 			which the counter has run out (including that item)
	 */
	private List<EditMessage<WordIU>> determineEditsToBeApplied(
			ListIterator<SmoothingCounter> smoothIter) {
		// make a list of the edits to be applied (which is reversed, as we go backwards through the iterator
		List<EditMessage<WordIU>> editsToBeAppliedInReverseOrder = new ArrayList<EditMessage<WordIU>>();
		while (smoothIter.hasPrevious()) {
			editsToBeAppliedInReverseOrder.add(smoothIter.previous().edit);
			smoothIter.remove();
		}
		// change to regular order
		Collections.reverse(editsToBeAppliedInReverseOrder);
		return editsToBeAppliedInReverseOrder;
	}

	/** 
	 * apply edits to the output lists (wordIUs and wordEdits)
	 * @param editsToBeApplied
	 */
	private void applyEditsToOutputLists(List<EditMessage<WordIU>> editsToBeApplied) {
		// apply the edits
		for (EditMessage<WordIU> edit : editsToBeApplied) {
			wordIUs.apply(edit);
			wordEdits.add(edit);					
		}
	}

	/** only for testing */
	public static void main(String[] args) {
		try {
			WordIU.setAVPairs(AVPairMappingUtil.readAVPairs("res/GreifarmAVMapping"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		WordAdaptiveSmoothingDeltifier wasd = new WordAdaptiveSmoothingDeltifier();
		// the list of already present WordIUs in the queue: 
		ArrayList<SmoothingCounter> queue = new ArrayList<SmoothingCounter>();
		TextualWordIU alreadyThereWord = new TextualWordIU("null", null);
		IUList<WordIU> prevWordIUs = new IUList<WordIU>();
		prevWordIUs.add(alreadyThereWord);
		wasd.wordIUs.clear();
		wasd.wordIUs.addAll(prevWordIUs);
		wasd.saveOutputLists();
		TextualWordIU firstWord = new TextualWordIU("eins", null);
		TextualWordIU secondWord = new TextualWordIU("zwei", null);
		queue.add(wasd.new SmoothingCounter(new EditMessage<WordIU>(EditType.ADD, firstWord), 0));
		queue.add(wasd.new SmoothingCounter(new EditMessage<WordIU>(EditType.ADD, secondWord), 2));
		wasd.smoothing = 7;
		wasd.smoothingQueue = queue;
		
		// the incoming WordIUs:
		IUList<WordIU> inputWords = new IUList<WordIU>();
		WordIU stopWord = new TextualWordIU("stop", firstWord);
		inputWords.add(alreadyThereWord);
		inputWords.add(firstWord);
		inputWords.add(secondWord);
		inputWords.add(stopWord);
		wasd.wordIUs.clear();
		wasd.wordIUs.addAll(inputWords);
		wasd.wordEdits.clear();
		wasd.wordEdits.add(new EditMessage<WordIU>(EditType.ADD, firstWord));
		wasd.wordEdits.add(new EditMessage<WordIU>(EditType.ADD, secondWord));
		wasd.wordEdits.add(new EditMessage<WordIU>(EditType.ADD, stopWord));
		
		// kind of a re-implementation of SmoothingDeltifier#deltify(Token), but
		// without the Token part, and instead directly supplying the inputWords 
		wasd.handleIncomingWordIUs(); 
		wasd.applySmoothingQueueToOutputWords();

		System.out.println("I expect the following for: " + inputWords);
		System.out.println("output for both nach and stop");
		
		System.out.println("I got the following edits: " + wasd.getWordEdits());
		System.out.println("I got the following IUs: " + wasd.getWordIUs());
		
	}
}