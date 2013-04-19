package inpro.incremental.processor;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.hts.VocodingFramePostProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AdaptableSynthesisModule extends SynthesisModule {
	
	VocodingFramePostProcessor framePostProcessor = null;
	
	public AdaptableSynthesisModule() {
		this(SimpleMonitor.setupDispatcher());
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
	
	@Override
	protected synchronized void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		super.leftBufferUpdate(ius, edits);
		for (SysSegmentIU seg : getSegments())
			seg.setVocodingFramePostProcessor(framePostProcessor);
	}
	
	/** return the segments in the ongoing utterance (if any) */ 
	private List<SysSegmentIU> getSegments() {
		if (currentInstallment != null)
			return currentInstallment.getSegments();
		else
			return Collections.<SysSegmentIU>emptyList();
	}

	/**
	 * @param s absolute scaling, that is, applying s=2 multiple times does not result in multiple changes
	 */
	public void scaleTempo(double s) {
		for (SysSegmentIU seg: getSegments()) {
			if (!seg.isCompleted()) {
				seg.stretchFromOriginal(s);
			}
		}
	}
	
	/**
	 * @param pitchShiftInCent cent is 1/100 of a halftone. 1200 cent is an octave
	 */
	public void shiftPitch(int pitchShiftInCent) {
		for (SysSegmentIU seg: getSegments()) {
			if (!seg.isCompleted()) {
				seg.pitchShiftInCent = pitchShiftInCent;
			}
		}
	}
	
	public void setFramePostProcessor(VocodingFramePostProcessor postProcessor) {
		this.framePostProcessor = postProcessor;
	}
	
}
