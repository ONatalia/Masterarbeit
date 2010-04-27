/* 
 * Copyright 2009, Timo Baumann and the Inpro project
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
package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.nlu.AVPair;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

public class WordIU extends IU {

	/* TODO: implement magic to actually fill this map */
	static Map<String, List<AVPair>> avPairs;
	
	final boolean isSilence;
	final Pronunciation pron;
	final String word;

	public WordIU(Pronunciation pron, WordIU sll, List<IU> groundedIn) {
		this(pron.getWord().getSpelling(), pron, sll, groundedIn);
	}
	
	protected WordIU(String word, Pronunciation pron, WordIU sll, List<IU> groundedIn) {
		super(sll, groundedIn, true);
		this.pron = pron;
		this.word = word;
		isSilence = this.word.equals(("<sil>"));
	}
	
	/**
	 * create a new silent word
	 * @param sll
	 */
	public WordIU(WordIU sll) {
		super(sll, Collections.nCopies(1, 
					(IU) new SyllableIU(null, Collections.nCopies(1, 
								(IU) new SegmentIU("SIL", null)))), 
			true);
		this.pron = Pronunciation.UNKNOWN;
		this.word = "<sil>";
		isSilence = true;
	}
	
	public List<AVPair> getAVPairs() {
		return avPairs.get(this.getWord());
	}
	
	@SuppressWarnings("unchecked") // the untyped list in the call to Collections.checkedList
	public List<SegmentIU> getSegments() {
		List<IU> returnList;
		if ((groundedIn == null) || groundedIn.size() == 0) {
			returnList = Collections.emptyList();
		} else if (groundedIn.get(0) instanceof SegmentIU) {
			returnList = groundedIn;
		} else if (groundedIn.get(0) instanceof SyllableIU) {
			returnList = new ArrayList<IU>();
			for (IU gIn : groundedIn) {
				returnList.addAll(gIn.groundedIn);
			}
		} else {
			throw new RuntimeException("I don't know how to get segments from my groundedIn list");
		}
		return Collections.checkedList((List) returnList, SegmentIU.class);
	}
	
	public void updateSegments(List<Label> newLabels) {
		List<SegmentIU> segments = getSegments();
		assert (segments.size() >= newLabels.size())
			: "something is wrong when updating segments in word:"
			+ this.toString()
			+ "I was supposed to add the following labels:"
			+ newLabels
			+ "but my segments are:"
			+ segments;
		Iterator<SegmentIU> segIt = segments.iterator();
		for (Label label : newLabels) {
			segIt.next().updateLabel(label);
		}
	}
	
	public boolean wordEquals(Pronunciation pron) {
		// words are equal if their pronunciations match
		// OR if the word is silent and the other's pronunciation is silent as well
		return ((isSilence && pron.getWord().isFiller()) || this.pron.equals(pron));
	}
	
	public boolean wordEquals(WordIU iu) {
		return ((isSilence && iu.isSilence) || pron.equals(iu.pron));
	}
	
	public String toTEDviewXML() {
		double startTime = startTime();
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("' duration='");
		sb.append(Math.round((endTime() - startTime) * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("'> ");
		sb.append(word.replace("<", "&lt;").replace(">", "&gt;"));
		sb.append(" </event>");
		return sb.toString();
	}
	
	@Override
	public String toLabelLine() {
		return String.format(Locale.US,	"%.2f\t%.2f\t%s", startTime(), endTime(), word);
	}

	public String toOAAString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append(",'");
		sb.append(word);
		sb.append("'");
		return sb.toString();
	}
	
	public String getWord() {
		return word;
	}
	
	public boolean isSilence() {
		return isSilence;
	}
	
	public boolean hasProsody() {
		return false;
	}

	/**
	 * @precondition: only call this if hasProsody()
	 */
	public boolean pitchIsRising() {
		assert hasProsody();
		return false;
	}
	
	public static void setAVPairs(Map<String, List<AVPair>> avPairs) {
		assert (WordIU.avPairs == null) : "You're trying to re-set avPairs. This may be a bug.";
		WordIU.avPairs = avPairs;
	}

}
