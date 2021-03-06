package inpro.features;

import inpro.annotation.LabelFile;

import java.io.IOException;
import java.util.List;


import weka.core.Instance;

public class EOTTrainer extends EOTFeatureAggregator {

	private double startTime;
	private double stopTime;

	public void loadGoldStandard(String filename) {
		if ((CLUSTERED_TIME || CONTINUOUS_TIME) && filename != null) {
			try {
				filename = filename.replaceAll("\\.\\w+$", ".lab");
//				filename = filename.replaceAll("/data/", "/par/");
//				filename = filename.replaceAll("/data/", "/annot/ortho/");
				filename = filename.replaceAll("file:", "");
				List<String> labelLines = LabelFile.getLines(filename, 0);
				startTime = Math.floor(LabelFile.getStartTime(labelLines.get(1)) * 100) / 100;
				stopTime =  Math.floor(LabelFile.getStopTime(labelLines.get(labelLines.size() - 1)) * 100) / 100;
				System.err.println("EOTTrainer: start: " + startTime + ", stop: " + stopTime);
				if (startTime == 0) {
					System.err.println("WARNING (EOTTrainer): Are you sure, speech starts at 0.0 seconds or may something be wrong with the transcription?!?");
				}
			} catch (IOException e) {
				startTime = 0.f;
				stopTime = 0.f;
				e.printStackTrace();
			}
		}
	}
	
	public double getSOTDistance() {
		return startTime - ((double) framesIntoAudioValue) / 100.0f;
	}
	
	public double getEOTDistance() {
		return stopTime - ((double) framesIntoAudioValue) / 100.0f;
	}
	
	/*
	 * if in turn: time since turn beginning
	 * otherwise: 0.0
	 */
	public double getTimeIntoTurn() {
		return getTimeIntoAudio() - startTime;
	}

	public Instance getCurrentInstance() {
		Instance inst = getNewestFeatures();
		double timeIntoTurn = getTimeIntoTurn();
		double remainingTime = stopTime - getTimeIntoAudio();
		if (CLUSTERED_TIME) {
			inst.setValue(turnBin, EOTBins.turnState(timeIntoTurn, remainingTime));
		}
		if (CONTINUOUS_TIME) {
			inst.setValue(timeToEOT, remainingTime);
		}
		return inst;
	}

}
