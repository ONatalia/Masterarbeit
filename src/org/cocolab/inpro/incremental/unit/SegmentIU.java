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
import java.util.Locale;
import java.util.Set;

import org.cocolab.inpro.annotation.Label;

public class SegmentIU extends IU {
	
	public static final Set<String> SILENCE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"<sil>", "SIL", "<p:>", "<s>", "</s>", "")));
	public static final Set<String> VOWELS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"2:", "9", "@", "@U", "6", "a", "a:", "aa:", "A:", "aa", "A", "ai", "aI", "au", "aU", "e", "e:", "ee", "E", "ee:", "E:", "ei", "eI", "i:", "i", "ii", "I", "o", "o:", "oo", "O", "oy", "OY", "u:", "u", "ui", "uI", "ui:", "uu", "U", "y:", "y", "yy", "Y")));
// for swedish:
/*			"A", "`A", "'A", "'A:", "''A", "''A:", "Å", "Å:", "`Å:", "'Å", "'Å:", "''Å", "''Å:", "Ä", "`Ä", "`Ä:", "'Ä", "'Ä:", "''Ä", "''Ä:", "A:_1", "'Ä3", "Ä4", "'Ä4", "''Ä4", 
			"E", "E:", "`E:", "'E", "'E:", "''E", "''E:", "E0", "'E0", 
			"I", "I:", "'I", "'I:", "''I", "''I:", 
			"O", "'O", "'O:", "''O:", "''Ö:", "'Ö:", "'Ö3", "''Ö3", "Ö4", "''Ö4", 
			"U", "U:", "'U", "'U:", "''U", "''U:", "'Y", "'Y:", "''Y")));
*/
	public static final Set<String> CONSONANTS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"b", "cc", "C", "d", "f", "g", "h", "j", "k", "l", "m", "n", "nn", "N", "p", "qq", "Q", "r", "s", "ss", "S", "t", "v", "x", "z", "Z")));
// for swedish:
/*			"b", "B", "d", "D", 
			"F", "g", "G", "H", 
			"J", "k", "K", "L", "M", "''M", "N", "NG", 
			"p", "P", "R", "S", 
			"SJ", "t", "T", 
			"V")));
*/
	// we keep start time, end time and text of the segment in a label
	Label l;
	
	public SegmentIU(String segment, SegmentIU sll) {
		super(sll);
		assert (SILENCE.contains(segment) || VOWELS.contains(segment) || CONSONANTS.contains(segment)) : "segment " + segment + " is neither a vowel, consonant nor silence I could understand.";
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
/* this code is helpful for prosodic feature extraction *r/
		if (edit == EditType.COMMIT) {
			double time = startTime() + 0.01;
			for (; time <= endTime() + 0.00001; time += 0.01) { 
				System.err.printf(Locale.US, 
				                  "%.2f\t%f\t%f\t%f\t\"%s\"\t%s\n", 
				                  time, 
				                  bd.getLoudness(time),
				                  bd.getPitchInCent(time),
				                  bd.getVoicing(time),
				                  l.getLabel(),
				                  isVowel() ? 'V' : isSilence() ? 'S' : 'C');
			}
		}
/**/
	}
	
	@Override
	public String toOAAString() {
		StringBuilder sb = new StringBuilder(Integer.toString(id));
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
