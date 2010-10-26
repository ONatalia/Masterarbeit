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

	@SuppressWarnings("unchecked")
	@Override
	public String toPayLoad() {
		if (systemProduced) {
			return tts;
		} else {
			String words = WordIU.wordsToString((List<WordIU>) groundedIn());
			return words;
		}
	}

}
