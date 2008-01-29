package org.cocolab.inpro.features;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import edu.cmu.sphinx.util.props.Resetable;

public class EOTFeatureAggregator implements Resetable {
	
	private static final int[] energyRegressionSteps = {5, 10, 20, 50, 100, 200};
	private static final int[] voicedEnergyRegressionSteps = {50, 100, 200, 500};
	private static final int[] pitchRegressionSteps = {5, 10, 20, 50, 100, 200, 500};
	
	private static final String[] regressionParams = {"Mean", "Slope", "MSE", "PredictionError", "Range"};

	private Attribute framesIntoTurnAttribute;
/*	private Attribute wordsIntoTurnAttribute;
	private Attribute framesIntoLastWordAttribute;
	private Attribute asrResultsLagAttribute;
*/
	private Attribute currentFrameEnergyAttribute;
	// for each time in energyRegressions: mean, slope, mse, predictionError, range
	private Attribute[] energyRegressionAttributes;
	private Attribute[] voicedEnergyRegressionAttributes;
	
	private Attribute currentVoicingAttribute;
	private Attribute currentPitchAttribute;
	// for each time in pitchRegressions: mean, slope, mse, predictionError, range
	private Attribute[] pitchRegressionAttributes; 
	
	protected final static boolean CLUSTERED_TIME = false; // FIXME: this should be configurable (maybe via configurable)
	protected final static boolean CONTINUOUS_TIME = true; // FIXME: this should be configurable (via configurable interface)
	
	protected Attribute timeToEOT;
	protected Attribute clusteredTimeToEOT;
	
	protected Instances instances;
	
	int framesIntoTurnValue;
/*	int wordsIntoTurnValue;
	int framesIntoLastWordValue;
	int asrResultsLagValue;
*/
	double currentFrameEnergyValue;
	TimeShiftingAnalysis[] energyRegressions;
	TimeShiftingAnalysis[] voicedEnergyRegressions;
	boolean currentVoicingValue;
	double currentPitchValue;
	TimeShiftingAnalysis[] pitchRegressions;
	
	int numAttributes;
	
	private static Attribute[] createRegressionAttributesFor(int[] steps, String name, FastVector attInfo) {
		Attribute[] attributes = new Attribute[regressionParams.length * steps.length];
		int i = 0;
		for (int frames : steps) {
			for (String param : regressionParams) {
				attributes[i] = new Attribute(name + frames + "0ms" + param);
				attInfo.addElement(attributes[i]);
				i++;
			}
		}
		return attributes;
	}
	
	private static TimeShiftingAnalysis[] createRegressions(int[] steps) {
		TimeShiftingAnalysis[] tsas = new TimeShiftingAnalysis[steps.length];
		int i = 0;
		for (int frames : steps) {
			tsas[i] = new TimeShiftingAnalysis(frames);
			i++;
		}
		return tsas;
	}
	
	private void setRegressionValues(Attribute[] atts, TimeShiftingAnalysis[] tsas, Instance instance) {
		// when more parameters are added, this procedure has to be changed
		assert regressionParams.length == 5;  
		assert atts.length == tsas.length * 5;
		int i = 0;
		for (TimeShiftingAnalysis tsa : tsas) {
			if (tsa.hasValidData()) {
				instance.setValue(atts[i++], tsa.getMean());
				instance.setValue(atts[i++], tsa.getSlope());
				instance.setValue(atts[i++], tsa.getMSE());
				instance.setValue(atts[i++], tsa.predictValueAt(framesIntoTurnValue) - tsa.getLastValue());
				instance.setValue(atts[i++], tsa.getRange());
			} else {
				for (int j = 0; j < 5; j++) {
					instance.setMissing(atts[i++]);
				}
			}
		}
	}
	
