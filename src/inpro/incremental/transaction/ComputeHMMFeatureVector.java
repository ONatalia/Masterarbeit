package inpro.incremental.transaction;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import inpro.config.SynthesisConfig;
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
    	FULL_CONTEXT;
    }
    static EnumMap<FeatureClasses, List<String>> featureClasses = new EnumMap<FeatureClasses, List<String>>(FeatureClasses.class);
    
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
            	if (featDef.hasFeature(featureName)) { // allow missing features, saves the trouble of making featureClasses language-dependent
	                int featureIndex = featDef.getFeatureIndex(featureName);
	                byteValues[featureIndex] = SynthesisConfig.getDefaultInstance().getDTreeFeatureDefaults()[featureIndex];
            	}
            }
        }
        return new FeatureVector(byteValues, null, null, 0);
    }
    
    static EnumSet<FeatureClasses> allClasses = EnumSet.allOf(FeatureClasses.class);
    static EnumSet<FeatureClasses> currentWordIUinformed(SysSegmentIU siu, EnumSet<FeatureClasses> base) {
    	// we can safely include the next but one segment, if the next but one segment has the same syllable as this one  
    	if (siu.getFromNetwork("next", "next", "up") == siu.getFromNetwork("up"))
    		base.add(FeatureClasses.NEXT_2PHONE);
    	// we can safely include the next syllable if its part of the current word
    	if (siu.getFromNetwork("up", "next", "up") == siu.getFromNetwork("up", "up")) 
    		base.add(FeatureClasses.NEXT_SYLLABLE);
    	return base;
    }
    static EnumSet<FeatureClasses> currentPhraseIUinformed(SysSegmentIU siu, EnumSet<FeatureClasses> base) {
    	EnumSet<FeatureClasses> currentWordFeatures = currentWordIUinformed(siu, base);
    	SysSegmentIU nextWordFirstSegment = (SysSegmentIU) siu.getFromNetwork("up", "up", "down[-1]", "down[-1]", "next");
    	IU myPhrase = siu.getFromNetwork("up", "up", "up");
    	// we can include phrase info for the last word of the phrase
    	// i.e. if the next word's phrase differs from the current word's phrase
    	if (nextWordFirstSegment == null || 
    		nextWordFirstSegment.getFromNetwork("up", "up", "up") != myPhrase || 
    		(nextWordFirstSegment.isSilence() && nextWordFirstSegment.getFromNetwork("next", "up", "up", "up") != myPhrase)) { 
    		currentWordFeatures.add(FeatureClasses.CURRPHRASE);
    		currentWordFeatures.add(FeatureClasses.CURRPHRASE_ENDING);
    		//System.out.println("***"); // --> indicates how often this constraint is met. should be as many occurrences as the last word in the phrase has segments (plus one for the final pause)
    		//HTSModelComparator.active = false;
    	} else {
    		//HTSModelComparator.active = true;
    	}
    	return currentWordFeatures;
    }
    static EnumSet<FeatureClasses> currentUtteranceIUinformed(SysSegmentIU siu, EnumSet<FeatureClasses> base) {
    	EnumSet<FeatureClasses> currentPhraseFeatures = currentPhraseIUinformed(siu, base);
    	SysSegmentIU nextWordFirstSegment = (SysSegmentIU) siu.getFromNetwork("up", "up", "down[-1]", "down[-1]", "next");
    	// if the word is the last in the utterance (no phoneme follows after this word), we can include all remaining information
    	if (nextWordFirstSegment == null || (nextWordFirstSegment.isSilence() && nextWordFirstSegment.getFromNetwork("next") == null)) {
    		//System.out.println("***"); // --> indicates how often this constraint is met. should be as many occurrences as the last word in the utterance has segments (plus one for the final pause) 
    		currentPhraseFeatures.add(FeatureClasses.FULL_CONTEXT);
    		//HTSModelComparator.active = false;
    	} else {
    		//HTSModelComparator.active = true;
    	}
    	return currentPhraseFeatures;
    }
    static EnumSet<FeatureClasses> currentPhraseAndWords = EnumSet.range(FeatureClasses.PAST, FeatureClasses.CURRPHRASE_ENDING); 
    static EnumSet<FeatureClasses> maximum = EnumSet.allOf(FeatureClasses.class);
    
	public static FeatureVector featuresForSegmentIU(SysSegmentIU siu) {
		EnumSet<FeatureClasses> featureSet = EnumSet.noneOf(FeatureClasses.class);
		switch (SynthesisConfig.getDefaultInstance().getDTreeContext()) {
		case PHRASE : 
			featureSet = EnumSet.range(FeatureClasses.PAST, FeatureClasses.CURRPHRASE_ENDING);
			break;
		case CURRWORDN2 : 
			featureSet = EnumSet.range(FeatureClasses.PAST, FeatureClasses.CURRWORD_ENDING);
			break;
		case FULL : 
			featureSet.add(FeatureClasses.FULL_CONTEXT);
			//$FALL-THROUGH$
		case IUINFORMED : 
			featureSet = currentUtteranceIUinformed(siu, featureSet);
			//$FALL-THROUGH$
		case CURRPHRASEIU : 
			featureSet = currentPhraseIUinformed(siu, featureSet);
			//$FALL-THROUGH$
		case CURRWORDIU : 
			featureSet = currentWordIUinformed(siu, featureSet);
			//$FALL-THROUGH$
		case CURRWORDN1 : 
			featureSet.add(FeatureClasses.CURRWORD);
			featureSet.add(FeatureClasses.CURRWORD_ENDING);
			//$FALL-THROUGH$
		case MINIMAL_LOOKAHEAD : 
			featureSet.add(FeatureClasses.CURRSYLL);
			featureSet.add(FeatureClasses.NEXT_1PHONE);
			//$FALL-THROUGH$
		case NO_LOOKAHEAD : 
			featureSet.add(FeatureClasses.PAST);
			//$FALL-THROUGH$
		case PHONE : 
			featureSet.add(FeatureClasses.CURRPHONE);
		}
		return substitueDefaultFeatures(featureSet, siu.hmmdata.getFeatureDefinition(), siu.fv);
	}
}
