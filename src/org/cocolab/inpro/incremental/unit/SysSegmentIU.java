package org.cocolab.inpro.incremental.unit;

import java.util.List;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.tts.PitchMark;

public class SysSegmentIU extends SegmentIU {

	Label plannedLabel; // -> alternatively store "realizedLabel"
	List<PitchMark> pitchMarks;
	
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
	
}
