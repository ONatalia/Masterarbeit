package org.cocolab.inpro.features;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import edu.cmu.sphinx.util.props.Resetable;

public class EOTFeatureAggregator implements Resetable {
	
	private final static int NUMBER_OF_ATTRIBUTES = 5; 
	
	private Attribute timeIntoTurnAttribute; 
	private Attribute wordsIntoTurnAttribute;
	private Attribute timeIntoLastWordAttribute;
	private Attribute currentFrameEnergyAttribute;
	
	protected final static boolean CLUSTERED_TIME = true; // FIXME: this should be configurable (maybe via constructor) 
	
	private Attribute timeToEOT;
	private Attribute clusteredTimeToEOT;
	
	protected Instances instances;
	
	double timeIntoTurnValue;
	int wordsIntoTurnValue;
	double timeIntoLastWordValue;
	double currentFrameEnergyValue;
	
	
	public EOTFeatureAggregator() {
		FastVector attInfo = new FastVector(NUMBER_OF_ATTRIBUTES);
		timeIntoTurnAttribute = new Attribute("timeIntoTurn");
		attInfo.addElement(timeIntoTurnAttribute);
		wordsIntoTurnAttribute = new Attribute("wordsIntoTurn");
		attInfo.addElement(wordsIntoTurnAttribute);
		timeIntoLastWordAttribute = new Attribute("timeIntoLastWord");
		attInfo.addElement(timeIntoLastWordAttribute);
		currentFrameEnergyAttribute = new Attribute("currentFrameEnergy");
		attInfo.addElement(currentFrameEnergyAttribute);

		
		if (CLUSTERED_TIME) {
			clusteredTimeToEOT = EOTBins.eotBinsAttribute();
			attInfo.addElement(clusteredTimeToEOT);
		}
		else {
			timeToEOT = new Attribute("timeToEOT");
			attInfo.addElement(timeToEOT);
		}
		instances = new Instances("eotFeatures", attInfo, 0);
		if (CLUSTERED_TIME) {
			instances.setClass(clusteredTimeToEOT);
		}
		else {
			instances.setClass(timeToEOT);			
		}
		reset();
	}
	
	protected Instance getNewestFeatures() {
		Instance instance = new Instance(NUMBER_OF_ATTRIBUTES);
		instance.setValue(timeIntoTurnAttribute, timeIntoTurnValue);
		instance.setValue(wordsIntoTurnAttribute, wordsIntoTurnValue);
		instance.setValue(timeIntoLastWordAttribute, timeIntoLastWordValue);
		instance.setValue(currentFrameEnergyAttribute, currentFrameEnergyValue);
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
	
	public void setWordsIntoTurn(int wordsIntoTurn) {
		wordsIntoTurnValue = wordsIntoTurn;
	}
	
	public void setTimeIntoLastWord(double timeIntoLastWord) {
		timeIntoLastWordValue = timeIntoLastWord;
	}
	
	public void setCurrentFrameEnergy(double logEnergy) {
		currentFrameEnergyValue = logEnergy;
	}

	public void reset() {
		timeIntoTurnValue = -1.0;
		wordsIntoTurnValue = -1;
		timeIntoLastWordValue = -1.0;
		currentFrameEnergyValue = -1.0;
	}
}
