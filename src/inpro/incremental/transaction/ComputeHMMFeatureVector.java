package inpro.incremental.transaction;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.SysSegmentIU;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/** 
 * a class that computes a HMM Feature Vector according to some definition 
 * for one SysSegmentIU from the IU's given IU network
 */
public class ComputeHMMFeatureVector {
	
    private enum FeatureClasses { 
    	PAST, 
    	CURRPHONE, // pertaining to the current phone
    	NEXT_1PHONE, // pertaining to the next phone
    	NEXT_2PHONE, // the next but one phone
    	CURRSYLL, // the current syllable
    	NEXT_SYLLABLE, // stress-status of next syllable
    	CURRWORD, // pertaining to the current word (e.g. POS) 
    	CURRWORD_ENDING, // pertaining to knowing the end of the current word (e.g. phones till end of word)
    	NEXT_WORD, 
    	CURRPHRASE, // pertaining to the overall layout of the current phrase, INCLUDING ToBI
    	CURRPHRASE_ENDING, // number of things in phrase; position from phrase end
//    	CURRPHRASE_PLUS_FUTUREWORDS, // used to be the combination of CURRPHRASE and CURRPHRASE_ENDING
    	FULL_CONTEXT }
    static EnumMap<FeatureClasses, List<String>> featureClasses = new EnumMap<FeatureClasses, List<String>>(FeatureClasses.class);
    static byte[] featureDefaultsGerman = { // on my corpus with BITS-1 voice
        43, 0, 2, 1, 0, 0, 0, 6, 2, 0, 1, 0, 0, 1, 1, 43, 1, 0, 0, 0, 0, 43, 1, 1, 1, 0, 0, 2, 2, 0, 1, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 0, 9, 12, 9, 0, 0, 1, 1, 1, 6, 2, 0, 0, 0, 43, 0, 2, 0, 0, 43, 2, 0, 0, 0, 0, 0, 1, 1, 2, 0, 2, 0, 0, 1, 1, 2, 3, 0, 1, 1, 9, 1, 1, 3, 4, 0, 1, 3, 6, 7, 2, 1, 1, 1, 2, 1, 0, 0, 0, 6, 2, 3, 4, 4, 4, 4, 4
    };
    static byte[] featureDefaultsEnglish = { // on my corpus with CMU-SLT voice
        1, 0, 2, 2, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 17, 2, 0, 0, 0, 0, 35, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 2, 1, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 6, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 2, 0, 0, 0, 0, 0, 1, 1, 1, 0, 2, 0, 0, 1, 0, 0, 2, 0, 0, 12, 0, 1, 4, 4, 0, 1, 0, 0, 7, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0
    };
    static {
        featureClasses.put(FeatureClasses.PAST, Arrays.asList("prev_phone", "prev_accent", "prev_cplace", "prev_ctype", "prev_cvox", "prev_vc", "prev_vfront", "prev_vheight", "prev_vlng", "prev_vrnd", "prev_is_pause", "prev_prev_cplace", "prev_prev_ctype", "prev_prev_cvox", "prev_prev_phone", "prev_prev_vc", "prev_prev_vfront", "prev_prev_vheight", "prev_prev_vlng", "prev_prev_vrnd", "segs_from_syl_start", "prev_stressed", "prev_syl_break", "segs_from_word_start", "syls_from_word_start", "syls_from_prev_accent", "syls_from_prev_stressed", "syls_from_phrase_start", "stressed_syls_from_phrase_start", "accented_syls_from_phrase_start", "words_from_phrase_start", "words_from_prev_punctuation", "words_from_sentence_start", "prev_punctuation", "prev_phrase_endtone", "phrases_from_sentence_start"));
        featureClasses.put(FeatureClasses.CURRPHONE, Arrays.asList("phone", "ph_cplace", "ph_ctype", "ph_cvox", "ph_vc", "ph_vfront", "ph_vheight", "ph_vlng", "ph_vrnd", "style"));
        featureClasses.put(FeatureClasses.CURRSYLL, Arrays.asList("onsetcoda", "pos_in_syl", "syl_numsegs", "segs_from_syl_end", "stressed", "accented", "syl_break", "position_type"));
        featureClasses.put(FeatureClasses.NEXT_1PHONE, Arrays.asList("next_phone", "next_is_pause", "selection_next_phone_class", "next_cplace", "next_ctype", "next_cvox", "next_vc", "next_vfront", "next_vheight", "next_vlng", "next_vrnd"));
        featureClasses.put(FeatureClasses.NEXT_2PHONE, Arrays.asList("next_next_phone", "next_next_cplace", "next_next_ctype", "next_next_cvox", "next_next_vc", "next_next_vfront", "next_next_vheight", "next_next_vlng", "next_next_vrnd"));
        featureClasses.put(FeatureClasses.CURRWORD, Arrays.asList(
        		"pos", "gpos", "word_frequency")); 
        featureClasses.put(FeatureClasses.CURRWORD_ENDING, Arrays.asList(
                "segs_from_word_end", "syls_from_word_end", 
                "word_numsegs", "word_numsyls")); // current word
        featureClasses.put(FeatureClasses.NEXT_SYLLABLE, Arrays.asList(
                "next_accent", "next_stressed")); // next syllable
        featureClasses.put(FeatureClasses.NEXT_WORD, Arrays.asList(
                "next_wordbegin_cplace", "next_wordbegin_ctype", "next_pos")); // next word
        featureClasses.put(FeatureClasses.CURRPHRASE, Arrays.asList(
                "edge", "selection_prosody", 
                "breakindex", "tobi_accent", "tobi_endtone", "phrase_endtone",
                "next_punctuation"));
        featureClasses.put(FeatureClasses.CURRPHRASE_ENDING, Arrays.asList(
                "accented_syls_from_phrase_end", 
                "words_to_next_punctuation", "words_from_phrase_end",  
                "phrase_numsyls", "phrase_numwords")); // current phrase
        featureClasses.put(FeatureClasses.FULL_CONTEXT, Arrays.asList(
                "syls_to_next_accent", "syls_to_next_stressed", 
                "syls_from_phrase_end", "stressed_syls_from_phrase_end", 
                "next_tobi_accent", "next_tobi_endtone", "nextnext_tobi_accent", "nextnext_tobi_endtone",
                "words_from_sentence_end", "phrases_from_sentence_end", 
                "sentence_numphrases", "sentence_numwords", "sentence_punc"));
    }
    
