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
	
	private Attribute timeToEOT;
	
	protected Instances instances;
	
	double timeIntoTurnValue;
	int wordsIntoTurnValue;
	double timeIntoLastWordValue;
	double currentFrameEnergyValue;
	
	public EOTFeatureAggregator() {
		timeIntoTurnAttribute = new Attribute("timeIntoTurn");
		wordsIntoTurnAttribute = new Attribute("wordsIntoTurn");
		timeIntoLastWordAttribute = new Attribute("timeIntoLastWord");
		currentFrameEnergyAttribute = new Attribute("currentFrameEnergy");
		timeToEOT = new Attribute("timeToEOT");
		FastVector attInfo = new FastVector(NUMBER_OF_ATTRIBUTES);
		attInfo.addElement(timeIntoTurnAttribute);
		attInfo.addElement(wordsIntoTurnAttribute);
		attInfo.addElement(timeIntoLastWordAttribute);
		attInfo.addElement(currentFrameEnergyAttribute);
		attInfo.addElement(timeToEOT);
		instances = new Instances("eotFeatures", attInfo, 0);
		instances.setClass(timeToEOT);
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
