package org.cocolab.inpro.features;

import weka.core.Instance;

public class EOTTrainer extends EOTFeatureAggregator {

	private double EOT;

	public EOTTrainer() {
		System.out.println(instances);
	}
	
	public void loadGoldStandard(String filename) {
		EOT = 2.4; //FIXME: TODO: extract last sample from file in filename and convert to time value
	}
	
	public Instance getCurrentInstance() {
		Instance inst = getNewestFeatures();
		double remainingTime = EOT - getTimeIntoTurn();
		inst.setClassValue(remainingTime);
		return inst;
	}

}
