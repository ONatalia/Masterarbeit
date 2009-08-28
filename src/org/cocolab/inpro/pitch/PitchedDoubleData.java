/* 
 * Copyright 2007, 2008, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.pitch;

import java.util.List;

import org.cocolab.inpro.pitch.util.PitchUtils;

import edu.cmu.sphinx.frontend.DoubleData;

@SuppressWarnings("serial")
public class PitchedDoubleData extends DoubleData {

	boolean voiced;
	double voicing;
	double pitchHz;
	double power;
	List<PitchCandidate> candidates;

	public PitchedDoubleData(double[] values, int sampleRate, long collectTime, long firstSampleNumber) {
		super(values, sampleRate, collectTime, firstSampleNumber);
	}
	
	public PitchedDoubleData(double[] values, int sampleRate, 
							 long collectTime, long firstSampleNumber, 
							 boolean voiced, double voicing, double pitch, double power) {
		super(values, sampleRate, collectTime, firstSampleNumber);
		this.voiced = voiced;
		this.voicing = voicing;
		this.pitchHz = pitch;
		this.power = power;
	}
	
	public PitchedDoubleData(DoubleData data, boolean voiced, double voicing, double pitch, List<PitchCandidate> candidates, double power) {
		super(data.getValues(), data.getSampleRate(), data.getCollectTime(), data.getFirstSampleNumber());
		this.voiced = voiced;
		this.voicing = voicing;
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
	
	public double getVoicing() {
		return voicing;
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
