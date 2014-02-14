package inpro.io.webspeech.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author casey
 *
 */
public class AsrHyp {
	
	static Logger log = Logger.getLogger(AsrHyp.class.getName());
	
	private boolean isFinal;
	private double timestamp;
	private String  hyp;
	private double confidence;
	private int utteranceKey;
	
	/**
	 * Constructor, set the hypothesis (usually a sentence / utterance) and the confidence score
	 * 
	 * @param hyp
	 * @param confidence
	 */
	public AsrHyp(String hyp, double confidence) {
		this.setHyp(hyp);
		this.setConfidence(confidence);
	}

	/**
	 * @return double the confidence of the hypothesis
	 */
	public double getConfidence() {
		return confidence;
	}

	/**
	 * Set the confidence score, if you have it
	 * 
	 * @param confidence
	 */
	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	/**
	 * Will split the hypothesis on whitespace and return a list of words.
	 * 
	 * @return List of Strings (words)
	 */
	@SuppressWarnings("unchecked")
	public List<String> getWords() {
		return new LinkedList<String>(Arrays.asList(this.getHyp().split("\\s+")));
	}

	/**
	 * NOTE that it will return a lower-cased version of the hypothesis!
	 * 
	 * @return String the hypothesis
	 */
	public String getHyp() {
		return hyp.toLowerCase();
	}

	/**
	 * @param hyp set the hypothesis
	 */
	public void setHyp(String hyp) {
		log.debug("New hyp:" + hyp);
		this.hyp = hyp;
	}

	public String toString() {
		return this.getHyp() + " " + this.getConfidence() + " " + timestamp;
	}

	/**
	 * @return timestamp
	 */
	public double getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp, if you have it
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @param isFinal
	 * @return boolean hypothesis is final
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * Set to true if the hypothesis is final (endpointing)
	 * 
	 * @param isFinal
	 */
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	/**
	 * @return int utterance key to help determine order of returned hypotheses
	 */
	public int getUtteranceKey() {
		return utteranceKey;
	}

	/**
	 * Set the utterance key, if you have it.
	 * 
	 * @param utteranceKey
	 */
	public void setUtteranceKey(int utteranceKey) {
		this.utteranceKey = utteranceKey;
	}

}
