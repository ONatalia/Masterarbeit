package test.pitch;

import edu.cmu.sphinx.frontend.DoubleData;

public class PitchedDoubleData extends DoubleData {

	boolean voiced;
	double pitch;
	
	public PitchedDoubleData(double[] values, int sampleRate, long collectTime, long firstSampleNumber) {
		super(values, sampleRate, collectTime, firstSampleNumber);
	}
	
	public PitchedDoubleData(double[] values, int sampleRate, 
							 long collectTime, long firstSampleNumber, 
							 boolean voiced, double pitch) {
		super(values, sampleRate, collectTime, firstSampleNumber);
		this.voiced = voiced;
		this.pitch = pitch;
	}
	
	public PitchedDoubleData(DoubleData data, boolean voiced, double pitch) {
		super(data.getValues(), data.getSampleRate(), data.getCollectTime(), data.getFirstSampleNumber());
		this.voiced = voiced;
		this.pitch = pitch;
	}
	
	boolean isVoiced() {
		return voiced;
	}
	
	double getPitch() {
		return pitch;
	}

	public String toString() {
		return "PitchedDoubleData: " + 
			   (voiced ? ("voiced, pitch: " + pitch ) : "voiceless" ) + 
			   ", data cointained: " + super.toString();
	}
	
}
