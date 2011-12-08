package org.cocolab.inpro.tts.hts;

public class FullPFeatureFrame {

    private final double[] mcepParVec;
    private final double[] magParVec;
    private final double[] strParVec;
    private final boolean voiced;
    private final double lf0Par;
    
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

}
