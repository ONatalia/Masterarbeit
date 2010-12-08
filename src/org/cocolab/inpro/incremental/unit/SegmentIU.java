package org.cocolab.inpro.incremental.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cocolab.inpro.annotation.Label;

public class SegmentIU extends IU {
	
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
			"b", "cc", "C", "d", "f", "g", "h", "j", "k", "l", "m", "n", "nn", "N", "p", "qq", "?", "Q", "r", "rr", "R", "s", "ss", "S", "t", "ts", "v", "x", "z", "Z")));
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
		assert (Label.SILENCE.contains(segment) || VOWELS.contains(segment) || CONSONANTS.contains(segment)) : "segment " + segment + " is neither a vowel, consonant nor silence I could understand.";
		this.l = new Label(segment);
	}
	
	public SegmentIU(Label l) {
		assert (Label.SILENCE.contains(l.getLabel()) || VOWELS.contains(l.getLabel()) || CONSONANTS.contains(l.getLabel())) : "segment " + l.getLabel() + " is neither a vowel, consonant nor silence I could understand.";
		this.l = l;
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
		return l.isSilence();
	}
	
	public boolean isVowel() {
		return VOWELS.contains(l.getLabel());
	}
	
	@Override
	public void update(EditType edit) {
/* this code is helpful for prosodic feature extraction * /
		if (edit == EditType.COMMIT) {
			double time = startTime() + 1 * ResultUtil.FRAME_TO_SECOND_FACTOR;
			for (; time <= endTime() + 0.00001; time += 1 * ResultUtil.FRAME_TO_SECOND_FACTOR) { 
				System.err.printf(Locale.US, 
				                  "%.2f\t%f\t%f\t%f\t%f\t%f\t\"%s\"\t%s\n", 
				                  time, 
				                  bd.getLoudness(time),
				                  bd.getPitchInCent(time),
				                  bd.getVoicing(time),
				                  bd.getSpectralTilt(time),
				                  bd.getSpectralTiltQual(time),
				                  l.getLabel(),
				                  isVowel() ? 'V' : isSilence() ? 'S' : 'C');
			}
		}
/**/
	}

	@Override
	public String toPayLoad() {
		return l.getLabel();
	}

	/** this segment represented as an mbrola line */
	public StringBuilder toMbrolaLine() {
		StringBuilder sb = new StringBuilder(l.toMbrola());
		sb.append("\n");
		return sb;
	}
	
}