	public EOTFeatureAggregator() {
		FastVector attInfo = new FastVector(10000); // allow for a lot of elements, trim later on
		framesIntoTurnAttribute = new Attribute("timeIntoTurn");
		attInfo.addElement(framesIntoTurnAttribute);
/*		wordsIntoTurnAttribute = new Attribute("wordsIntoTurn");
		attInfo.addElement(wordsIntoTurnAttribute);
		framesIntoLastWordAttribute = new Attribute("timeIntoLastWord");
		attInfo.addElement(framesIntoLastWordAttribute);
		asrResultsLagAttribute = new Attribute("asrResultsLag");
		attInfo.addElement(asrResultsLagAttribute);
*/		
		currentFrameEnergyAttribute = new Attribute("currentFrameEnergy");
		attInfo.addElement(currentFrameEnergyAttribute);

		energyRegressionAttributes = createRegressionAttributesFor(energyRegressionSteps, "energy", attInfo);
		energyRegressions = createRegressions(energyRegressionSteps);
		voicedEnergyRegressionAttributes = createRegressionAttributesFor(voicedEnergyRegressionSteps, "voicedEnergy", attInfo);
		voicedEnergyRegressions = createRegressions(voicedEnergyRegressionSteps);
		
		currentVoicingAttribute = new Attribute("currentVoicing");
		attInfo.addElement(currentVoicingAttribute);
		currentPitchAttribute = new Attribute("currentPitch");
		attInfo.addElement(currentPitchAttribute);

		pitchRegressionAttributes = createRegressionAttributesFor(pitchRegressionSteps, "pitch", attInfo);
		pitchRegressions = createRegressions(pitchRegressionSteps);
		
		if (CLUSTERED_TIME) {
			clusteredTimeToEOT = EOTBins.eotBinsAttribute();
			attInfo.addElement(clusteredTimeToEOT);
		}
		if (CONTINUOUS_TIME) {
			timeToEOT = new Attribute("timeToEOT");
			attInfo.addElement(timeToEOT);
		}
		instances = new Instances("eotFeatures", attInfo, 0);
//		instances.setClass(framesIntoTurnAttribute);
		if (CONTINUOUS_TIME) {
			instances.setClass(timeToEOT);			
		}
		if (CLUSTERED_TIME) {
			instances.setClass(clusteredTimeToEOT);
		}
		attInfo.trimToSize();
		numAttributes = attInfo.size();
		reset();
	}
	
	protected Instance getNewestFeatures() {
		Instance instance = new Instance(numAttributes);
		instance.setValue(framesIntoTurnAttribute, ((double) framesIntoTurnValue) / 100.0);
/*		instance.setValue(wordsIntoTurnAttribute, wordsIntoTurnValue);
		instance.setValue(framesIntoLastWordAttribute, ((double) framesIntoLastWordValue) / 100.0);
		instance.setValue(asrResultsLagAttribute, asrResultsLagValue);
*/		
		instance.setValue(currentFrameEnergyAttribute, currentFrameEnergyValue);
		
		setRegressionValues(energyRegressionAttributes, energyRegressions, instance);
		setRegressionValues(voicedEnergyRegressionAttributes, voicedEnergyRegressions, instance);
		
		instance.setValue(currentVoicingAttribute, currentVoicingValue ? 1.0 : 0.0);
		if (currentVoicingValue == true) {
			instance.setValue(currentPitchAttribute, currentPitchValue);
		} else {
			instance.setMissing(currentPitchAttribute);
		}

		setRegressionValues(pitchRegressionAttributes, pitchRegressions, instance);

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
/*
	public void setASRResultsLag(int frames) {
		asrResultsLagValue = frames;
	}
*/
	public double getTimeIntoTurn() {
		return ((double) framesIntoTurnValue) / 100.0; // FIXME: the frame rate should be configurable 
	}
/*	
	public void setWordsIntoTurn(int wordsIntoTurn) {
		wordsIntoTurnValue = wordsIntoTurn;
	}
	
	public void setFramesIntoLastWord(int framesIntoLastWord) {
		framesIntoLastWordValue = framesIntoLastWord;
	}
*/	
	public void setCurrentFrameEnergy(double logEnergy) {
		currentFrameEnergyValue = logEnergy;
		for (TimeShiftingAnalysis tsa : energyRegressions) {
			tsa.add(framesIntoTurnValue, logEnergy);
		}
		if (currentVoicingValue) {
			for (TimeShiftingAnalysis tsa : voicedEnergyRegressions) {
				tsa.add(framesIntoTurnValue, logEnergy);
			}
		}
	}

	public void setCurrentVoicing(boolean voicing) {
		currentVoicingValue = voicing;
	}

	public void setCurrentPitch(double pitch) {
		currentPitchValue = pitch;
		for (TimeShiftingAnalysis tsa : pitchRegressions) {
			if (currentVoicingValue) {
				tsa.add(framesIntoTurnValue, pitch);
			} else {
				tsa.shiftTime(framesIntoTurnValue);
			}
		}
	}	

	public void reset() {
		framesIntoTurnValue = -1;
/*		wordsIntoTurnValue = -1;
		framesIntoLastWordValue = -1;
*/		currentFrameEnergyValue = -1.0;
		for (TimeShiftingAnalysis tsa : energyRegressions) {
			tsa.reset();
		}
		for (TimeShiftingAnalysis tsa : pitchRegressions) {
			tsa.reset();
		}
	}

}