    //{ PAST, CURRPHONE, CURRSYLL, PLUS_1PHONE, PLUS_2PHONE, CURRWORD, PLUS_NEXTSYLLABLE, CURRPHRASE_PLUS_FUTUREWORDS, FULL_CONTEXT }
    private static FeatureVector substitueDefaultFeatures(EnumSet<FeatureClasses> includedClasses, FeatureDefinition featDef, FeatureVector fv) {
        byte[] byteValues = Arrays.copyOf(fv.getByteValuedDiscreteFeatures(), fv.getByteValuedDiscreteFeatures().length);
        for (FeatureClasses overwriteFeature : EnumSet.complementOf(includedClasses)) {
            for (String featureName : featureClasses.get(overwriteFeature)) { 
                int featureIndex = featDef.getFeatureIndex(featureName);
                byteValues[featureIndex] = featureDefaultsGerman[featureIndex];
            }
        }
        return new FeatureVector(byteValues, null, null, 0);
    }
    
    static EnumSet<FeatureClasses> allClasses = EnumSet.allOf(FeatureClasses.class);
    static EnumSet<FeatureClasses> minimum = EnumSet.of(FeatureClasses.PAST, FeatureClasses.CURRPHONE);
    static EnumSet<FeatureClasses> reasonableMinimum = EnumSet.of(FeatureClasses.PAST, FeatureClasses.CURRPHONE, FeatureClasses.CURRSYLL, FeatureClasses.NEXT_1PHONE);
    static EnumSet<FeatureClasses> currentWordN1 = EnumSet.of(FeatureClasses.PAST, FeatureClasses.CURRPHONE, FeatureClasses.CURRSYLL, FeatureClasses.NEXT_1PHONE, FeatureClasses.CURRWORD, FeatureClasses.CURRWORD_ENDING);
    static EnumSet<FeatureClasses> currentWordN2 = EnumSet.of(FeatureClasses.PAST, FeatureClasses.CURRPHONE, FeatureClasses.CURRSYLL, FeatureClasses.NEXT_1PHONE, FeatureClasses.NEXT_2PHONE, FeatureClasses.CURRWORD, FeatureClasses.CURRWORD_ENDING);
    static EnumSet<FeatureClasses> currentWordIUinformed(SysSegmentIU siu) {
    	EnumSet<FeatureClasses> iuInfoCurrentWord = currentWordN1;
    	// remove the next phoneme if it's not part of the current word
    	//if (siu.getFromNetwork("next", "up", "up") != siu.getFromNetwork("up", "up"))
    	//	iuInfoCurrentWord.remove(FeatureClasses.NEXT_1PHONE);
    	// we can safely include the next but one segment, if the next but one segment has the same syllable as this one  
    	if (siu.getFromNetwork("next", "next", "up") == siu.getFromNetwork("up"))
    		iuInfoCurrentWord.add(FeatureClasses.NEXT_2PHONE);
    	// we can safely include the next syllable if its part of the same word
    	if (siu.getFromNetwork("up", "next", "up") == siu.getFromNetwork("up", "up")) 
    		iuInfoCurrentWord.add(FeatureClasses.NEXT_SYLLABLE);
    	// we can include phrase info for the last word of the phrase
    	// i.e. if the next word's phrase differs from the current word's phrase
    	SysSegmentIU nextWordFirstSegment = (SysSegmentIU) siu.getFromNetwork("up", "up", "down[-1]", "down[-1]", "next");
    	IU myPhrase = siu.getFromNetwork("up", "up", "up");
    	if (nextWordFirstSegment == null || 
    		nextWordFirstSegment.getFromNetwork("up", "up", "up") != myPhrase || 
    		(nextWordFirstSegment.isSilence() && nextWordFirstSegment.getFromNetwork("next", "up", "up", "up") != myPhrase)) { 
    		iuInfoCurrentWord.add(FeatureClasses.CURRPHRASE);
    		iuInfoCurrentWord.add(FeatureClasses.CURRPHRASE_ENDING);
    		//System.out.println("***"); // --> indicates how often this constraint is met. should be as many occurrences as the last word in the phrase has segments (plus one for the final pause)
    		//HTSModelComparator.active = false;
    	} else {
    		//HTSModelComparator.active = true;
    	}
    	// if the word is the last in the utterance (no phoneme follows after this word), we can include all remaining information
    	if (nextWordFirstSegment == null || (nextWordFirstSegment.isSilence() && nextWordFirstSegment.getFromNetwork("next") == null)) {
    		//System.out.println("***"); // --> indicates how often this constraint is met. should be as many occurrences as the last word in the utterance has segments (plus one for the final pause) 
    		//iuInfoCurrentWord.add(FeatureClasses.FULL_CONTEXT);
    		//HTSModelComparator.active = false;
    	} else {
    		//HTSModelComparator.active = true;
    	}
    	return iuInfoCurrentWord;
    }
    static EnumSet<FeatureClasses> currentPhraseAndWords = EnumSet.range(FeatureClasses.PAST, FeatureClasses.CURRPHRASE_ENDING); 
    static EnumSet<FeatureClasses> maximum = EnumSet.allOf(FeatureClasses.class);
    
	public static FeatureVector featuresForSegmentIU(SysSegmentIU siu) {
        
		return substitueDefaultFeatures(maximum, siu.hmmdata.getFeatureDefinition(), siu.fv);
	}
}
