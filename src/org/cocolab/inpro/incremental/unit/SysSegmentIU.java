package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.tts.PitchMark;
import org.cocolab.inpro.tts.hts.FullPFeatureFrame;
import org.cocolab.inpro.tts.hts.FullPStream;

public class SysSegmentIU extends SegmentIU {

	Label plannedLabel; // -> alternatively store "realizedLabel"
	/** the label that was originally planned by TTS, before any stretching has been done */ 
	Label originalLabel;
	List<PitchMark> pitchMarks;
	List<FullPFeatureFrame> hmmSynthesisFeatures;
	
	public SysSegmentIU(Label l, List<PitchMark> pitchMarks) {
		super(l);
		plannedLabel = new Label(l);
		this.pitchMarks = pitchMarks;
	}
	
	@Override
	public StringBuilder toMbrolaLine() {
		StringBuilder sb = l.toMbrola();
		for (PitchMark pm : pitchMarks) {
			sb.append(" ");
			sb.append(pm.toString());
		}
		sb.append("\n");
		return sb;
	}
	
	@Override
	public void appendMaryXML(StringBuilder sb) {
		sb.append("<ph d='");
		sb.append((int) (l.getDuration() * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("' end='");
		sb.append(l.getEnd());
		sb.append("' f0='");
		for (PitchMark pm : pitchMarks) {
			sb.append(pm.toString());
		}
		sb.append("' p='");
		sb.append(l.getLabel());
		sb.append("'/>\n");
	}
	
	public void setHmmSynthesisFrames(List<FullPFeatureFrame> hmmSynthesisFeatures) {
		this.hmmSynthesisFeatures = hmmSynthesisFeatures;
		assert Math.abs(hmmSynthesisFeatures.size() - duration() * 200) < 0.001 : "" + hmmSynthesisFeatures.size() + " != " + (duration() * 200) + " in " + toString();
	}
	
	/**	the duration of this segment in multiples of 5 ms */
	public int durationInSynFrames() {
		return (int) Math.round(duration() * FullPStream.FRAMES_PER_SECOND);
	}
	
	public double originalDuration() {
		if (originalLabel != null)
			return originalLabel.getDuration();
		else
			return duration();
	}
	
	public List<FullPFeatureFrame> getHMMSynthesisFrames() {
		return Collections.unmodifiableList(hmmSynthesisFeatures);
	}
	
	/** 
	 * stretch the current segment by a given factor.
	 * NOTE: as time is discrete (in 5ms steps), the stretching factor that is actually applied
	 * may differ from the requested factor. 
	 * @param factor larger than 1 to lengthen, smaller than 1 to shorten; must be larger than 0
	 * @return the actual duration that has been applied 
	 */
	public double stretch(double factor) {
		assert factor > 0f;
		double newDuration = duration() * factor;
		return setNewDuration(newDuration);
		
	}
	
	public double stretchFromOriginal(double factor) {
		assert factor > 0f;
		double newDuration = originalDuration() * factor;
		return setNewDuration(newDuration);
	}
	
	/** @return the actual new duration (multiple of 5ms) */
	public double setNewDuration(double d) {
		assert d > 0;
		if (originalLabel == null) 
			originalLabel = this.l;
		Label oldLabel = this.l;
		// change duration to conform to 5ms limit
		double newDuration = Math.round(d * 200.f) / 200.f;
		shiftBy(newDuration - duration(), true);
		// correct this' label to start where it used to start (instead of the shifted version)
		this.l = new Label(oldLabel.getStart(), this.l.getEnd(), this.l.getLabel());
		return newDuration;
	}
	
	/** shift the start and end times of this (and possibly all following SysSegmentIUs */
	public void shiftBy(double offset, boolean recurse) {
		Label l = this.l;
		this.l = new Label(l.getStart() + offset, l.getEnd() + offset, l.getLabel());
		this.plannedLabel = this.l;
		if (recurse) {
			for (IU nSll : getNextSameLevelLinks()) {
				((SysSegmentIU) nSll).shiftBy(offset, recurse);
			}
		}
	}

	/** SysSegmentIUs may have a granularity finer than 10 ms (5ms) */
	@Override
	public String toLabelLine() {
		return String.format(Locale.US,	"%.3f\t%.3f\t%s", startTime(), endTime(), toPayLoad());
	}

}
