package org.cocolab.inpro.tts.hts;

import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSParameterGeneration;

public class HTSFullPStream extends FullPStream {
    private HTSPStream mcepPst;
    private HTSPStream strPst;
    private HTSPStream magPst;
    private HTSPStream lf0Pst;
    private int[] lf0indices; 
    private boolean[] voiced;
    
    public HTSFullPStream(HTSParameterGeneration htspg) {
    	this(htspg.getMcepPst(), htspg.getStrPst(), htspg.getMagPst(), htspg.getlf0Pst(), htspg.getVoicedArray());
    }
    
    public HTSFullPStream(HTSPStream mcepPst, HTSPStream strPst, HTSPStream magPst, HTSPStream lf0Pst, boolean[] voiced) {
        this.mcepPst = mcepPst;
        this.strPst = strPst;
        this.magPst = magPst != null ? magPst : new EmptyHTSPStream();
        this.lf0Pst = lf0Pst;
        this.voiced = voiced;
        lf0indices = new int[voiced.length];
        for (int t = 0, vi = 0; t < voiced.length; t++) {
            lf0indices[t] = vi;
            if (voiced[t] && vi < lf0Pst.getT() - 1)
                vi++;
        }
    }
    
    @Override
	public FullPFeatureFrame getFullFrame(int t) {
    	//assert t < getMaxT();
    	if (t < getMaxT()) {
    		return new FullPFeatureFrame(mcepPst.getParVec(t), magPst.getParVec(t), strPst.getParVec(t), voiced[t], lf0Pst.getPar(lf0indices[t], 0));
    	} else return null;
    }
    
    @Override
	public int getMcepParSize() {
        return mcepPst.getOrder();
    }

    @Override
	public int getStrParSize() {
        return strPst.getOrder();
    }

//    @Override
//	public double[] getMcepParVec(int t) {
//        return mcepPst.getParVec(t);
//    }
//
//    @Override
//	public double[] getMagParVec(int t) {
//        return magPst.getParVec(t);
//    }
//
//    @Override
//	public double[] getStrParVec(int t) {
//        return strPst.getParVec(t);
//    }
//
//    @Override
//	public double getlf0Par(int t) {
//        return lf0Pst.getPar(lf0indices[t], 0); 
//    }
//
//    @Override
//	public boolean isVoiced(int t) {
//        return voiced[t];
//    }

    @Override
	public int getMaxT() {
        return mcepPst.getT();
    }
    
    class EmptyHTSPStream extends HTSPStream {

    	private double[] emptyParVec = new double[] {};
    	
		public EmptyHTSPStream() {
			super(0, 0, 0, 0);
		}
		
		@Override
		public double[] getParVec(int t) {
			return emptyParVec;
		}
    	
    }

}
