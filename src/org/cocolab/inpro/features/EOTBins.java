package org.cocolab.inpro.features;

import weka.core.Attribute;
import weka.core.FastVector;

/**
 * class for binning time to turn-relevant units 
 * @author timo
 *
 * 
 * the turn-relevant units can be defined relative to start-of-turn (SOT)
 * and end-of-turn (EOT).
 *
 * for backwards compatibility, units can be measured in distance to eot alone
 * 
 * 
 */

public class EOTBins {
	
	public final static String[] SOT_BINS = {
		"sil"
	};
	
	private final static double[] SOT_BIN_ENDS = {
		0.0
	};
	
	public final static String[] EOT_BINS = {
		"talk", "EOT", "sil"
	};
	
	private final static double[] EOT_BIN_ENDS = {
		0.51, -0.050, -Double.MAX_VALUE
	};
	
	/*  
	 * return a weka attribute with the values for eot-bins
	 */ 
	public static Attribute eotBinsAttribute() {
		FastVector binnedTimesToEOT = new FastVector(EOT_BINS.length);
		for (int i = 0; i < EOT_BINS.length; i++) {
			binnedTimesToEOT.addElement(EOT_BINS[i]);
		}
		Attribute a = new Attribute("binnedTimeToEOT", binnedTimesToEOT);
		return a;
	}
	
	/*
	 * return a weka attribute with values for all turn-bins (EOT_BINS u SOT_BINS)
	 */
	public static Attribute turnBinsAttribute() {
		FastVector turnBins = new FastVector(SOT_BINS.length + EOT_BINS.length);
		for (int i = 0; i < SOT_BINS.length; i++) {
			turnBins.addElement(SOT_BINS[i]);
		}
		for (int i = 0; i < EOT_BINS.length; i++) {
			if (!turnBins.contains(EOT_BINS[i])) {
				turnBins.addElement(EOT_BINS[i]);
			}
		}
		Attribute a = new Attribute("turnBins", turnBins);
		return a;		
	}
	
	/**
	 * finds the right bin for the given distance to EOT
	 * @return bin-name
	 */
	public static String eotBin(double eot) {
		for (int i = 0; i < EOT_BINS.length; i++) {
			if (eot > EOT_BIN_ENDS[i]) {
				return EOT_BINS[i];
			}
		}
		// this should never be reached
		assert false;
		return null;
	}
	
	/**
	 * finds the right bin for the given distances to SOT and EOT
	 * @param sot distance to start of turn
	 * @param eot dist. to eot
	 * @return bin-name
	 */
	public static String turnState(double sot, double eot) {
		// check if we are in the beginning of the turn and return the corresponding bin
		for (int i = 0; i < SOT_BINS.length; i++) {
			if (sot < SOT_BIN_ENDS[i]) {
				return SOT_BINS[i];
			}
		}
		// otherwise return bin relative to eot
		return eotBin(eot);
	}
	
	public static void main(String[] args) {
		System.out.println("Testing class EOTBins");
		for (double d = 2.6; d > -0.5; d -= 0.02) {
			System.out.println(d + " is " + eotBin(d));
		}
	}
	
}



/*	public final static String[] EOT_BINS = {
		"moreThan2560ms", "2560to1281ms", "1280to641ms", "640to321ms", "320to161ms",
		"160to81ms", "80to41ms", "40to1ms", "0to39msAgo", "40to79msAgo", "80to159msAgo",
		"moreThan160msAgo"
	};
	
	private final static double[] EOT_BIN_ENDS = {
		2.561, 1.281, 0.641, 0.321, 0.161, 0.081, 0.041, 0.001, -0.039, -0.079, -0.159, -Double.MAX_VALUE
	};
	public final static String[] EOT_BINS = {
		"moreThan640ms", "640to161ms", "161to81ms", "80to1ms", "0to38msAgo", "40to119msAgo", "moreThan120msAgo" 
	};
	
	private final static double[] EOT_BIN_ENDS = {0.641, 0.161, 0.081, 0.001, -0.039, -0.119, -Double.MAX_VALUE}; 

	public final static String[] EOT_BINS = {
		"moreThan640ms", "640to161ms", "161to81ms", "80to1ms", "0to38msAgo", "40to119msAgo", "moreThan120msAgo" 
	};
	
	private final static double[] EOT_BIN_ENDS = {0.641, 0.161, 0.081, 0.001, -0.039, -0.119, -Double.MAX_VALUE}; 
	
	public final static String[] EOT_BINS = {
		"notYet", "now"
	};
	
	private final static double[] EOT_BIN_ENDS = {
		0.101, -Double.MAX_VALUE
	};
*/ 