package org.cocolab.inpro.incremental.util;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.incremental.BaseDataKeeper;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.SyllableIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.sphinx.linguist.dictionary.SyllableAwarePronunciation;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

public class WordUtil {

	public static WordIU wordFromPronunciation(Pronunciation pron, WordIU wordSLL, SyllableIU syllSLL, SegmentIU segmentSLL, BaseDataKeeper bd) {
		List<Unit[]> sylls;
		if (pron instanceof SyllableAwarePronunciation) {
			sylls = ((SyllableAwarePronunciation) pron).getSyllables();
		} else {
			sylls = new ArrayList<Unit[]>(1);
			sylls.add(pron.getUnits());
		}
		List<SyllableIU> syllIUs = new ArrayList<SyllableIU>(sylls.size());
		for (Unit[] syll : sylls) {
			List<SegmentIU> segments = new ArrayList<SegmentIU>(syll.length);
			for (Unit unit : syll) {
				SegmentIU segIU = new SegmentIU(unit.getName(), segmentSLL, bd);
				segments.add(segIU);
				segmentSLL = segIU;
			}
			SyllableIU syllIU = new SyllableIU(syllSLL, segments, bd); 
			syllIUs.add(syllIU);
			syllSLL = syllIU;
			
		}
		return new WordIU(pron, wordSLL, syllIUs, bd);
	}

	public static WordIU wordFromPronunciation(Pronunciation pron, BaseDataKeeper bd) {
		return wordFromPronunciation(pron, null, null, null, bd);
	}
}
