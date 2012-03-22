package org.cocolab.inpro.domains.calendar;

import java.util.List;

import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

public class PhraseIU extends IU {

	final String phrase;
	PhraseType type;
	PhraseStatus status;
	/** the state of delivery that this unit is in */
	Progress progress = Progress.UPCOMING;
	PhraseBasedInstallmentIU containingInstallment;
	
	public enum PhraseType {
	    INITIAL, // the first phrase in the utterance
	    CONTINUATION, // continues the previous phrase 
	    REPAIR, // correction of the previous phrase
	    FINAL, // last phrase of the utterance <-- this is not exclusive with e.g. continuation,initial,repair,
	    	   // unlike the other types this describes the end of the phrase, not the beginning 
	    UNDEFINED // dunno
	}
	
	public enum PhraseStatus {
	    NORMAL, // currently spoken/to be spoken next
	    PROJECTED // the next phrase, just as context for the currently ongoing phrase 
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
	
	/** the number of white-space delimited tokens in the payload */
	public int expectedWordCount() {
		String phrase = toPayLoad();
		return phrase.split("\\s+").length;
	}
	
	/** grounds in the list of wordIUs, which must have the expected number of elements */
	@Override
	public void groundIn(List<IU> ius) {
		super.groundIn(ius);
		// we don't ever look at the groundedIn's, so we don't need to care about this failing assertion
		//assert (((WordIU) ius.get(0)).isSilence()) ? ius.size() == expectedWordCount() + 1 : ius.size() == expectedWordCount();
	}

	protected void setProgress(Progress p) {
		if (p != this.progress) { 
			this.progress = p;
			notifyListeners();
		}
	}
	
	public Progress getProgress() {
		return progress;
	}
}
