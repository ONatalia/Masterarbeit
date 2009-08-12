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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cocolab.inpro.annotation.Label;

public class SegmentIU extends IU {
	
	public static final Set<String> SILENCE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"<sil>", "SIL", "<p:>", "<s>", "</s>")));
	public static final Set<String> VOWELS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"@", "a", "aa:", "ai", "au", "e:", "ee", "ee:", "ei", "i:", "ii", "o:", "oo", "oy", "u:", "ui", "ui:", "uu", "y:", "yy")));
	public static final Set<String> CONSONANTS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"b", "cc", "d", "f", "g", "h", "j", "k", "l", "m", "n", "nn", "p", "qq", "r", "s", "ss", "t", "v", "x", "z")));
	
	// we keep start time, end time and text of the segment in a label
	Label l;
	
	public SegmentIU(String segment, SegmentIU sll) {
		super(sll);
		this.l = new Label(segment);
	}

	public void updateLabel(Label l) {
		assert (this.l.getLabel().equals(l.getLabel())) : "my label is " + this.l.toString() + ", was asked to update with " + l.toString();
		this.l = l;
	}
	
	@Override
	public double startTime() {
		return l.getStart();
	}
	
	@Override
	public double endTime() {
		return l.getEnd();
	}

	public boolean isSilence() {
		return SILENCE.contains(l.getLabel()); 
	}
	
	public boolean isVowel() {
		return VOWELS.contains(l.getLabel());
	}
	
	@Override
	public void update(EditType edit) {
		if (edit == EditType.COMMIT) {
			System.err.print("I am the now commited ");
			System.err.print(isSilence() ? "silence " : isVowel() ? "vowel " : "consonant ");
			System.err.println(toString());
		}
	}
	
	@Override
	public String toOAAString() {
		StringBuffer sb = new StringBuffer(Integer.toString(id));
		sb.append(",'");
		sb.append(l.getLabel());
		sb.append("'");
		return sb.toString();
	}

	@Override
	public String toTEDviewXML() {
		return l.toTEDViewXML();
	}
	
	@Override
	public String toString() {
		return id + "," + l.toString();
	}
	
}
