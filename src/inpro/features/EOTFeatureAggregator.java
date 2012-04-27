package inpro.features;

import inpro.pitch.notifier.SignalFeatureListener;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4String;
import edu.cmu.sphinx.instrumentation.Resetable;

/**
 * implements SignalFeatureListener and derives acoustic features 
 * (that may or may not be) useful for EOT detection
 * @author timo
 */
public class EOTFeatureAggregator implements Resetable, Configurable, SignalFeatureListener {
	
	@S4String
	public static final String PROP_PITCH_WINDOWS_LIST = "pitchWindows";
	@S4String
	public static final String PROP_ENERGY_WINDOWS_LIST = "energyWindows";
	@S4String
	public static final String PROP_VENERGY_WINDOWS_LIST = "vEnergyWindows";
	
	@S4Boolean(defaultValue = false)
	public static final String PROP_CLUSTER_TIME = "clusterTime";
	@S4Boolean(defaultValue = false)
	public static final String PROP_CONTINUOUS_TIME = "continuousTime";
	
	@S4Boolean(defaultValue = false)
	public static final String PROP_FRAME_COUNT = "timeIntoAudio";
		
	private static final int[] noSteps = {};

	private int[] energyRegressionSteps;
	private int[] voicedEnergyRegressionSteps;
	private int[] pitchRegressionSteps;
	/** names for the sub-parameters for energy, voice and pitch */ 
	private static final String[] regressionParams = {"Mean", "Slope", "MSE", 
													  "PredictionError", "Range", "MeanDelta", 
													  "MinPos", "MaxPos",
													  "MinToLast", "MaxToLast",
													  "UpCount", "DownCount", "SameCount", "PeakCount", 
													  "CountTendency", 
													  "Wesseling"};
	
	protected Attribute framesIntoAudioAttribute;
	@S4Boolean(defaultValue = true)
	boolean includeFrameCount;
/*	private Attribute wordsIntoTurnAttribute;
	private Attribute framesIntoLastWordAttribute;
*/
	private Attribute currentFrameEnergyAttribute;
	// for each time in energyRegressions: mean, slope, mse, predictionError, range, meanDelta, minPos, maxPos
	private Attribute[] energyRegressionAttributes;
	private Attribute[] voicedEnergyRegressionAttributes;
	
	private Attribute currentVoicingAttribute;
	private Attribute currentPitchAttribute;
	// for each time in pitchRegressions: mean, slope, mse, predictionError, range, meanDelta, minPos, maxPos
	private Attribute[] pitchRegressionAttributes; 
	
	@S4Boolean(defaultValue = false)
	protected boolean CLUSTERED_TIME; 
	@S4Boolean(defaultValue = false)
	protected boolean CONTINUOUS_TIME; 
	
	protected Attribute timeToEOT;
	protected Attribute turnBin;
	
	protected Instances instances;
	
