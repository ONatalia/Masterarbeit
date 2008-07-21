package org.cocolab.inpro.pitch;

import org.cocolab.inpro.pitch.util.PitchUtils;

public class PitchCandidate {
	
	public int lag = 0;
	public double score = 0;
	public int frame = 0;
	private int samplingFrequency = 0;
	
	public PitchCandidate(int lag, double score, int samplingFrequency) {
		this.lag = lag;
		this.score = score;
		this.samplingFrequency = samplingFrequency;
	}
	
	public PitchCandidate() {}
	
	public double pitchInCent() {
		double d = PitchUtils.hzToCent(((double) samplingFrequency) / lag);
		if (Math.abs(d - 110) < 0.001) {
			System.err.println(110);
		}
		return d;
	}
	
}
