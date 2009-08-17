package org.cocolab.inpro.incremental.basedata;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

import org.cocolab.inpro.incremental.BaseDataKeeper;
import org.cocolab.inpro.pitch.PitchedDoubleData;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class BaseData implements Configurable, BaseDataKeeper, Resetable {

	public static final String PITCHED_DATA = "pitchedData";
	public static final String MEL_DATA = "melData";
	
	ConcurrentSkipListSet<TimedData<PitchedDoubleData>> pitchedData = new ConcurrentSkipListSet<TimedData<PitchedDoubleData>>(new TimedDataComparator());
	ConcurrentSkipListSet<TimedData<DoubleData>> melData = new ConcurrentSkipListSet<TimedData<DoubleData>>(new TimedDataComparator());;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException { 
		return;
		
	}

	public void addData(Data d, String type) {
		if (PITCHED_DATA.equals(type)) {
			if (d instanceof PitchedDoubleData) { // the first few frames may be DoubleData
				PitchedDoubleData pdd = (PitchedDoubleData) d;
				int frame = (int) pdd.getFirstSampleNumber() / 160; // frame since start of audio (regardless of VAD)
				pitchedData.add(new TimedData<PitchedDoubleData>(frame, pdd));
			}
		} else if (MEL_DATA.equals(type)) {
			assert (d instanceof DoubleData);
			DoubleData dd = (DoubleData) d;
			int frame = (int) dd.getFirstSampleNumber() / 160;
			melData.add(new TimedData<DoubleData>(frame, dd));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public double getPitchInCent(double time) {
		int frame = (int) (time * 100);
		TimedData<PitchedDoubleData> td = pitchedData.floor(new TimedData(frame));
		if (td != null) {
			PitchedDoubleData pdd = td.data;
			if (pdd.isVoiced()) {
				return pdd.getPitchCentTo110Hz();
			} else {
				return Double.NaN;
			}
		} else {
			System.err.println("no data for frame " + frame);
			return Double.NaN;
		}
	}

	public void reset() {
		pitchedData.clear();
		melData.clear();
	}

	class TimedData<T> {
		int frame;
		T data;
		
		TimedData(int frame) {
			this.frame = frame;
		}
		
		TimedData(int frame, T data) {
			this.frame = frame;
			this.data = data;
		}

	}
	
	class TimedDataComparator implements Comparator<TimedData<?>> {
		@Override
		public int compare(TimedData<?> o1, TimedData<?> o2) {
			return o1.frame - o2.frame;
		}
	}

}
