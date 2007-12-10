package org.cocolab.inpro.pitch;

import edu.cmu.sphinx.frontend.DoubleData;

public class PitchedDoubleData extends DoubleData {

	private static final double CENT_CONST = 1731.2340490667560888319096172; // 1200 / ln(2)
	
	boolean voiced;
	double pitchHz;
	
	public PitchedDoubleData(double[] values, int sampleRate, long collectTime, long firstSampleNumber) {
		super(values, sampleRate, collectTime, firstSampleNumber);
	}
	
	public PitchedDoubleData(double[] values, int sampleRate, 
							 long collectTime, long firstSampleNumber, 
							 boolean voiced, double pitch) {
		super(values, sampleRate, collectTime, firstSampleNumber);
		this.voiced = voiced;
		this.pitchHz = pitch;
	}
	
	public PitchedDoubleData(DoubleData data, boolean voiced, double pitch) {
		super(data.getValues(), data.getSampleRate(), data.getCollectTime(), data.getFirstSampleNumber());
		this.voiced = voiced;
		this.pitchHz = pitch;
	}
	
	public boolean isVoiced() {
		return voiced;
	}
	
	public double getPitchHz() {
		return pitchHz;
	}
	
	public double getPitchCentTo110Hz() {
		return CENT_CONST * Math.log(this.pitchHz / 110);
	}

	public String toString() {
		return "PitchedDoubleData: " + 
			   (voiced ? ("voiced, pitch in Hz: " + pitchHz + ", in cent relative to 110Hz: " + getPitchCentTo110Hz()) : "voiceless" ) + 
			   ", data cointained: " + super.toString();
	}
	
}
