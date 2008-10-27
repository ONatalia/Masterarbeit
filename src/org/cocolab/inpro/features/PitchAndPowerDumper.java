package org.cocolab.inpro.features;

import java.util.Locale;

import org.cocolab.inpro.sphinx.instrumentation.SignalFeatureListener;

public class PitchAndPowerDumper implements SignalFeatureListener {

	public void newSignalFeatures(double logEnergy, boolean voicing, double pitch) {
		if (voicing) {
			System.out.printf(Locale.US, "%.2f", pitch);
		}
		System.out.printf(Locale.US, "\t%.2f\n", logEnergy);
	}

	public void reset() { }

}
