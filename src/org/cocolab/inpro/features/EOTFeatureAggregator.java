package org.cocolab.inpro.features;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import edu.cmu.sphinx.util.props.Resetable;

public class EOTFeatureAggregator implements Resetable {
	
	private Attribute framesIntoTurnAttribute;
	private Attribute wordsIntoTurnAttribute;
	private Attribute framesIntoLastWordAttribute;
	private Attribute asrResultsLagAttribute;
	private Attribute currentFrameEnergyAttribute;
	private Attribute frameEnergy50msMeanAttribute;
	private Attribute frameEnergy50msSlopeAttribute;
	private Attribute frameEnergy50msPredictAttribute;
	private Attribute frameEnergy100msMeanAttribute;
	private Attribute frameEnergy100msSlopeAttribute;
	private Attribute frameEnergy100msPredictAttribute;
	private Attribute frameEnergy200msMeanAttribute;
	private Attribute frameEnergy200msSlopeAttribute;
	private Attribute frameEnergy200msPredictAttribute;
	
	protected final static boolean CLUSTERED_TIME = true; // FIXME: this should be configurable (maybe via configurable)
	protected final static boolean CONTINUOUS_TIME = true; // FIXME: this should be configurable (via configurable interface)
	
	protected Attribute timeToEOT;
	protected Attribute clusteredTimeToEOT;
	
	protected Instances instances;
	
	int framesIntoTurnValue;
	int wordsIntoTurnValue;
	int framesIntoLastWordValue;
	int asrResultsLagValue;
	double currentFrameEnergyValue;
	TimeShiftingAnalysis frameEnergy50ms = new TimeShiftingAnalysis(5);
	TimeShiftingAnalysis frameEnergy100ms = new TimeShiftingAnalysis(10);
	TimeShiftingAnalysis frameEnergy200ms = new TimeShiftingAnalysis(20); 
	
	int numAttributes;
	
	public EOTFeatureAggregator() {
		numAttributes = 0;
		FastVector attInfo = new FastVector();
		framesIntoTurnAttribute = new Attribute("timeIntoTurn");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(framesIntoTurnAttribute);
		wordsIntoTurnAttribute = new Attribute("wordsIntoTurn");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(wordsIntoTurnAttribute);
		framesIntoLastWordAttribute = new Attribute("timeIntoLastWord");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(framesIntoLastWordAttribute);
		asrResultsLagAttribute = new Attribute("asrResultsLag");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(asrResultsLagAttribute);
		
		currentFrameEnergyAttribute = new Attribute("currentFrameEnergy");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(currentFrameEnergyAttribute);

		frameEnergy50msMeanAttribute = new Attribute("frameEnergy50msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msMeanAttribute);
		frameEnergy50msSlopeAttribute = new Attribute("frameEnergy50msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msSlopeAttribute);
		frameEnergy50msPredictAttribute = new Attribute("frameEnergy50msPredict");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msPredictAttribute);

		frameEnergy100msMeanAttribute = new Attribute("frameEnergy100msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msMeanAttribute);
		frameEnergy100msSlopeAttribute = new Attribute("frameEnergy100msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msSlopeAttribute);
		frameEnergy100msPredictAttribute = new Attribute("frameEnergy100msPredict");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msPredictAttribute);

		frameEnergy200msMeanAttribute = new Attribute("frameEnergy200msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msMeanAttribute);
		frameEnergy200msSlopeAttribute = new Attribute("frameEnergy200msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msSlopeAttribute);
		frameEnergy200msPredictAttribute = new Attribute("frameEnergy200msPredict");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msPredictAttribute);

		if (CLUSTERED_TIME) {
			clusteredTimeToEOT = EOTBins.eotBinsAttribute();
			attInfo.setCapacity(++numAttributes);
			attInfo.addElement(clusteredTimeToEOT);
		}
		if (CONTINUOUS_TIME) {
			timeToEOT = new Attribute("timeToEOT");
			attInfo.setCapacity(++numAttributes);
			attInfo.addElement(timeToEOT);
		}
		instances = new Instances("eotFeatures", attInfo, 0);
		if (CONTINUOUS_TIME) {
			instances.setClass(timeToEOT);			
		}
		if (CLUSTERED_TIME) {
			instances.setClass(clusteredTimeToEOT);
		}
		reset();
	}
	
	protected Instance getNewestFeatures() {
		Instance instance = new Instance(numAttributes);
		instance.setValue(framesIntoTurnAttribute, ((double) framesIntoTurnValue) / 100.0);
		instance.setValue(wordsIntoTurnAttribute, wordsIntoTurnValue);
		instance.setValue(framesIntoLastWordAttribute, ((double) framesIntoLastWordValue) / 100.0);
		instance.setValue(asrResultsLagAttribute, asrResultsLagValue);
		
		instance.setValue(currentFrameEnergyAttribute, currentFrameEnergyValue);
		instance.setValue(frameEnergy50msMeanAttribute, 
						  frameEnergy50ms.getMean());
		instance.setValue(frameEnergy50msSlopeAttribute, 
						  frameEnergy50ms.getSlope());
		instance.setValue(frameEnergy50msPredictAttribute, 
						  frameEnergy50ms.predictValueAt(framesIntoTurnValue));
		
		instance.setValue(frameEnergy100msMeanAttribute, 
				  		  frameEnergy100ms.getMean());
		instance.setValue(frameEnergy100msSlopeAttribute, 
						  frameEnergy100ms.getSlope());
		instance.setValue(frameEnergy100msPredictAttribute, 
				  		  frameEnergy100ms.predictValueAt(framesIntoTurnValue));
		
		instance.setValue(frameEnergy200msMeanAttribute, 
						  frameEnergy200ms.getMean());
		instance.setValue(frameEnergy200msSlopeAttribute, 
						  frameEnergy200ms.getSlope());
		instance.setValue(frameEnergy200msPredictAttribute, 
						  frameEnergy200ms.predictValueAt(framesIntoTurnValue));

		instance.setDataset(instances);
		instance.setClassMissing();
		return instance;
	}
	
	public void setFramesIntoTurn(int f) {
		framesIntoTurnValue = f;
	}
	
	public double getFramesIntoTurn() {
		return framesIntoTurnValue;
	}

	public void setASRResultsLag(int frames) {
		asrResultsLagValue = frames;
	}

	public double getTimeIntoTurn() {
		return ((double) framesIntoTurnValue) / 100.0; // FIXME: this should be configurable 
	}
	
	public void setWordsIntoTurn(int wordsIntoTurn) {
		wordsIntoTurnValue = wordsIntoTurn;
	}
	
	public void setFramesIntoLastWord(int framesIntoLastWord) {
		framesIntoLastWordValue = framesIntoLastWord;
	}
	
	public void setCurrentFrameEnergy(double logEnergy) {
		currentFrameEnergyValue = logEnergy;
		frameEnergy50ms.add(framesIntoTurnValue, logEnergy);
		frameEnergy100ms.add(framesIntoTurnValue, logEnergy);
		frameEnergy200ms.add(framesIntoTurnValue, logEnergy);
	}

	public void reset() {
		framesIntoTurnValue = -1;
		wordsIntoTurnValue = -1;
		framesIntoLastWordValue = -1;
		currentFrameEnergyValue = -1.0;
		frameEnergy50ms.reset();
		frameEnergy100ms.reset();
		frameEnergy200ms.reset();
	}

}
