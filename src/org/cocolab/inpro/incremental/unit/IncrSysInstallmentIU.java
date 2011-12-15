package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.util.data.audio.DDSAudioInputStream;

import org.cocolab.inpro.tts.MaryAdapter4internal;
import org.cocolab.inpro.tts.hts.FullPStream;
import org.cocolab.inpro.tts.hts.IUBasedFullPStream;
import org.cocolab.inpro.tts.hts.VocodingAudioStream;

/**
 * multiple synthesis options are organized in a tree structure.
 * this is incredibly inefficient if you've got many options. the XX YY ZZ AA BB CC (with each a few options) results in plenty possible variants
 * at the same time, this doesn't even allow for lengthening/speed changes
 * @author timo
 */
public class IncrSysInstallmentIU extends SysInstallmentIU {

	public IncrSysInstallmentIU(List<String> variants) {
		super(variants.get(0));
		System.err.println(toString());
		this.addFeatureStreamToSegmentIUs();
		for (int i = 1; i < variants.size(); i++) {
			SysInstallmentIU varInst = new SysInstallmentIU(variants.get(i));
			System.err.println(varInst.toString());
			varInst.addFeatureStreamToSegmentIUs();
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
//				varWord.getSameLevelLink().connectSLL(commonWord.getSameLevelLink());
//				((WordIU) varWord.getSameLevelLink()).word = "grüne";
				// now shift segment times for the variant to match that of the common root
				SysSegmentIU firstVarSegment = (SysSegmentIU) varWord.getSegments().get(0);
				SegmentIU lastCommonSegment = commonWord.getSegments().get(commonWord.getSegments().size() - 1);
				firstVarSegment.shiftBy(lastCommonSegment.endTime() - firstVarSegment.startTime(), true);
			}
		}
	}
	
	public void reorderOptions(int wordIndex, final String newBestFollower) {
		WordIU word = getWords().get(wordIndex - 1);
		word.setAsTopNextSameLevelLink(newBestFollower);
	}
	
	@Override
	public AudioInputStream getAudio() {
        Voice voice = Voice.getVoice(MaryAdapter4internal.DEFAULT_VOICE);
        HMMData hmmData =((HMMVoice) voice).getHMMData();
        boolean immediateReturn = true;
		VocodingAudioStream vas = new VocodingAudioStream(getFullPStream(), hmmData, immediateReturn);
        float sampleRate = 16000.0F;  //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;  //8,16
        int channels = 1;     //1,2
        boolean signed = true;    //true,false
        boolean bigEndian = false;  //true,false
        AudioFormat af = new AudioFormat(
              sampleRate,
              sampleSizeInBits,
              channels,
              signed,
              bigEndian);
        return new DDSAudioInputStream(vas, af);
	}
	
	public FullPStream getFullPStream() {
		return new IUBasedFullPStream(getWords().get(0));
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