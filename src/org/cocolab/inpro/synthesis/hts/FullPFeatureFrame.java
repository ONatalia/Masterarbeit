package org.cocolab.inpro.synthesis.hts;

import java.util.Arrays;

import org.cocolab.inpro.pitch.util.PitchUtils;

public class FullPFeatureFrame {

    private final double[] mcepParVec;
    private final double[] magParVec;
    private final double[] strParVec;
    public final boolean voiced;
    private double lf0Par;
    
    public FullPFeatureFrame(double[] mcep, double[] mag, double[] str, boolean voiced, double lf0Par) {
        mcepParVec = mcep;
        magParVec = mag;
        strParVec = str;
        this.voiced = voiced;
        this.lf0Par = lf0Par;
    }
    
    public double[] getMcepParVec() { return mcepParVec; }
    public double[] getMagParVec() { return magParVec; }
    public double[] getStrParVec() { return strParVec; }

    public int getMcepParSize() { return mcepParVec.length; }
    public int getStrParSize() { return strParVec.length; }

    public boolean isVoiced() { return voiced; }
    public double getlf0Par() { return lf0Par; }
    
    public void shiftlf0Par(double pitchShiftInCent) {
    	if (voiced)
    		lf0Par += pitchShiftInCent * PitchUtils.BY_CENT_CONST;
    }
    
    public void setlf0Par(double lf0) {
    	lf0Par = lf0;
    }
    
    /** f0 in Hz */
    public void setf0Par(double f0) {
    	setlf0Par(Math.log(f0));
    }
    
    @Override
    public String toString() {
    	return "mcep: " + Arrays.toString(mcepParVec) + 
    		 ", mag: " + Arrays.toString(magParVec) + 
    		 ", str: " + Arrays.toString(strParVec) + 
        (voiced ? ", voiced with pitch " + Math.exp(lf0Par) + " Hz" : ", unvoiced");
    }

	public String toPitchString() {
		return (voiced ? ", voiced with pitch " + Math.exp(lf0Par) + " Hz" : ", unvoiced");
	}

}
