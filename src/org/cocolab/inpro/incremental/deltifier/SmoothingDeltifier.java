package org.cocolab.inpro.incremental.deltifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

/** 
 * only outputs IUs if a word is consecutively hypothesized for <code>N</code> frames.

this is realized by overriding {@link ASRWordDeltifier#deltify(Token)}.
<p>
for further reading, please see:
<p>
Timo Baumann, Michaela Atterer and David Schlangen (2009).<br />
"Assessing and Improving the Performance of Speech Recognition for Incremental Systems".<br />
in <em>Proceedings of NAACL-HLT 2009</em>, Boulder, USA.<br />
Online: 
<a href="http://www.ling.uni-potsdam.de/~timo/pub/#naacl2009">http://www.ling.uni-potsdam.de/~timo/pub/#naacl2009</a>.
</p>
 * @see ASRWordDeltifier#deltify(Token)
 * @author timo
 */
public class SmoothingDeltifier extends ASRWordDeltifier {

    @S4Integer(defaultValue = 0)
	public final static String PROP_SMOOTHING = "smoothing";
	int smoothing;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		smoothing = ps.getInt(PROP_SMOOTHING);
	}
	
	List<SmoothingCounter> smoothingQueue = new ArrayList<SmoothingCounter>();
	
	private IUList<WordIU> prevWordIUs;
	
	/**
	 * deltifies using a smoothing algorithm.
	 * 
	 * words are only output when their IU has ``matured'', that is, has
	 * been proposed by the general deltification algorithm for a certain,
	 * consecutive number of frames (which is configurable)
	 * 
	 * <ul><li>
	 * we first save the list of previously output words to prevWordIUs,
	 * </li><li> 
	 * then we have the standard deltification algorithm calculate the edits
	 * to what has already been output
	 * </li><li>
	 * then we generate the filtered output on the basis of the standard
	 * output, which consists of two parts:  
	 * </li><ul><li>
	 *     handle incoming IUs, updating smoothingCounters for words of 
	 *     still-valid words in the smoothingQueue and removing invalid ones and  
	 *     adding new ones for other words
	 * </li><li>
	 *     generate a new list of output IUs and edits by checking which 
	 *     smoothingCounters have reached 0 in the smoothingQueue; for this
	 *     we need the list of previously output IUs (which is why we stored
	 *     them in the beginning
	 * </li></ul></ul> 
	 */
	@Override
	protected synchronized void deltify(Token token) {
		// keep a copy of the original wordIUs
		saveOutputLists();
		// calculate would-be edits the standard way
		super.deltify(token);
		if (!recoFinal) {
			handleIncomingWordIUs(); 
			applySmoothingQueueToOutputWords();
		}
	}

	/**
	 * update smoothing counters for words of still-valid words in the smoothing
	 * queue, remove invalid ones, add new ones for new words
	 */
	protected void handleIncomingWordIUs() {
		Iterator<EditMessage<WordIU>> editsIter = wordEdits.iterator();
		Iterator<SmoothingCounter> smoothIter = smoothingQueue.iterator();
		EditMessage<WordIU> edit = compareToSmoothingQueueForMatchingWords(editsIter, smoothIter);
		removeNonMatchingWordsFromSmoothingQueue(smoothIter);
		addNewWordsToSmoothingQueue(edit, editsIter, smoothingQueue);
	}

	/** 
	 * apply edits from smoothingQueue if their counters have run out, 
	 * add them to edit list and update wordIUs
	 * NOTE: this implementation only handles smoothing factors that
	 * are constant for all words:
	 * <b>
	 * how it works: going through the list of enqueued edits; 
	 * while the word's counter has run out, add them to output, 
	 * stop as soon as a counter is still counting
	 * 
	 */
	protected void applySmoothingQueueToOutputWords() {
		Iterator<SmoothingCounter> smoothIter = smoothingQueue.iterator();
		restoreOutputLists();
		while (smoothIter.hasNext()) {
			SmoothingCounter sc = smoothIter.next();
			if (sc.count <= 0) {
				wordIUs.apply(sc.edit);
				wordEdits.add(sc.edit);
				smoothIter.remove();
			} else {
				break;
			}
		}
	}
	
	protected void saveOutputLists() {
		prevWordIUs = new IUList<WordIU>(wordIUs);
	}

	/** initiate output lists */
	protected void restoreOutputLists() {
		wordEdits.clear();
		wordIUs.clear();
		wordIUs.addAll(prevWordIUs);
	}



	/**
	 * compares incoming edits and smoothing list, 
	 * decreases counters for matching smoothings and
	 * returns as soon as a non-matching word is found; this word is returned
	 * (as the underlying iterator can't go back :-(
	 * @param editsIter
	 * @param smoothIter
	 * @return the first non-matching word, or null if all words match
	 */
	private EditMessage<WordIU> compareToSmoothingQueueForMatchingWords(
			Iterator<EditMessage<WordIU>> editsIter, Iterator<SmoothingCounter> smoothIter) {
		// decrease smoothing-counter in all matching enqueued edits in the smoothingQueue
		// stop as soon as the new and enqueued edits don't match anymore 
		EditMessage<WordIU> edit = null;
		while (smoothIter.hasNext() && editsIter.hasNext()) {
			SmoothingCounter sc = smoothIter.next();
			edit = editsIter.next();
			if (sc.matches(edit)) {
				sc.updateWordTimings(edit.getIU());
				sc.count--;
			} else {
				smoothIter.remove();
				break;
			}
			edit = null;
		}
		return edit;
	}

	private void removeNonMatchingWordsFromSmoothingQueue(
			Iterator<SmoothingCounter> smoothIter) {
		// now deal with non-matching edits (kill remaining entries in smoothingQueue, enqueue for new edits)
		while (smoothIter.hasNext()) {
			smoothIter.next();
			smoothIter.remove();
		}
	}

	/**
	 * @param edit a (possibly null) edit to add first 
	 * @param editsIter more edits to add
	 * @param smoothingQueue the queue to add to
	 */
	private void addNewWordsToSmoothingQueue(EditMessage<WordIU> edit,
			Iterator<EditMessage<WordIU>> editsIter,
			List<SmoothingCounter> smoothingQueue) {
		if (edit != null) 
			smoothingQueue.add(new SmoothingCounter(edit));
		while (editsIter.hasNext()) {
			smoothingQueue.add(new SmoothingCounter(editsIter.next()));
		}
	}
	
	/**
	 * return the smoothing factor that applies to this edit message
	 * @param edit ignored in this implementation
	 * @return smoothing factor for this edit; constant in this implementation 
	 */
	protected int getSmoothingFactor(EditMessage<WordIU> edit) {
		return smoothing;
	}

	/**
	 * stores the maturity for a given  edit message
	 */
	protected class SmoothingCounter {
		EditMessage<WordIU> edit;
		int count;
		int countStart;
		
		SmoothingCounter(EditMessage<WordIU> edit) {
			this(edit, getSmoothingFactor(edit));
		}
		
		public void updateWordTimings(WordIU otherWord) {
			this.edit.getIU().updateTimings(otherWord);
			new RuntimeException("not implemented yet");
		}

		protected SmoothingCounter(EditMessage<WordIU> edit, int count) {
			this.edit = edit;
			this.countStart = count;
			this.count = countStart;
		}
		
		public String toString() {
			return edit.toString() + "(" + count + ")"; 
		}
		
		boolean matches(EditMessage<WordIU> edit) {
			return 
			    this.edit.getType().equals(edit.getType()) 
			 && this.edit.getIU().pronunciationEquals(edit.getIU());
		}
	}

	@Override
	public String toString() {
		return "SmoothingDeltifier with factor " + smoothing;
	}
	
}
