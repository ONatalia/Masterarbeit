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
				String labelLine = LabelFile.getLastLine(filename);
				EOT = LabelFile.getStopTime(labelLine);
			} catch (IOException e) {
				EOT = 0.f;
				e.printStackTrace();
			}
		}
	}
	
	public Instance getCurrentInstance() {
		Instance inst = getNewestFeatures();
		double remainingTime = EOT - getTimeIntoTurn();
		inst.setClassValue(remainingTime);
		return inst;
	}

}