	int framesIntoAudioValue;
/*	int wordsIntoTurnValue;
	int framesIntoLastWordValue;
*/
	double currentFrameEnergyValue;
	TimeShiftingAnalysis[] energyRegressions;
	TimeShiftingAnalysis[] voicedEnergyRegressions;
	boolean currentVoicingValue;
	double currentPitchValue;
	TimeShiftingAnalysis[] pitchRegressions;
	/** total number of attributes in an instance */
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
		final int regressionParameters = 16; 
		assert regressionParams.length == regressionParameters; 
		assert atts.length == tsas.length * regressionParameters;
		int i = 0;
		for (TimeShiftingAnalysis tsa : tsas) {
			if (tsa.hasValidData()) {
				instance.setValue(atts[i++], tsa.getMean());
				instance.setValue(atts[i++], tsa.getSlope());
				instance.setValue(atts[i++], tsa.getMSE());
				instance.setValue(atts[i++], tsa.predictValueAt(framesIntoAudioValue) - tsa.getLatestValue());
				instance.setValue(atts[i++], tsa.getRange());
				instance.setValue(atts[i++], tsa.getMeanStepDifference());
				instance.setValue(atts[i++], tsa.getMinPosition());
				instance.setValue(atts[i++], tsa.getMaxPosition());
				instance.setValue(atts[i++], tsa.getLatestValue() - tsa.getMin());
				instance.setValue(atts[i++], tsa.getMax() - tsa.getLatestValue());
				instance.setValue(atts[i++], tsa.getUpCount());
				instance.setValue(atts[i++], tsa.getDownCount());
				instance.setValue(atts[i++], tsa.getSameCount());
				instance.setValue(atts[i++], tsa.getPeakCount());
				instance.setValue(atts[i++], tsa.getUpCount() - tsa.getDownCount());
				instance.setValue(atts[i++], tsa.getWesseling());
			} else {
				for (int j = 0; j < regressionParameters; j++) {
					instance.setMissing(atts[i++]);
				}
			}
		}
	}
	
	public void createFeatures() {
		FastVector attInfo = new FastVector(1000); // allow for a lot of elements, trim later on
		if (includeFrameCount) {
			framesIntoAudioAttribute = new Attribute("timeIntoAudio");
			attInfo.addElement(framesIntoAudioAttribute);
		}
/*		wordsIntoTurnAttribute = new Attribute("wordsIntoTurn");
		attInfo.addElement(wordsIntoTurnAttribute);
		framesIntoLastWordAttribute = new Attribute("timeIntoLastWord");
		attInfo.addElement(framesIntoLastWordAttribute);
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
			turnBin = EOTBins.turnBinsAttribute();
			attInfo.addElement(turnBin);
		}
		if (CONTINUOUS_TIME) {
			timeToEOT = new Attribute("timeToEOT");
			attInfo.addElement(timeToEOT);
		}
		instances = new Instances("eotFeatures", attInfo, 0);
		if (CONTINUOUS_TIME) {
			instances.setClass(timeToEOT);			
		}
		if (CLUSTERED_TIME) {
			instances.setClass(turnBin);
		}
		attInfo.trimToSize();
		numAttributes = attInfo.size();
		reset();
	}
	
	public Instance getNewestFeatures() {
		Instance instance = new Instance(numAttributes);
		if (includeFrameCount) {
			instance.setValue(framesIntoAudioAttribute, getTimeIntoAudio());
		}
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
		if (CLUSTERED_TIME || CONTINUOUS_TIME) {
			instance.setClassMissing();
		}
		return instance;
	}

	public double getTimeIntoAudio() {
		return ((double) framesIntoAudioValue) / 100.f; // FIXME: the frame rate should be configurable 
	}
/*
	public void setASRResultsLag(int frames) {
		asrResultsLagValue = frames;
	}

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
			tsa.add(framesIntoAudioValue, logEnergy);
		}
		if (currentVoicingValue) {
			for (TimeShiftingAnalysis tsa : voicedEnergyRegressions) {
				tsa.add(framesIntoAudioValue, logEnergy);
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
				tsa.add(framesIntoAudioValue, pitch);
			} else {
				tsa.shiftTime(framesIntoAudioValue);
			}
		}
	}	
	
	private void reset(TimeShiftingAnalysis[] tsas) {
		if (tsas != null) {
			for (TimeShiftingAnalysis tsa : tsas) {
				tsa.reset();
			}
		}
	}

	public void reset() {
		framesIntoAudioValue = -1;
/*		wordsIntoTurnValue = -1;
		framesIntoLastWordValue = -1;
*/		currentFrameEnergyValue = -1.0;
		reset(energyRegressions);
		reset(pitchRegressions);
		reset(voicedEnergyRegressions);
	}

	private int[] regressionArrayForList(String s) {
		if (s == null) {
			return noSteps;
		}
		String[] l = s.split(" ");
		if (l.length == 0) {
			return noSteps;
		}
		int[] array = new int[l.length];
		for (int i = 0; i < l.length; i++) {
			array[i] = Integer.parseInt(l[i].toString());
		}
		return array;
	}
	
	public void printArffHeader() {
		System.out.println(instances.toString());
	}

	/* * * * * * * * *
	 * configurable  *
	 * * * * * * * * */
	
	public void newProperties(PropertySheet ps) throws PropertyException {
		String s = ps.getString(PROP_PITCH_WINDOWS_LIST);
		pitchRegressionSteps = regressionArrayForList(s);
		s = ps.getString(PROP_ENERGY_WINDOWS_LIST);
		energyRegressionSteps = regressionArrayForList(s);
		s = ps.getString(PROP_VENERGY_WINDOWS_LIST);
		voicedEnergyRegressionSteps = regressionArrayForList(s);
		CLUSTERED_TIME = ps.getBoolean(PROP_CLUSTER_TIME);
		CONTINUOUS_TIME = ps.getBoolean(PROP_CONTINUOUS_TIME);
		includeFrameCount = ps.getBoolean(PROP_FRAME_COUNT);
		createFeatures();
	}

	public double getCurrentEnergy() {
		return currentFrameEnergyValue;
	}
	
	public double getCurrentPitch() {
		return currentPitchValue;
	}

	/* * * * * * * * * * * * * *
	 * signal feature listener *
	 * * * * * * * * * * * * * */
	public void newSignalFeatures(double logEnergy, boolean voicing, double pitch) {
		framesIntoAudioValue++;
		setCurrentVoicing(voicing);
		setCurrentFrameEnergy(logEnergy);
		setCurrentPitch(pitch);
	}

	@Override
	public void newSignalFeatures(int frame, double power, boolean voicing,
			double pitch) {
		newSignalFeatures(power, voicing, pitch);
	}

}
