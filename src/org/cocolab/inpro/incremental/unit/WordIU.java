package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.features.TimeShiftingAnalysis;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.nlu.AVPair;

import weka.core.Instance;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

public class WordIU extends IU {

	/* TODO: implement magic to actually fill this map */
	static Map<String, List<AVPair>> avPairs;
	
	final boolean isSilence;
	final Pronunciation pron;
	final String word;

	public WordIU(Pronunciation pron, WordIU sll, List<IU> groundedIn) {
		this(pron.getWord().getSpelling(), pron, sll, groundedIn);
	}
	
	protected WordIU(String word, Pronunciation pron, WordIU sll, List<IU> groundedIn) {
		super(sll, groundedIn, true);
		this.pron = pron;
		this.word = word;
		isSilence = this.word.equals(("<sil>"));
	}
	
	/**
	 * create a new silent word
	 * @param sll
	 */
	public WordIU(WordIU sll) {
		super(sll, Collections.nCopies(1, 
					(IU) new SyllableIU(null, Collections.nCopies(1, 
								(IU) new SegmentIU("SIL", null)))), 
			true);
		this.pron = Pronunciation.UNKNOWN;
		this.word = "<sil>";
		isSilence = true;
	}
	
	public List<AVPair> getAVPairs() {
		return avPairs.get(this.getWord());
	}
	
	@SuppressWarnings("unchecked") // the untyped list in the call to Collections.checkedList
	public List<SegmentIU> getSegments() {
		List<IU> returnList;
		if ((groundedIn == null) || groundedIn.size() == 0) {
			returnList = Collections.emptyList();
		} else if (groundedIn.get(0) instanceof SegmentIU) {
			returnList = groundedIn;
		} else if (groundedIn.get(0) instanceof SyllableIU) {
			returnList = new ArrayList<IU>();
			for (IU gIn : groundedIn) {
				returnList.addAll(gIn.groundedIn);
			}
		} else {
			throw new RuntimeException("I don't know how to get segments from my groundedIn list");
		}
		return Collections.checkedList((List) returnList, SegmentIU.class);
	}
	
	public void updateSegments(List<Label> newLabels) {
		List<SegmentIU> segments = getSegments();
		assert (segments.size() >= newLabels.size())
			: "something is wrong when updating segments in word:"
			+ this.toString()
			+ "I was supposed to add the following labels:"
			+ newLabels
			+ "but my segments are:"
			+ segments;
		Iterator<SegmentIU> segIt = segments.iterator();
		for (Label label : newLabels) {
			segIt.next().updateLabel(label);
		}
	}
	
	/**
	 * this operation updates the timings of the grounding segments to
	 * match those of another IU for the same word (in terms of spelling)
	 * @param otherWord the word to base the new timings on
	 */
	public void updateTimings(WordIU otherWord) {
		assert this.wordEquals(otherWord) : "Can't update segment timings based on a different word's segments";
		List<Label> newLabels = new ArrayList<Label>();
		for (SegmentIU segment : otherWord.getSegments()) {
			newLabels.add(segment.l);
		}
		updateSegments(newLabels);
	}
	
	public boolean wordEquals(Pronunciation pron) {
		// words are equal if their pronunciations match
		// OR if the word is silent and the other's pronunciation is silent as well
		return ((isSilence && pron.getWord().isFiller()) || this.pron.equals(pron));
	}
	
	public boolean wordEquals(WordIU iu) {
		return ((isSilence && iu.isSilence) || pron.equals(iu.pron));
	}
	
	public String getWord() {
		return word;
	}
	
	public boolean isSilence() {
		return isSilence;
	}
	
	public static void setAVPairs(Map<String, List<AVPair>> avPairs) {
		assert (WordIU.avPairs == null) : "You're trying to re-set avPairs. This may be a bug.";
		WordIU.avPairs = avPairs;
	}

	public boolean hasProsody() {
		return (bd != null && startTime() != Double.NaN && endTime() != Double.NaN && !isSilence());
	}

	/**
	 * <strong>precondition</strong>: only call this if hasProsody()
	 */
	public boolean pitchIsRising() {
		assert hasProsody();
		if (prosodicFeatures == null) {
			prosodicFeatures = new ProsodicFeatures();
			Instance instance = bd.getEOTFeatures(endTime());
			System.err.println(instance.toString());
		}
		Logger.getLogger(WordIU.class).info(this + " has rising prosody: " + prosodicFeatures.pitchIsRising());
		return prosodicFeatures.pitchIsRising();
	}
	
	private ProsodicFeatures prosodicFeatures;
	
	private class ProsodicFeatures {
		
		TimeShiftingAnalysis tsa;

		ProsodicFeatures() {
			tsa = new TimeShiftingAnalysis();
			for (double time = startTime(); time < endTime(); time += 0.01) {
				int frame = (int) Math.floor(time * ResultUtil.SECOND_TO_MILLISECOND_FACTOR + 0.5);
				double pitch = bd.getPitchInCent(time);
				if (!Double.isNaN(pitch))
					tsa.add(frame, pitch);
			}
		}
		
		public boolean pitchIsRising() {
			return tsa.getSlope() > 2.5; // this means: 2.5 half-tones per second
		} 
		
	}

	@Override
	public String toPayLoad() {
		return word;
	}
	
	/** 
     * Builds a simple string from a list of wordIUs
	 * @return a string with the contained words separated by whitespace
	 */
	public static String wordsToString(List<WordIU> words) {
		String ret = "";
		for (WordIU iu : words) {
			if (!iu.isSilence()) {
				ret += iu.getWord() + " ";				
			}
		}
		return ret.replaceAll("^ *", "").replaceAll(" *$", "");
	}
	
}
