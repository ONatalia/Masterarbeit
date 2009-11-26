package org.cocolab.inpro.sphinx.linguist.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.WordClassification;

public class SyllableAwarePronunciation extends Pronunciation {

	List<Integer> syllableBoundaryIndices;

	SyllableAwarePronunciation(Unit[] units, String tag,
			WordClassification wordClassification, float probability) {
		this(units, null, tag, wordClassification, probability);
	}

	SyllableAwarePronunciation(Unit[] units, List<Integer> syllableBoundaries, String tag,
			WordClassification wordClassification, float probability) {
		super(units, tag, wordClassification, probability);
		this.syllableBoundaryIndices = syllableBoundaries;
	}
	
	SyllableAwarePronunciation(Unit[] units, List<Integer> syllableBoundaries) {
		this(units, syllableBoundaries, null, null, 1.0f);
	}
	
	public List<Unit[]> getSyllables() {
		Unit[] units = getUnits();
		List<Unit[]> sylls = new ArrayList<Unit[]>(syllableBoundaryIndices.size());
		int prevIndex = 0;
		for (Integer boundary : syllableBoundaryIndices) {
			sylls.add(Arrays.copyOfRange(units, prevIndex, boundary));
			prevIndex = boundary;
		}
		sylls.add(Arrays.copyOfRange(units, prevIndex, units.length));
		return sylls;
	}

}
