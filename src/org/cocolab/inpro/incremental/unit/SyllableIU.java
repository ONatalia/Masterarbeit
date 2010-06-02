package org.cocolab.inpro.incremental.unit;

import java.util.List;


public class SyllableIU extends IU {

	public SyllableIU(SyllableIU sll, List<IU> segments) {
		super(sll, segments);
		for (IU iu : segments) {
			assert iu instanceof SegmentIU : "Only segments may ground syllables";
		}
	}

	@Override
	public String toPayLoad() {
		return null;
	}

	// nothing to see here (yet), move on
	
}
