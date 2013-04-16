package inpro.incremental.processor;

import inpro.incremental.unit.SysSegmentIU;

import java.util.List;

public class AdaptableSynthesisModule extends SynthesisModule {
	
	public void stopAfterOngoingWord() {
		currentInstallment.stopAfterOngoingWord();
	}
	
	/**
	 * @param s absolute scaling, that is, applying s=2 multiple times does not result in multiple changes
	 */
	public void scaleTempo(double s) {
		List<SysSegmentIU> segs = currentInstallment.getSegments();
		for (SysSegmentIU seg: segs) {
			if (!seg.isCompleted()) {
				seg.stretchFromOriginal(s);
			}
		}
	}
	
	/**
	 * @param pitchShiftInCent cent is 1/100 of a halftone. 1200 cent is an octave
	 */
	public void shiftPitch(int pitchShiftInCent) {
		List<SysSegmentIU> segs = currentInstallment.getSegments();
		for (SysSegmentIU seg: segs) {
			if (!seg.isCompleted()) {
				seg.pitchShiftInCent = pitchShiftInCent;
			}
		}
	}
	
}
