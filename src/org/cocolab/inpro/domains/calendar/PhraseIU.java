package org.cocolab.inpro.domains.calendar;

import org.cocolab.inpro.incremental.unit.IU;

public class PhraseIU extends IU {

	final String phrase;
	PhraseType type;
	PhraseStatus status;
	
	public enum PhraseType {
	    INITIAL, CONTINUATION, REPAIR, FINAL, UNDEFINED
	}
	
	public enum PhraseStatus {
	    NORMAL, PROJECTED
	}
	
	public PhraseIU(String phrase, PhraseStatus status) {
		this.phrase = phrase;
		this.status = status;		
		this.type = PhraseType.UNDEFINED;
	}
	
	public PhraseIU(String phrase, PhraseStatus status, PhraseType type) {
		this.phrase = phrase;
		this.status = status;
		this.type = type;
	}
	
	@Override
	public String toPayLoad() {
		return phrase;
	}

}
