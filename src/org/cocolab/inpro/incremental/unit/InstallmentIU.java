package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

/**
 * a simple wrapper for both system and user uttered installments
 * @author timo
 *
 */
public class InstallmentIU extends IU {

	String tts;
	
	boolean systemProduced; // true: system utterance, false: user utterance
	
	public InstallmentIU(List<WordIU> currentInstallment) {
		super(currentInstallment);
		this.systemProduced = false;
	}
	
	public InstallmentIU(DialogueActIU dialogueAct, String tts) {
		super(Collections.<IU>singletonList(dialogueAct));
		this.systemProduced = true;
		this.tts = tts;
	}
	
	public boolean systemProduced() {
		return systemProduced;
	}
	
	public boolean userProduced() {
		return !systemProduced;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toPayLoad() {
		if (systemProduced) {
			return tts;
		} else { // user produced
			List<WordIU> words = (List<WordIU>) groundedIn();
			StringBuilder text = new StringBuilder(WordIU.wordsToString(words));
			if (!words.isEmpty()) { 
				WordIU lastWord = words.get(words.size() - 1);
				while (lastWord != null && lastWord.isSilence) {
					lastWord = (WordIU) lastWord.sameLevelLink;
				}
				if (lastWord != null && lastWord.hasProsody()) {
					text.append(lastWord.pitchIsRising() ? "+" : "-");
				} else {
					System.err.println("no prosody in " + lastWord);
				}
			}
			return text.toString();
		}
	}

}
