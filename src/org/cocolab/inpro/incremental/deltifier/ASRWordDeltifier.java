/* 
 * Copyright 2008, 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.incremental.deltifier;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.incremental.util.WordUtil;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


/**
 * an ASRWordDeltifier (and descendants) can be used 
 * to store the ASR's (or one of the ASR's) current hypothesis
 * 
 * you would usually call deltify() (either on the result or a specific token from the n-best-list)
 * and afterwards retrieve the WordIUs and edits via getIUs() and getEdits()
 * 
 * Descendents of this class implement methods described in NAACL-HLT 2009
 * 
 * @author Timo Baumann
 */
public class ASRWordDeltifier implements Configurable, Resetable, ASRResultKeeper, SignalListener {

	IUList<WordIU> wordIUs = new IUList<WordIU>();
	
	List<EditMessage<WordIU>> wordEdits;
	
//	private static final Logger logger = Logger.getLogger(ASRWordDeltifier.class);
	
	int currentFrame = 0;
	int currentOffset = 0; // measured in centiseconds / frames
	long startTime = 0;
	
	protected boolean recoFinal; // flag to avoid smoothing or fixed lags on final result
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
	}
	
	protected synchronized List<Token> getTokens(Token token) {
		List<Token> tokens = ResultUtil.getTokenList(token, true, true);
		assert (tokens != null);
		// the following asserts that each word token is followed by at least one phoneme token
		// and that the sequence starts with a word token
		boolean phoneSeen = true;
		for (Token t : tokens) {
			SearchState s = t.getSearchState();
			if (s instanceof WordSearchState) {
				assert (phoneSeen) : tokens.toString();
				phoneSeen = false;
			} else if (s instanceof UnitSearchState) {
				phoneSeen = true;
			} else {
				assert false;
			}
		}
		return tokens;
	}
	
	/**
	 * update the stored ASR-representation with information from the ASR result's given Token  
	 * 
	 * the actual deltification algorithm works as follows:
	 * - we first extract all the word- and phoneme tokens from the token sequence
	 *   - each word-token is followed by the corresponding phoneme tokens
	 * - we then compare the previous and the current hypothesis:
	 *   - the beginning of the previous and this word hypothesis should be equal,
	 *     we only need to update phoneme timings
	 *   - eventually, the two hypotheses will differ:
	 *     - we construct remove-edits for the remaining old words
	 *     - we then construct the new words and add-edits for them 
	 * 
	 * @param token the token from which the deltification starts
	 */
	protected synchronized void deltify(Token token) {
		List<Token> newTokens = getTokens(token);
		@SuppressWarnings("unchecked")
		List<WordIU> currWordIUs = (List<WordIU>) wordIUs.clone();
		// step over wordIUs and newWords to see which are equal in both
		ListIterator<Token> newIt = newTokens.listIterator();
		ListIterator<WordIU> currWordIt = currWordIUs.listIterator();
		double segmentStartTime = currentOffset * 0.01;
		double segmentEndTime = 0.0;
		List<SegmentIU> emptyList = Collections.emptyList(); // needed to initialize prevSegmentIt with an empty non-null iterator
		Iterator<SegmentIU> currSegmentIt = emptyList.iterator();
		boolean addSilenceWord = false;
		while (newIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			if (newSearchState instanceof WordSearchState) {
				if (!currWordIt.hasNext()) {
					newIt.previous();
					break;
				}
				Pronunciation pron = ((WordSearchState) newToken.getSearchState()).getPronunciation();
				WordIU prevIU = currWordIt.next();
				// check if the words match and break once we reach the point where they don't match anymore
				if (!prevIU.wordEquals(pron)) {
					currWordIt.previous(); // go back the one word that didn't match anymore
					newIt.previous();
					break;
				}
				// set the segment iterator that labels for this word can be updated from the trailing segment tokens
				currSegmentIt = prevIU.getSegments().iterator();
			} else if (newSearchState instanceof UnitSearchState) {
				segmentEndTime = getTimeFromNewIt(newIt);
				String name = ((UnitSearchState) newSearchState).getUnit().getName();
				// in the FA case, SIL-phonemes are not covered by SIL-words. duh.
				if (currSegmentIt.hasNext()) {
					currSegmentIt.next().updateLabel(new Label(segmentStartTime, segmentEndTime, name));
				} else if (name.equals("SIL")) {
					if (currWordIt.hasNext() && currWordIt.next().isSilence()) {
						currWordIt.previous();
						WordIU currWord = currWordIt.next(); 
						currWord.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, name)));
						currSegmentIt = currWord.getSegments().iterator();
					} else {
						addSilenceWord = true;
						break;
					}
				} else {
					// assert false; // hmm, I don't know why, but it works without this assertion. and it doesn't hurt
				}
				segmentStartTime = segmentEndTime;
			}
		}
		// ok, now:
		// if there are words left in the prev word list, send purge notifications
		// purge notifications have to be sent in reversed order, starting with the very last word
		// therefore we put them in reverse order into a new list
		wordEdits = new LinkedList<EditMessage<WordIU>>();
		while (currWordIt.hasNext()) {
			WordIU prevIU = currWordIt.next();
			wordEdits.add(0, new EditMessage<WordIU>(EditType.REVOKE, prevIU));
		}
		// check if we need to insert a silence in the end (this happens when SIL does not have its own word token) 
		if (addSilenceWord) {
			WordIU newIU = new WordIU(null);
			newIU.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, "SIL")));
			wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
		}
		// for the remaining words in the new list, add them to the old list and send add notifications
		while (newIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			/* on WordSearchStates, we build an IU and add it */
			if (newSearchState instanceof WordSearchState) {
				Pronunciation pron = ((WordSearchState) newSearchState).getPronunciation();
				WordIU newIU = WordUtil.wordFromPronunciation(pron);
				currSegmentIt = newIU.getSegments().iterator();
				wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
			} else 
			/* on UnitSearchStates, we find the unit's timing and update the segmentIU */
			if (newSearchState instanceof UnitSearchState) {
				segmentEndTime = getTimeFromNewIt(newIt);

				String name = ((UnitSearchState) newSearchState).getUnit().getName();
				if (currSegmentIt.hasNext()) {
					currSegmentIt.next().updateLabel(new Label(segmentStartTime, segmentEndTime, name));
				} else if (name.equals("SIL")) {
					WordIU newIU = new WordIU(null);
					newIU.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, "SIL")));
					wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
				}
				segmentStartTime = segmentEndTime;
			}
		}
		wordIUs.apply(wordEdits);
	}
	
	/* tries to get segmentEndTime from a list iterator; 
	 * the iterator will be at the same position afterwards as it was before
	 */
	private double getTimeFromNewIt(ListIterator<Token> newIt) {
		double segmentEndTime;
		if (newIt.hasNext()) {
			Token t = newIt.next();
			if ((t.getSearchState() instanceof WordSearchState) 
			 && (newIt.hasNext()) 
			) {
				segmentEndTime = (newIt.next().getFrameNumber()+ currentOffset) * 0.01;
				newIt.previous();
			} else {
				segmentEndTime = (t.getFrameNumber() + currentOffset) * 0.01;
			}
			newIt.previous();
		} else
			segmentEndTime = getCurrentTime();
		return segmentEndTime;
	}

	/**
	 * update the stored ASR-representation with the new Result from ASR
	 * 
	 * - the current representation can afterwards be queried through getWordIUs()
	 * - the difference from the previous state can be queried through getWordEdits()
	 * 
	 * the actual deltification algorithm is described in deltify(Token)
	 * 
	 * @param result current ASR hypothesis
	 */
	public synchronized void deltify(Result result) {
		currentFrame = result.getFrameNumber();
		if (result.isFinal())
			recoFinal = true;
		Token t = result.getBestToken();
		if (t != null)
			deltify(result.getBestToken());
		else {
			System.err.println("there was no best token at frame " + currentFrame);
			wordEdits = new LinkedList<EditMessage<WordIU>>();
		}
	}

	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		return wordEdits;
	}

	public synchronized List<WordIU> getWordIUs() {
		return wordIUs;
	}
	
	public synchronized int getCurrentFrame() {
		return currentFrame + currentOffset;
	}
	
	public synchronized double getCurrentTime() {
		return (currentFrame + currentOffset) * 0.001;
	}

	@Override
	public void reset() {
		wordIUs = new IUList<WordIU>();
		recoFinal = false;
	}

	/**
	 *  there are two alternatives to set the offset:
	 *  - put an OffsetAdapter into the FrontEnd (after VAD)    
	 * 	- use signalOccurred() below; for this you have to call 
	 *    FrontEnd.addSignalListener(deltifier) somewhere (CurrentHypothesis-setup)
	 *    
	 *     here, the offset is given in centiseconds (frames)
	 */
	public void setOffset(int currentOffset) {
		System.err.println("SETTING OFFSET TO " + currentOffset);
		this.currentOffset = currentOffset;
	}
	
	@Override
	public void signalOccurred(Signal signal) {
		System.err.println(signal.toString());
		if (signal instanceof DataStartSignal) {
			startTime = signal.getTime();
		} else if (signal instanceof SpeechStartSignal) {
			setOffset((int) (signal.getTime() - startTime) / 10);
		}
	}
	
}
