/* 
 * Copyright 2007, 2008, 2009, 2010 Timo Baumann and the Inpro project
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
package inpro.pitch;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import ddf.minim.effects.RevisedBCurveFilter;

import edu.cmu.sphinx.frontend.DoubleData;

@SuppressWarnings("serial")
public class PitchedDoubleData extends DoubleData {

	/** whether voicing information is already available */
	boolean deferred;
	/** whether the frame was classified as voiced */
	boolean voiced;
	/** more informative voicing strength on a scale from 0 to 1 */
	double voicing;
	/** the pitch in Hz for this frame */
	double pitchHz;
	/** the signal power */
	Double power; // only computed (and then cached) on demand
	Double rbcFilteredPower; // only computed (and then cached) on demand
	private RevisedBCurveFilter rbcFilter; // only initialized on demand
/** all the pitch candidates for this frame, i.e. peaks in the autocorrelation function */ 
	List<PitchCandidate> candidates;
	
	/** use this if pitch calculation has not yet completed (add results later with setPitch() */
	public PitchedDoubleData(DoubleData data) {
		this(data, false, 0.0, 0.0, null);
		deferred = true;
	}
	
	/** use this if calculation has already finished and all values can be set */
	public PitchedDoubleData(DoubleData data, boolean voiced, double voicing, double pitch, List<PitchCandidate> candidates) {
		super(data.getValues(), data.getSampleRate(), data.getFirstSampleNumber());
		this.voiced = voiced;
		this.voicing = voicing;
		this.pitchHz = pitch;
		this.candidates = candidates;
	}
	
	/** set pitch-relevant properties after construction */
	void setPitch(List<PitchCandidate> candidates, double pitchHz, boolean voiced, double voicing) {
		this.candidates = candidates;
		this.voiced = voiced;
		this.voicing = voicing;
		this.pitchHz = pitchHz;
		deferred = false;
	}
	
	public void setVoiced(boolean v) {
		this.voiced = v;
	}
	
	public boolean isVoiced() {
		return voiced;
	}
	
	public double getVoicing() {
		return voicing;
	}
	
	public void setPitchHz(double hz) {
		this.pitchHz = hz;
	}
	
	public double getPitchHz() {
		return pitchHz;
	}
	
	public double getPitchCentTo110Hz() {
		return PitchUtils.hzToCent(this.pitchHz);
	}
	
	/** calculate signal power (averaged per sample) */
    public static double signalPower(double[] samples) {
        assert samples.length > 0;
        double sumOfSquares = 0.0f;
        for (int i = 0; i < samples.length; i++) {
            double sample = samples[i];
            sumOfSquares += sample * sample;
        }
        return  Math.sqrt(sumOfSquares/samples.length);
    }

	public double getPower() {
		if (power == null)
			power = Math.max(0, signalPower(getValues()));
		return power;
	}
	
	public double getRbcFilteredPower() {
		double[] samples = getValues();
		assert samples.length <= 160 : "rbc filtering only works with <= 160 samples";
		if (rbcFilteredPower == null) {
			double[] samplesF = Arrays.copyOf(samples, samples.length);
			if (rbcFilter == null) 
				rbcFilter = new RevisedBCurveFilter();
	    	rbcFilter.process(samplesF);
	    	rbcFilteredPower = Math.max(0, signalPower(samplesF));
		}
		return rbcFilteredPower;
	}
	
	public List<PitchCandidate> getCandidates() {
		return Collections.unmodifiableList(candidates);
	}
	
	public String toString() {
		return "PitchedDoubleData: " + 
			   (voiced ? ("voiced, pitch in Hz: " + pitchHz + ", in cent relative to 110Hz: " + getPitchCentTo110Hz()) : "voiceless" ) + 
			   ", mean power: " + power + ", data cointained: " + super.toString();
	}
	
}
