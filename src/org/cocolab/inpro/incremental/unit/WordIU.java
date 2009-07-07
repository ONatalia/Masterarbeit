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
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.annotation.Label;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

public class WordIU extends IU {

	final Pronunciation pron;
	final String word;
	
	public WordIU(Pronunciation pron, WordIU sll, List<? extends IU> groundedIn) {
		super(sll, groundedIn, true);
		this.pron = pron;
		this.word = pron.getWord().getSpelling();
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
		if (segments.size() != newLabels.size()) {
			System.err.println("something is wrong:");
			System.err.println(segments);
			System.err.println(newLabels);
		}
		Iterator<Label> labelIt = newLabels.iterator();
		for (SegmentIU segment : segments) {
			segment.updateLabel(labelIt.next());
		}
	}
	
	public boolean wordEquals(Pronunciation pron) {
		return this.pron.equals(pron);
	}
	
	public boolean wordEquals(WordIU iu) {
		return pron.equals(iu.pron);
	}
	
	public String toTEDviewXML() {
		double startTime = startTime();
		return "<event time='"
				+ Math.round(startTime * 1000.0) 
				+ "' duration='"
				+ Math.round((endTime() - startTime) * 1000.0)
				+ "'> "
				+ word.replace("<", "&lt;").replace(">", "&gt;")
				+ " </event>";
	}
	
	public String toLabelLine() {
		return startTime() + "\t" + endTime() + "\t" + word;
	}

	public String toString() {
		return toLabelLine(); // + "\n";
	}
	
	public String toOAAString() {
		StringBuffer sb = new StringBuffer(Integer.toString(id));
		sb.append(",'");
		sb.append(word);
		sb.append("'");
		return sb.toString();
	}

}
