package org.cocolab.inpro.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.cocolab.inpro.pitch.notifier.SignalFeatureListener;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class PitchAndPowerDumper implements SignalFeatureListener {

	int counter = 0;
	LinkedList<Double> powList = new LinkedList<Double>();
	LinkedList<Double> rbcList = new LinkedList<Double>();
	
	public PitchAndPowerDumper() {
		for (int i = 0; i < 10; i++) {
			powList.addFirst(Double.valueOf(0.0d));
			rbcList.addFirst(Double.valueOf(0.0d));
		}
	}
	
	public void newSignalFeatures(double logEnergy, boolean voicing, double pitch) {
		if (voicing) {
			System.out.printf(Locale.US, "%.2f", pitch);
		} else {
			System.out.print("NaN");
		}
		System.out.printf(Locale.US, "\t%.2f\n", logEnergy);
	}

	private static double percentileFilter(List<Double> list) {
		List<Double> sortedList = new ArrayList<Double>(list);
		Collections.sort(sortedList);
		return sortedList.get(8).doubleValue();
	}
	
	@Override
	public void newSignalFeatures(double powEnergy, double rbcEnergy,
			boolean voicing, double pitch) {
		System.out.print(counter++ + "\t");
		if (voicing) {
			System.out.printf(Locale.US, "%.2f", pitch);
		} else {
			System.out.print("NaN");
		}
		powList.removeLast();
		rbcList.removeLast();
		powList.addFirst(Double.valueOf(powEnergy));
		rbcList.addFirst(Double.valueOf(rbcEnergy));
		double smoothedPowEnergy = percentileFilter(powList); 
		double smoothedRbcEnergy = percentileFilter(rbcList);
		System.out.printf(Locale.US, "\t%.2f\t%.2f\t%.2f\t%.2f\n", powEnergy, rbcEnergy, smoothedPowEnergy, smoothedRbcEnergy);
	}

	public void reset() { }
	public void newProperties(PropertySheet ps) throws PropertyException {	}

}
