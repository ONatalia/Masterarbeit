package org.cocolab.inpro.pitch;

public class PitchCandidate {
	public int lag = 0;
	public double score = 0;
	public int frame = 0;
	
	public PitchCandidate(int lag, double score) {
		this.lag = lag;
		this.score = score;
	}
	
	public PitchCandidate() {}
	
}
