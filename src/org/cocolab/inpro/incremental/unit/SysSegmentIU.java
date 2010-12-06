package org.cocolab.inpro.incremental.unit;

import java.util.List;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.tts.PitchMark;

public class SysSegmentIU extends SegmentIU {

	Label plannedLabel; // -> alternatively store "realizedLabel"
	List<PitchMark> pitchMarks;
	
	public SysSegmentIU(Label l, List<PitchMark> pitchMarks) {
		super(l);
		plannedLabel = new Label(l);
		this.pitchMarks = pitchMarks;
	}
	
	public void scale(double factor, double base) {
		
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
	
}
