package org.cocolab.inpro.domains.calendar;

import org.cocolab.inpro.incremental.unit.IU;

public class PhraseIU extends IU {

	final String phrase;
	PhraseType type;
	PhraseStatus status;
	/** the state of delivery that this unit is in */
	Progress progress = Progress.UPCOMING;
	
	public enum PhraseType {
	    INITIAL, CONTINUATION, REPAIR, FINAL, UNDEFINED
	}
	
	public enum PhraseStatus {
	    NORMAL, PROJECTED
	}
	
	public PhraseIU(String phrase, PhraseStatus status) {
		this(phrase, status, PhraseType.UNDEFINED);
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

	protected void setProgress(Progress p) {
		if (p != this.progress) { 
			this.progress = p;
			notifyListeners();
		}
	}
	
	public Progress getProgress() {
		return progress;
	};
}
