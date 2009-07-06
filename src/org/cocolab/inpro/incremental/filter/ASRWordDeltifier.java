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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.ASRResultKeeper;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.instrumentation.Resetable;
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
public class ASRWordDeltifier implements Configurable, Resetable, ASRResultKeeper {

	IUList<WordIU> wordIUs = new IUList<WordIU>();
	List<EditMessage<WordIU>> edits;
	
	int currentFrame;
	
	protected boolean recoFinal; // flag to avoid smoothing or fixed lags on final result
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
	}
	
	protected synchronized List<Label> getWordLabels(Token token) {
/*		
		if (wordState.getPronunciation() instanceof SyllableAwarePronunciation) {
			SyllableAwarePronunciation pron = (SyllableAwarePronunciation) wordState.getPronunciation();
			List<Unit[]> sylls = pron.getSyllables();
			System.err.print(pron.getWord().toString() + " ");
			for (Unit[] syll : sylls) {
				for (Unit unit : syll) {
					System.err.print(unit.getName() + " ");
				}
				System.err.print("- ");
			}
			System.err.println();
		}
*/
		return ResultUtil.getWordLabelSequence(token);
	}
	
	protected synchronized void deltify(Token token) {
		List<Label> newWords = getWordLabels(token);
		IUList<WordIU> prevWordIUs = wordIUs;
		wordIUs = new IUList<WordIU>();
		// step over wordIUs and newWords to see which are equal in both
		Iterator<Label> newIt = newWords.iterator();
		Iterator<WordIU> prevIt = prevWordIUs.iterator();
		int i = 0;
		while (newIt.hasNext() && prevIt.hasNext()) {
			WordIU prevIU = prevIt.next();
			Label newLabel = newIt.next();
			if (!prevIU.wordEquals(newLabel.getLabel())) {
				break;
			}
			prevIU.updateLabel(newLabel);
			wordIUs.add(prevIU);
			i++;
		}
		prevIt = prevWordIUs.listIterator(i);
		newIt = newWords.listIterator(i);
		// if there are words left in the prev word list, send purge notifications
		// purge notifications have to be sent in reversed order, starting with the very last word
		// therefore we have to put them in reverse order into a new list and only revoke them afterwards
		edits = new LinkedList<EditMessage<WordIU>>();
		while (prevIt.hasNext()) {
			edits.add(0, new EditMessage<WordIU>(EditType.REVOKE, prevIt.next()));
		}
		// for the remaining words in the new list, add them to the old list and send add notifications
		while (newIt.hasNext()) {
			WordIU iu = new WordIU(newIt.next());
			wordIUs.add(iu);
			edits.add(new EditMessage<WordIU>(EditType.ADD, iu));
		}
	}

	public synchronized void deltify(Result result) {
		currentFrame = result.getFrameNumber();
		if (result.isFinal())
			recoFinal = true;
		deltify(result.getBestToken());
	}

	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		return edits;
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
