package org.cocolab.inpro.incremental.basedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.BaseDataKeeper;
import org.cocolab.inpro.pitch.PitchTracker;
import org.cocolab.inpro.pitch.PitchedDoubleData;

import ddf.minim.effects.RevisedBCurveFilter;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class BaseData implements Configurable, BaseDataKeeper, Resetable {

	final static Logger logger = Logger.getLogger(BaseData.class);
	
	public static final String PITCHED_DATA = "pitchedData";
	public static final String MEL_DATA = "melData";
	
	ConcurrentSkipListSet<TimedData<PitchedDoubleData>> pitchedData = new ConcurrentSkipListSet<TimedData<PitchedDoubleData>>(new TimedDataComparator());
	ConcurrentSkipListSet<TimedData<DoubleData>> melData = new ConcurrentSkipListSet<TimedData<DoubleData>>(new TimedDataComparator());
	private ConcurrentSkipListSet<TimedData<Double>> loudnessData = new ConcurrentSkipListSet<TimedData<Double>>(new TimedDataComparator());

	private RevisedBCurveFilter rbcFilter = new RevisedBCurveFilter();

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException { }

	public void addData(Data d, String type) {
		if (PITCHED_DATA.equals(type)) {
			if (d instanceof PitchedDoubleData) { // the first few frames may be DoubleData
				PitchedDoubleData pdd = (PitchedDoubleData) d;
				int frame = (int) pdd.getFirstSampleNumber() / 160; // frame since start of audio (regardless of VAD)
				pitchedData.add(new TimedData<PitchedDoubleData>(frame, pdd));
			}
		} else if (MEL_DATA.equals(type)) {
			assert (d instanceof DoubleData) : d;
			DoubleData dd = (DoubleData) d;
			int frame = (int) dd.getFirstSampleNumber() / 160;
			melData.add(new TimedData<DoubleData>(frame, dd));
		}
	}

	@Override
	public double getPitchInCent(double time) {
		@SuppressWarnings("unchecked")
		TimedData<PitchedDoubleData> td = pitchedData.floor(new TimedData(time));
		if (td != null) {
			PitchedDoubleData pdd = td.value;
			if (pdd.isVoiced()) {
				return pdd.getPitchCentTo110Hz();
			} else {
				return Double.NaN;
			}
		} else {
			logger.warn("no data at time " + time);
			return Double.NaN;
		}
	}
	
	@Override
	public double getVoicing(double time) {
		@SuppressWarnings("unchecked")
		TimedData<PitchedDoubleData> td = pitchedData.floor(new TimedData(time));
		if (td != null) {
			PitchedDoubleData pdd = td.value;
			return pdd.getVoicing();
		}
		return Double.NaN;
	}

    private double revisedBCurveCorrectedRMS(DoubleData d) {
    	double rms = 0.0d;
    	if (d instanceof DoubleData) {
    		double[] samples = d.getValues();
    		assert samples.length <= 160;
    		double[] samplesF = Arrays.copyOf(samples, samples.length); 
	    	rbcFilter.process(samplesF);
	    	rms = PitchTracker.signalPower(samplesF);
    	}
        rms = Math.max(rms, 0);
    	return rms;
    }
    
	private static double percentileFilter(List<Double> list, int windowPosition) {
		List<Double> sortedList = new ArrayList<Double>(list);
		Collections.sort(sortedList);
		return sortedList.get(windowPosition).doubleValue();
	}
	
	private void makeLoudness(int smoothingWindow, int windowPosition) {
		loudnessData.clear();
		LinkedList<Double> rbcList = new LinkedList<Double>();
		for (int i = 0; i < smoothingWindow; i++) {
			rbcList.add(Double.valueOf(0.0d));
		}
		for (TimedData<PitchedDoubleData> data : pitchedData) {
			double rbcEnergy = revisedBCurveCorrectedRMS(data.value);
			rbcList.removeLast();
			rbcList.addFirst(Double.valueOf(rbcEnergy));
			double smoothedRbcEnergy = percentileFilter(rbcList, windowPosition);
			double rbcEnergyInDB = 10 * Math.log10(smoothedRbcEnergy);
			loudnessData.add(new TimedData<Double>(data.frame, new Double(rbcEnergyInDB)));
		}
	}
	
	@Override
	public double getLoudness(double time) {
		if (loudnessData.size() == 0) {
			makeLoudness(1, 0);
		}
		@SuppressWarnings("unchecked")
		TimedData<Double> td = loudnessData.floor(new TimedData(time + 0.01));
		if (td != null) {
			return td.value.doubleValue();
		} else {
			return Double.NaN;
		}
	}
	
	private double specTiltTime;
	private double specTiltSlope;
	private double specTiltRSquared;
	private void computeSpectralTilt(double time) {
		if (time == specTiltTime) return;
		@SuppressWarnings("unchecked")
		TimedData<DoubleData> td = melData.floor(new TimedData(time));
		// let's hope that nobody messed with melData. Then we should find the interesting range 
		// (130-~4000) in the first 32 coefficients. to keep things simple, we just do a linear regression
		
		// this is actually fft now, ranging from 0-8000 Hz with 256 coefficients;
		// range 500-4000 should then be 16-128 (112 values)
		
		if ((td == null) || (td.value.getValues() == null)) {
			specTiltSlope = Double.NaN;
			specTiltRSquared = Double.NaN;
			return;
		}
		double[] y = Arrays.copyOfRange(td.value.getValues(), 16, 116);
		double sumY = 0;
		double sumTY = 0;
		double sumYY = 0;
		double sumT = 0;
		double sumTT = 0;
		final int n = 100;
		for (int i = 0; i < n; i++) {
			sumT += i;
			sumTT += i * i;
			sumY += y[i];
			sumYY += y[i] * y[i];
			sumTY += i * y[i];
		}
		double sumSqDevT = sumTT - sumT * sumT / (double) n;
		double sumSqDevTY = sumTY - sumT * sumY / (double) n;
		double sumSqDevY = sumYY - sumY * sumY / (double) n;
		specTiltSlope = sumSqDevTY / sumSqDevT;
		double denom = sumSqDevT * sumSqDevY;
		specTiltRSquared = (denom != 0.0) ? (sumSqDevTY * sumSqDevTY / denom) : 1.0;
		specTiltTime = time;
	}
	
	public double getSpectralTiltQual(double time) {
		computeSpectralTilt(time);
		return specTiltRSquared;
	}
	
	public double getSpectralTilt(double time) {
		computeSpectralTilt(time);
		return specTiltSlope;
	}
	
	public void reset() {
		pitchedData.clear();
		melData.clear();
		loudnessData.clear();
	}

	private class TimedData<T> {
		int frame;
		T value;
		
		TimedData(double time) {
			this((int) (time * 100));
		}
		
		TimedData(int frame) {
			this.frame = frame;
		}
		
		TimedData(int frame, T data) {
			this.frame = frame;
			this.value = data;
		}

	}
	
	class TimedDataComparator implements Comparator<TimedData<?>> {
		@Override
		public int compare(TimedData<?> o1, TimedData<?> o2) {
			return o1.frame - o2.frame;
		}
	}

}
