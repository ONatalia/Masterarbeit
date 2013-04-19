package inpro.incremental.processor;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.SysSegmentIU;

import java.util.List;

public class AdaptableSynthesisModule extends SynthesisModule {
	
	/** use default dispatcher for Herwin's convenience */
	public AdaptableSynthesisModule() {
		super(SimpleMonitor.setupDispatcher());
	}
	
	public AdaptableSynthesisModule(DispatchStream ds) {
        super(ds);
    }
	
	/** stop the ongoing (uncommitted) utterance after the ongoing word */
	public void stopAfterOngoingWord() {
		currentInstallment.stopAfterOngoingWord();
	}
	
	public void stopAfterOngoingPhoneme() {
		for (SysSegmentIU seg : currentInstallment.getSegments()) {
			seg.setSameLevelLink(null);
			seg.removeAllNextSameLevelLinks();
		}
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
