package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.audio.DDS16kAudioInputStream;
import org.cocolab.inpro.tts.hts.VocodingAudioStream;

/**
 * multiple synthesis options are organized in a tree structure.
 * this is incredibly inefficient if you've got many options. the XX YY ZZ AA BB CC (with each a few options) results in plenty possible variants
 * at the same time, this doesn't even allow for lengthening/speed changes
 * @author timo
 */
public class IncrSysInstallmentIU extends SysInstallmentIU {

	public IncrSysInstallmentIU(String base) {
		super(base);
	}
	
	public IncrSysInstallmentIU(List<String> variants) {
		this(variants.get(0));
		for (int i = 1; i < variants.size(); i++) {
			addAlternativeVariant(variants.get(i));
		}
	}
	
	public void addAlternativeVariant(String variant) {
		SysInstallmentIU varInst = new SysInstallmentIU(variant);
		WordIU commonWord = getInitialWord();
		// variant word
		WordIU varWord = varInst.getInitialWord();
		assert (commonWord.spellingEquals(varWord)) : "installment variants must all start with the same word!";
		boolean hasVarWord = true;
		while (hasVarWord) {
			hasVarWord = false;
			varWord = (WordIU) varWord.getNextSameLevelLink();
			for (IU nextIU : commonWord.getNextSameLevelLinks()) {
				WordIU nextWord = (WordIU) nextIU;
				if (nextWord.spellingEquals(varWord)) {
					hasVarWord = true;
					commonWord = nextWord;
					break; // next while loop
				}
			}
		}
		if (varWord != null) {
			varWord.connectSLL(commonWord);
			WordIU groundingWord = varWord;
			while (groundingWord != null) {
				groundedIn.add(groundingWord);
				groundingWord = (WordIU) groundingWord.getNextSameLevelLink();
			}
			// now shift segment times for the variant to match that of the common root
			SysSegmentIU firstVarSegment = (SysSegmentIU) varWord.getSegments().get(0);
			SegmentIU lastCommonSegment = commonWord.getLastSegment();
			firstVarSegment.shiftBy(lastCommonSegment.endTime() - firstVarSegment.startTime(), true);
		}
	}
	
	public void appendContinuation(List<WordIU> words) {
		WordIU oldLastWord = getFinalWord();
		WordIU newFirstWord = words.get(0);
		newFirstWord.connectSLL(oldLastWord);
		groundedIn.addAll(words);
/* TODO: pitch adaptation will have to be reworked
		// adapt pitch
		SysSegmentIU oldLastSegment = (SysSegmentIU) oldLastWord.getLastSegment();
		while (oldLastSegment != null && !oldLastSegment.isVoiced()) {
			oldLastSegment = (SysSegmentIU) oldLastSegment.getSameLevelLink();
		}
		SysSegmentIU newFirstSegment = (SysSegmentIU) newFirstWord.getFirstSegment();
		while (newFirstSegment != null && !newFirstSegment.isVoiced()) {
			newFirstSegment = (SysSegmentIU) newFirstSegment.getNextSameLevelLink();
		}
		// attain previous IU's pitch for a smoother transition to the next
		//if (oldLastSegment != null && newFirstSegment != null)
		//	oldLastSegment.attainPitch(newFirstSegment.getFirstVoicedlf0());
		 */
	}
	
	public void reorderOptions(int wordIndex, final String newBestFollower) {
		WordIU word = getWords().get(wordIndex - 1);
		word.setAsTopNextSameLevelLink(newBestFollower);
	}
	
	@Override
	public AudioInputStream getAudio() {
        boolean immediateReturn = true;
		VocodingAudioStream vas = new VocodingAudioStream(getFullPStream(), immediateReturn);
        return new DDS16kAudioInputStream(vas);
	}
	
	public List<WordIU> getWordsAtPos(int pos) {
		WordIU wordIU = getInitialWord();
		return getWordsAtPos(Collections.<WordIU>singletonList(wordIU), pos);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<WordIU> getWordsAtPos(List<WordIU> parentWords, int i) {
		if (i == 0) {
			return parentWords;
		} else {
			List<WordIU> daughterWords = new ArrayList<WordIU>();
			for (WordIU parent : parentWords) {
				List<WordIU> daughters = (List) parent.getNextSameLevelLinks();
				if (daughters != null)
					daughterWords.addAll(daughters);
			}
			return getWordsAtPos(daughterWords, i - 1);
		}
	}

}