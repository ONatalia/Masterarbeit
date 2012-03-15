package org.cocolab.inpro.domains.calendar;

import org.cocolab.inpro.incremental.unit.IU;

public class PhraseIU extends IU {

	final String phrase;
	final PhraseType type;
	
	public enum PhraseType {
	    INITIAL, CONTINUATION, REPAIR 
	}
	
	public PhraseIU(String phrase, PhraseType type) {
		this.phrase = phrase;
		this.type = type;
	}
	
	@Override
	public String toPayLoad() {
		return phrase;
	}

}
