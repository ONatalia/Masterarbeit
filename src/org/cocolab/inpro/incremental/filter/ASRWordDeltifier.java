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
package org.cocolab.inpro.incremental.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.ASRResultKeeper;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.SyllableIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.incremental.util.WordUtil;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.emory.mathcs.backport.java.util.Collections;


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
public class ASRWordDeltifier implements Configurable, Resetable, ASRResultKeeper {

	IUList<WordIU> wordIUs = new IUList<WordIU>();
	IUList<SyllableIU> syllableIUs = new IUList<SyllableIU>();
	IUList<SegmentIU> segmentIUs = new IUList<SegmentIU>();

	List<EditMessage<WordIU>> wordEdits;
	
	int currentFrame;
	
	protected boolean recoFinal; // flag to avoid smoothing or fixed lags on final result
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
	}
	
	protected synchronized List<Token> getTokens(Token token) {
		return ResultUtil.getTokenList(token, true, true);
	}
	
	protected synchronized void deltify(Token token) {
		List<Token> newTokens = getTokens(token);
		Collections.reverse(newTokens);
		List<WordIU> prevWordIUs = wordIUs;
		wordIUs = new IUList<WordIU>();
		// step over wordIUs and newWords to see which are equal in both
		ListIterator<Token> newIt = newTokens.listIterator();
		ListIterator<WordIU> prevIt = prevWordIUs.listIterator();
		List<Label> segmentLabels = new LinkedList<Label>(); // this is where we accumulate phonemes for the word that follows
		double segmentStartTime = 0.0;
		double segmentEndTime = 0.0;
		while (newIt.hasNext() && prevIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			if (newSearchState instanceof UnitSearchState) {
				if (newIt.hasNext()) {
					segmentEndTime = newIt.next().getFrameNumber() * 0.01;
					newIt.previous();
				}
				segmentLabels.add(new Label(segmentStartTime, segmentEndTime, 
											((UnitSearchState) newSearchState).getUnit().getName()));
				segmentStartTime = segmentEndTime;
			} else if (newSearchState instanceof WordSearchState) {
				Pronunciation pron = ((WordSearchState) newToken.getSearchState()).getPronunciation();
				WordIU prevIU = prevIt.next();
				// check if the words match and break once we reach the point where they don't match anymore
				if (!prevIU.wordEquals(pron)) {
					prevIt.previous(); // go back the one word that didn't match anymore
					newIt.previous();
					break;
				}
				// if words match, update the segments with the segment boundaries from the segmentLabels list
				prevIU.updateSegments(segmentLabels);
				wordIUs.add(prevIU);
				segmentLabels = new LinkedList<Label>();
			} else {
				throw new RuntimeException("Nobody expects the Spanish inquisition!");
			}
		}
		// ok, now:
		// if there are words left in the prev word list, send purge notifications
		// purge notifications have to be sent in reversed order, starting with the very last word
		// therefore we put them in reverse order into a new list
		wordEdits = new LinkedList<EditMessage<WordIU>>();
		while (prevIt.hasNext()) {
			wordEdits.add(0, new EditMessage<WordIU>(EditType.REVOKE, prevIt.next()));
		}
		// for the remaining words in the new list, add them to the old list and send add notifications
		while (newIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			if (newSearchState instanceof UnitSearchState) {
				if (newIt.hasNext()) {
					segmentEndTime = newIt.next().getFrameNumber() * 0.01;
					newIt.previous();
				}
				segmentLabels.add(new Label(segmentStartTime, segmentEndTime, 
											((UnitSearchState) newSearchState).getUnit().getName()));
				segmentStartTime = segmentEndTime;
			} else if (newSearchState instanceof WordSearchState) {
				Pronunciation pron = ((WordSearchState) newSearchState).getPronunciation();
				WordIU newIU = WordUtil.wordFromPronunciation(pron);
				newIU.updateSegments(segmentLabels);
				segmentLabels = new LinkedList<Label>();
				wordIUs.add(newIU);
				wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
			} else {
				throw new RuntimeException("Nobody expects the Spanish inquisition!");
			}
		}
	}
	
	public synchronized void deltify(Result result) {
		currentFrame = result.getFrameNumber();
		if (result.isFinal())
			recoFinal = true;
		deltify(result.getBestToken());
	}

	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		return wordEdits;
	}

	public synchronized List<WordIU> getWordIUs() {
		return wordIUs;
	}
	
	public synchronized int getCurrentFrame() {
		return currentFrame;
	}
	
	public synchronized double getCurrentTime() {
		return currentFrame * 0.01;
	}

	@Override
	public void reset() {
		wordIUs = new IUList<WordIU>();
		recoFinal = false;
	}
	
}
