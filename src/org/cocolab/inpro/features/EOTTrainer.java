package org.cocolab.inpro.features;

import java.io.IOException;

import weka.core.Instance;

public class EOTTrainer extends EOTFeatureAggregator {

	private double EOT;

	public EOTTrainer() {
		System.out.println(instances);
	}

	public void loadGoldStandard(String filename) {
		if (filename != null) {
			try {
				filename = filename.replaceAll("\\.\\w+$", ".ortho");
				filename = filename.replaceAll("/data/", "/par/");
				filename = filename.replaceAll("file:", "");
				String labelLine = LabelFile.getLastLine(filename);
				EOT = LabelFile.getStopTime(labelLine);
			} catch (IOException e) {
				EOT = 0.f;
				e.printStackTrace();
			}
		}
	}
	
	public double getEOTDistance() {
		return EOT - ((double) framesIntoTurnValue) / 100.0f;
	}

	public Instance getCurrentInstance() {
		Instance inst = getNewestFeatures();
		double remainingTime = EOT - getTimeIntoTurn();
		if (CLUSTERED_TIME) {
			inst.setValue(clusteredTimeToEOT, EOTBins.eotBin(remainingTime));
		}
		if (CONTINUOUS_TIME) {
			inst.setValue(timeToEOT, remainingTime);
		}
		return inst;
	}

}
