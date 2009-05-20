/*
 * Copyright 2007-2008 Timo Baumann and the Inpro Project 
 * released under the terms of the GNU general public license
 */
package org.cocolab.inpro.pitch;

import java.util.List;

import org.cocolab.inpro.pitch.util.PitchUtils;

import edu.cmu.sphinx.frontend.DoubleData;

@SuppressWarnings("serial")
public class PitchedDoubleData extends DoubleData {

	boolean voiced;
	double pitchHz;
	double power;
	List<PitchCandidate> candidates;

	public PitchedDoubleData(double[] values, int sampleRate, long collectTime, long firstSampleNumber) {
		super(values, sampleRate, collectTime, firstSampleNumber);
	}
	
	public PitchedDoubleData(double[] values, int sampleRate, 
							 long collectTime, long firstSampleNumber, 
							 boolean voiced, double pitch, double power) {
		super(values, sampleRate, collectTime, firstSampleNumber);
		this.voiced = voiced;
		this.pitchHz = pitch;
		this.power = power;
	}
	
	public PitchedDoubleData(DoubleData data, boolean voiced, double pitch, List<PitchCandidate> candidates, double power) {
		super(data.getValues(), data.getSampleRate(), data.getCollectTime(), data.getFirstSampleNumber());
		this.voiced = voiced;
		this.pitchHz = pitch;
		this.power = power;
		this.candidates = candidates;
	}
	
	public void setPitchInCent(double cent, boolean voiced) {
		this.voiced = voiced;
		this.pitchHz = PitchUtils.centToHz(cent);
	}
	
	public boolean isVoiced() {
		return voiced;
	}
	
	public double getPitchHz() {
		return pitchHz;
	}
	
	public double getPitchCentTo110Hz() {
		return PitchUtils.hzToCent(this.pitchHz);
	}
	
	public double getEnergy() {
		return power;
	}
	
	public List<PitchCandidate> getCandidates() {
		return candidates;
	}
	
	public String toString() {
		return "PitchedDoubleData: " + 
			   (voiced ? ("voiced, pitch in Hz: " + pitchHz + ", in cent relative to 110Hz: " + getPitchCentTo110Hz()) : "voiceless" ) + 
			   ", mean power: " + power + ", data cointained: " + super.toString();
	}
	
}
