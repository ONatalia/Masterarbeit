package org.cocolab.inpro.domains.calendar;

import java.util.ArrayList;
import java.util.ListIterator;

import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;

public class PhraseBasedInstallmentIU extends IncrSysInstallmentIU {

	public PhraseBasedInstallmentIU(PhraseIU phrase) {
		// todo: think about a new method in PhraseIU that gets us more specifically tuned text for synthesis depending on phraseTypes
		super(phrase.toPayLoad());
		phrase.groundIn(new ArrayList<IU>(groundedIn));
		phrase.containingInstallment = this;
	}

	/** breaks the segment links between words so that synthesis stops after the currently ongoing word */
	public void stopAfterOngoingWord() {
		ListIterator<IU> groundIt = groundedIn.listIterator(groundedIn.size());
		for (; groundIt.hasPrevious(); ) {
			WordIU word = (WordIU) groundIt.previous();
			// break the segmentIU layer
			SegmentIU seg = word.getLastSegment();
			seg.removeAllNextSameLevelLinks();
			if (seg.isCompleted()) 
				break; // no need to go on, as this the past already
		}
	}
}
