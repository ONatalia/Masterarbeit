/*
 * Portions Copyright 2009 Timo Baumann, InPro Project
 * 
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package org.cocolab.inpro.sphinx.linguist.dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;

public class SyllableAwareFullDictionary extends FullDictionary {

    public static final String SYLLABLE_BOUNDARY_SYMBOL = "-";

/*    public void clearDictionary() {
    	wordDictionary.clear();
    }
    
    public void addPronunciation(String text, Pronunciation pron) {
    	allowMissingWords = true;
    	Word word = getWord(text);
    	word.getPronunciations();
    }
*/
    /**
     * Loads the given sphinx3 style simple dictionary from the given InputStream. The InputStream is assumed to contain
     * ASCII data.
     *
     * @param inputStream  the InputStream of the dictionary
     * @param isFillerDict true if this is a filler dictionary, false otherwise
     * @throws java.io.IOException if there is an error reading the dictionary
     */
    protected Map<String, Word> loadDictionary(InputStream inputStream, boolean isFillerDict)
            throws IOException {
        Map<String, List<Pronunciation>> dictionary = new HashMap<String, List<Pronunciation>>();
        ExtendedStreamTokenizer est = new ExtendedStreamTokenizer(inputStream,
                true);
        String word;
        while ((word = est.getString()) != null) {
            word = removeParensFromWord(word);
            word = word.toLowerCase();
            List<Unit> units = new ArrayList<Unit>(20);
            String unitText;
            List<Integer> syllBoundaries = new ArrayList<Integer>();
            int counter = 0;
            while ((unitText = est.getString()) != null) {
            	if (unitText.equals(SYLLABLE_BOUNDARY_SYMBOL)) {
            		syllBoundaries.add(Integer.valueOf(counter));
            	} else {
                    units.add(getCIUnit(unitText, isFillerDict));
                    counter++;            		
            	}
            }
            Unit[] unitsArray = units.toArray(new Unit[units.size()]);
            List<Pronunciation> pronunciations = (List<Pronunciation>) dictionary.get(word);
            if (pronunciations == null) {
                pronunciations = new LinkedList<Pronunciation>();
            }
            Pronunciation pronunciation = new SyllableAwarePronunciation(unitsArray, syllBoundaries);
            pronunciations.add(pronunciation);
            // we never add a SIL ending duplicate
            dictionary.put(word, pronunciations);
        }
        inputStream.close();
        est.close();
        HashMap<String, Word> wordDictionary = createWords(dictionary, isFillerDict);
        return wordDictionary;
    }
    
}
