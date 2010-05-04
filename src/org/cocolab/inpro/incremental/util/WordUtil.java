package org.cocolab.inpro.incremental.util;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.SyllableIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.sphinx.linguist.dictionary.SyllableAwarePronunciation;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

/**
 * utility functions to build IU sub-networks for Sphinx' words
 */
public class WordUtil {

	/**
	 * creates a new word/syllable/segment network from the parameters
	 * @param pron the pronunciation to get sub-word information from
	 * @param wordSLL the SLL the WordIU will be connected to
	 * @param syllSLL the SLL the SyllIU will be connected to
	 * @param segmentSLL the SLL the SegmentIU will be connected to
	 * @return a new WordIU network for the given pronunciation
	 */
	public static WordIU wordFromPronunciation(Pronunciation pron, WordIU wordSLL, SyllableIU syllSLL, SegmentIU segmentSLL) {
		List<Unit[]> sylls;
		if (pron instanceof SyllableAwarePronunciation) {
			sylls = ((SyllableAwarePronunciation) pron).getSyllables();
		} else {
			sylls = new ArrayList<Unit[]>(1);
			sylls.add(pron.getUnits());
		}
		List<IU> syllIUs = new ArrayList<IU>(sylls.size());
		for (Unit[] syll : sylls) {
			List<IU> segments = new ArrayList<IU>(syll.length);
			for (Unit unit : syll) {
				SegmentIU segIU = new SegmentIU(unit.getName(), segmentSLL);
				segments.add(segIU);
				segmentSLL = segIU;
			}
			SyllableIU syllIU = new SyllableIU(syllSLL, segments); 
			syllIUs.add(syllIU);
			syllSLL = syllIU;
			
		}
		return new WordIU(pron, wordSLL, syllIUs);
	}

	/**
	 * creates a new word/syllable/segment network from the parameters
	 * @return a WordIU-network that is not connected to previous IUs via SLLs
	 */
	public static WordIU wordFromPronunciation(Pronunciation pron) {
		return wordFromPronunciation(pron, null, null, null);
	}
}
