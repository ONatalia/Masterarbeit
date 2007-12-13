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
	
	private static final String[] regressionParams = {"Mean", "Slope", "MSE", "Prediction", "PredictionError", "Range"};

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
		assert regressionParams.length == 6;  
		assert atts.length == tsas.length * 6;
		int i = 0;
		for (TimeShiftingAnalysis tsa : tsas) {
			instance.setValue(atts[i++], tsa.getMean());
			instance.setValue(atts[i++], tsa.getSlope());
			instance.setValue(atts[i++], tsa.getMSE());
			instance.setValue(atts[i++], tsa.predictValueAt(framesIntoTurnValue));
			instance.setValue(atts[i++], tsa.predictValueAt(framesIntoTurnValue) - tsa.getLastValue());
			instance.setValue(atts[i++], tsa.getRange());
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
		instance.setValue(currentPitchAttribute, currentPitchValue);

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
			if (currentVoicingValue) {
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
			if (pitch > 0.0) {
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

/*

	private Attribute frameEnergy50msMeanAttribute;
	private Attribute frameEnergy50msSlopeAttribute;
	private Attribute frameEnergy50msPredictionErrorAttribute;
	private Attribute frameEnergy100msMeanAttribute;
	private Attribute frameEnergy100msSlopeAttribute;
	private Attribute frameEnergy100msPredictionErrorAttribute;
	private Attribute frameEnergy200msMeanAttribute;
	private Attribute frameEnergy200msSlopeAttribute;
	private Attribute frameEnergy200msPredictionErrorAttribute;

	private Attribute pitch50msMeanAttribute;
	private Attribute pitch50msSlopeAttribute;
	private Attribute pitch50msPredictionErrorAttribute;
	private Attribute pitch100msMeanAttribute;
	private Attribute pitch100msSlopeAttribute;
	private Attribute pitch100msPredictionErrorAttribute;
	private Attribute pitch200msMeanAttribute;
	private Attribute pitch200msSlopeAttribute;
	private Attribute pitch200msPredictionErrorAttribute;
	private Attribute pitch500msMeanAttribute;
	private Attribute pitch500msSlopeAttribute;
	private Attribute pitch500msPredictionErrorAttribute;
	private Attribute pitch1000msMeanAttribute;
	private Attribute pitch1000msSlopeAttribute;
	private Attribute pitch1000msPredictionErrorAttribute;
	private Attribute pitch2000msMeanAttribute;
	private Attribute pitch2000msSlopeAttribute;
	private Attribute pitch2000msPredictionErrorAttribute;


	TimeShiftingAnalysis frameEnergy50ms = new TimeShiftingAnalysis(5);
	TimeShiftingAnalysis frameEnergy100ms = new TimeShiftingAnalysis(10);
	TimeShiftingAnalysis frameEnergy200ms = new TimeShiftingAnalysis(20);

	TimeShiftingAnalysis pitch50ms = new TimeShiftingAnalysis(5);
	TimeShiftingAnalysis pitch100ms = new TimeShiftingAnalysis(10);
	TimeShiftingAnalysis pitch200ms = new TimeShiftingAnalysis(20);
	TimeShiftingAnalysis pitch500ms = new TimeShiftingAnalysis(50);
	TimeShiftingAnalysis pitch1000ms = new TimeShiftingAnalysis(100);
	TimeShiftingAnalysis pitch2000ms = new TimeShiftingAnalysis(200);


		frameEnergy50msMeanAttribute = new Attribute("frameEnergy50msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msMeanAttribute);
		frameEnergy50msSlopeAttribute = new Attribute("frameEnergy50msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msSlopeAttribute);
		frameEnergy50msPredictionErrorAttribute = new Attribute("frameEnergy50msPredictionError");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy50msPredictionErrorAttribute);

		frameEnergy100msMeanAttribute = new Attribute("frameEnergy100msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msMeanAttribute);
		frameEnergy100msSlopeAttribute = new Attribute("frameEnergy100msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msSlopeAttribute);
		frameEnergy100msPredictionErrorAttribute = new Attribute("frameEnergy100msPredictionError");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy100msPredictionErrorAttribute);

		frameEnergy200msMeanAttribute = new Attribute("frameEnergy200msMean");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msMeanAttribute);
		frameEnergy200msSlopeAttribute = new Attribute("frameEnergy200msSlope");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msSlopeAttribute);
		frameEnergy200msPredictionErrorAttribute = new Attribute("frameEnergy200msPredictionError");
		attInfo.setCapacity(++numAttributes);
		attInfo.addElement(frameEnergy200msPredictionErrorAttribute);



		instance.setValue(frameEnergy50msMeanAttribute, 
						  frameEnergy50ms.getMean());
		instance.setValue(frameEnergy50msSlopeAttribute, 
						  frameEnergy50ms.getSlope());
		instance.setValue(frameEnergy50msPredictionErrorAttribute, 
						  frameEnergy50ms.predictValueAt(framesIntoTurnValue) - currentFrameEnergyValue);
		
		instance.setValue(frameEnergy100msMeanAttribute, 
				  		  frameEnergy100ms.getMean());
		instance.setValue(frameEnergy100msSlopeAttribute, 
						  frameEnergy100ms.getSlope());
		instance.setValue(frameEnergy100msPredictionErrorAttribute, 
				  		  frameEnergy100ms.predictValueAt(framesIntoTurnValue) - currentFrameEnergyValue);
		
		instance.setValue(frameEnergy200msMeanAttribute, 
						  frameEnergy200ms.getMean());
		instance.setValue(frameEnergy200msSlopeAttribute, 
						  frameEnergy200ms.getSlope());
		instance.setValue(frameEnergy200msPredictionErrorAttribute, 
						  frameEnergy200ms.predictValueAt(framesIntoTurnValue) - currentFrameEnergyValue);

		instance.setValue(pitch50msMeanAttribute, 
						  pitch50ms.getMean());
		instance.setValue(pitch50msSlopeAttribute, 
						  pitch50ms.getSlope());
		instance.setValue(pitch50msPredictionErrorAttribute, 
						  pitch50ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);

		instance.setValue(pitch100msMeanAttribute, 
						  pitch100ms.getMean());
		instance.setValue(pitch100msSlopeAttribute, 
						  pitch100ms.getSlope());
		instance.setValue(pitch100msPredictionErrorAttribute, 
						  pitch100ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);

		instance.setValue(pitch200msMeanAttribute, 
						  pitch200ms.getMean());
		instance.setValue(pitch200msSlopeAttribute, 
						  pitch200ms.getSlope());
		instance.setValue(pitch200msPredictionErrorAttribute, 
						  pitch200ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);

		instance.setValue(pitch500msMeanAttribute, 
						  pitch500ms.getMean());
		instance.setValue(pitch500msSlopeAttribute, 
						  pitch500ms.getSlope());
		instance.setValue(pitch500msPredictionErrorAttribute, 
						  pitch500ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);
		
		instance.setValue(pitch1000msMeanAttribute, 
						  pitch1000ms.getMean());
		instance.setValue(pitch1000msSlopeAttribute, 
						  pitch1000ms.getSlope());
		instance.setValue(pitch1000msPredictionErrorAttribute, 
						  pitch1000ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);
		
		instance.setValue(pitch2000msMeanAttribute, 
						  pitch2000ms.getMean());
		instance.setValue(pitch2000msSlopeAttribute, 
						  pitch2000ms.getSlope());
		instance.setValue(pitch2000msPredictionErrorAttribute, 
						  pitch2000ms.predictValueAt(framesIntoTurnValue) - currentPitchValue);


		frameEnergy50ms.add(framesIntoTurnValue, logEnergy);
		frameEnergy100ms.add(framesIntoTurnValue, logEnergy);
		frameEnergy200ms.add(framesIntoTurnValue, logEnergy);

			if (pitch > 0.0) {
				pitch50ms.add(framesIntoTurnValue, pitch);
				pitch100ms.add(framesIntoTurnValue, pitch);
				pitch200ms.add(framesIntoTurnValue, pitch);
				pitch500ms.add(framesIntoTurnValue, pitch);
				pitch1000ms.add(framesIntoTurnValue, pitch);
				pitch2000ms.add(framesIntoTurnValue, pitch);
			} else {
				pitch50ms.shiftTime(framesIntoTurnValue);
				pitch100ms.shiftTime(framesIntoTurnValue);
				pitch200ms.shiftTime(framesIntoTurnValue);
				pitch500ms.shiftTime(framesIntoTurnValue);
				pitch1000ms.shiftTime(framesIntoTurnValue);
				pitch2000ms.shiftTime(framesIntoTurnValue);
			}

		frameEnergy50ms.reset();
		frameEnergy100ms.reset();
		frameEnergy200ms.reset();
		pitch50ms.reset();
		pitch100ms.reset();
		pitch200ms.reset();
		pitch500ms.reset();
		pitch1000ms.reset();
		pitch2000ms.reset();
*/
