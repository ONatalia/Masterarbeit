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
import java.util.Map;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.dialogmanagement.composer.AVPair;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

public class WordIU extends IU {

	/* TODO: implement magic to actually fill this map */
	static Map<String, List<AVPair>> avPairs;
	
	final boolean isSilence;
	final Pronunciation pron;
	final String word;

	public WordIU(Pronunciation pron, WordIU sll, List<? extends IU> groundedIn) {
		this(pron.getWord().getSpelling(), pron, sll, groundedIn);
	}
	
	protected WordIU(String word, Pronunciation pron, WordIU sll, List<? extends IU> groundedIn) {
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
					new SyllableIU(null, Collections.nCopies(1, 
								new SegmentIU("SIL", null)))), 
			true);
		this.pron = Pronunciation.UNKNOWN;
		this.word = "<sil>";
		isSilence = true;
	}
	
	public List<AVPair> getAVPairs() {
		return avPairs.get(this.word);
	}
	
	@SuppressWarnings("unchecked")
	public List<SegmentIU> getSegments() {
		if ((groundedIn == null) || groundedIn.size() == 0) { 
			return null;
		} else if (groundedIn.get(0) instanceof SegmentIU) {
			return (List<SegmentIU>) groundedIn;
		} else if (groundedIn.get(0) instanceof SyllableIU) {
			List<SegmentIU> returnList = new ArrayList<SegmentIU>();
			for (IU gIn : groundedIn) {
				returnList.addAll((List<SegmentIU>) gIn.groundedIn);
			}
			return returnList;
		} else {
			throw new RuntimeException("I don't know how to get segments from my groundedIn list");
		}
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
		sb.append(Math.round(startTime * 1000.0));
		sb.append("' duration='");
		sb.append(Math.round((endTime() - startTime) * 1000.0));
		sb.append("'> ");
		sb.append(word.replace("<", "&lt;").replace(">", "&gt;"));
		sb.append(" </event>");
		return sb.toString();
	}
	
	public String toLabelLine() {
		return startTime() + "\t" + endTime() + "\t" + word;
	}

	public String toString() {
		return id + "," + toLabelLine(); // + "\n";
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
	
	public static void setAVPairs(Map<String, List<AVPair>> avPairs) {
		assert (WordIU.avPairs == null) : "You're trying to re-set avPairs. This may be a bug.";
		WordIU.avPairs = avPairs;
	}

}
