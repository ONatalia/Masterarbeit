package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

/**
 * a simple wrapper for both system and user uttered installments
 * - for user installments references to recognized WordIUs are kept 
 *   used to infer the spoken text in toPayload()
 * - for system installments a reference to the corresponding dialogueAct
 *   is kept and the spoken text is stored in the variable @see{tts}.
 * @author timo
 *
 */
public class InstallmentIU extends IU {

	final String tts;
	
	boolean systemProduced; // true: system utterance, false: user utterance
	
	public InstallmentIU(List<WordIU> currentInstallment) {
		super(currentInstallment);
		this.systemProduced = false;
		this.tts = null;
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
