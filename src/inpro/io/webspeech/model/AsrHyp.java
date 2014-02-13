package inpro.io.webspeech.model;

import java.util.LinkedList;


import edu.emory.mathcs.backport.java.util.Arrays;

public class AsrHyp {
	
	private boolean isFinal;
	private double timestamp;
	private String  hyp;
	private double confidence;
	private int utteranceKey;
	
	public AsrHyp(String hyp, double confidence) {
		this.setHyp(hyp);
		this.setConfidence(confidence);
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	public LinkedList<String> getWords() {
		return new LinkedList<String>(Arrays.asList(this.getHyp().split("\\s+")));
	}

	public String getHyp() {
		return hyp.toLowerCase();
	}

	public void setHyp(String hyp) {
		this.hyp = hyp;
	}
	public String toString() {
		return this.getHyp() + " " + this.getConfidence() + " " + timestamp;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isFinal(boolean isFinal2) {
		return isFinal;
	}

	public void setFinal() {
		this.isFinal = true;
	}

	public int getUtteranceKey() {
		return utteranceKey;
	}

	public void setUtteranceKey(int utteranceKey) {
		this.utteranceKey = utteranceKey;
	}

}
