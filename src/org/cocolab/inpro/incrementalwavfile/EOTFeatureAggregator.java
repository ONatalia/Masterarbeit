package org.cocolab.inpro.incrementalwavfile;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class EOTFeatureAggregator {
	
	private Attribute timeIntoTurnAttribute;
	private Attribute timeToEOT;
	protected Instances instances;
	
	double timeIntoTurnValue = -1.0;
	
	public EOTFeatureAggregator() {
		timeIntoTurnAttribute = new Attribute("timeIntoTurn");
		timeToEOT = new Attribute("timeToEOT");
		FastVector attInfo = new FastVector(2);
		attInfo.addElement(timeIntoTurnAttribute);
		attInfo.addElement(timeToEOT);
		instances = new Instances("eotFeatures", attInfo, 0);
		instances.setClass(timeToEOT);
	}
	
	protected Instance getNewestFeatures() {
		Instance instance = new Instance(2);
		instance.setValue(timeIntoTurnAttribute, timeIntoTurnValue);
		instance.setDataset(instances);
		instance.setClassMissing();
		return instance;
	}
	
	public void setTimeIntoTurn(double t) {
		timeIntoTurnValue = t;
	}

	public double getTimeIntoTurn() {
		return timeIntoTurnValue;
	}
	
}
