package inpro.incremental.unit;

import inpro.annotation.Label;
import inpro.features.TimeShiftingAnalysis;
import inpro.incremental.util.ResultUtil;
import inpro.nlu.AVPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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
	
	public WordIU(String spelling, WordIU sll, List<IU> groundedIn) {
		this(spelling, null, sll, groundedIn);
	}
	
	protected WordIU(String word, Pronunciation pron, WordIU sll, List<IU> groundedIn) {
		super(sll, groundedIn, true);
		this.pron = pron;
		this.word = word;
		isSilence = this.word.equals("<sil>");
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
	
	public boolean hasAVPairs() {
		return avPairs.get(getWord()) != null;
	}
	
	public List<AVPair> getAVPairs() {
		return avPairs.get(this.getWord());
	}
	
	/**
	 * Retrieves all values of an WordIU's AVPairs, given an attribute.
	 * 
	 * @param attribute the attribute to retrieve
	 * @return the list of values of a given word's AVPairs that matched the attribute.
	 */
	public List<String> getValues(String attribute) {
		List<String> values = new ArrayList<String>();
		for (AVPair avp : WordIU.avPairs.get(this.getWord())) {
			if (avp.getAttribute().equals(attribute)) {
				values.add((String) avp.getValue());
			}
		}
		return values;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) // the untyped list in the call to Collections.checkedList
	public List<SegmentIU> getSegments() {
		List<IU> returnList;
		if ((groundedIn == null) || groundedIn.size() == 0) {
			returnList = Collections.<IU>emptyList();
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
	
	public SegmentIU getFirstSegment() {
		return getSegments().get(0);
	}
	
	public SegmentIU getLastSegment() {
		List<SegmentIU> segments = getSegments();
		if (segments.size() > 0)
			return segments.get(segments.size() - 1);
		else
			return null;
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
		assert this.pronunciationEquals(otherWord) : "Can't update segment timings based on a different word's segments";
		List<Label> newLabels = new ArrayList<Label>();
		for (SegmentIU segment : otherWord.getSegments()) {
			newLabels.add(segment.l);
		}
		updateSegments(newLabels);
	}
	
	public boolean pronunciationEquals(Pronunciation pron) {
		// words are equal if their pronunciations match
		// OR if the word is silent and the other's pronunciation is silent as well
		return ((isSilence && pron.getWord().isFiller()) || this.pron.equals(pron));
	}
	
	public boolean pronunciationEquals(WordIU iu) {
		assert pron != null;
		assert iu.pron != null;
		return ((isSilence && iu.isSilence) || pron.equals(iu.pron));
	}
	
	public boolean spellingEquals(WordIU iu) {
		return (iu != null) && (getWord().equals(iu.getWord()));
	}
	
	public String getWord() {
		return word;
	}
	
	/** shift the start and end times of this (and possibly all following SysSegmentIUs */
	public void shiftBy(double offset) {
		for (SegmentIU segment : getSegments()) {
			if (segment instanceof SysSegmentIU) {
				((SysSegmentIU) segment).shiftBy(offset, false);
			}
		}
	}
	
	public boolean isSilence() {
		return isSilence;
	}
	
	public boolean isBackchannel() {
		return this.pitchIsRising() && this.word.equals("hm");
	}
	
	public static void setAVPairs(Map<String, List<AVPair>> avPairs) {
//		assert (WordIU.avPairs == null) : "You're trying to re-set avPairs. This may be a bug.";
		WordIU.avPairs = avPairs;
	}

	public boolean hasProsody() {
		return (bd != null && !Double.isNaN(startTime()) && !Double.isNaN(endTime()) && !isSilence());
	}

	/**
	 * <strong>precondition</strong>: only call this if hasProsody()
	 */
	public boolean pitchIsRising() {
		assert hasProsody();
		if (prosodicFeatures == null) {
			prosodicFeatures = new ProsodicFeatures();
//			Instance instance = bd.getEOTFeatures(endTime());
//			System.err.println(instance.toString());
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
		StringBuilder ret = new StringBuilder();
		for (WordIU iu : words) {
			if (!iu.isSilence()) {
				ret.append(iu.getWord());
				ret.append(" ");				
			}
		}
		return ret.toString().replaceAll("^ *", "").replaceAll(" *$", "");
	}

	public StringBuilder toMbrolaLines() {
		StringBuilder sb = new StringBuilder("; ");
		sb.append(toPayLoad());
		sb.append("\n");
		for (SegmentIU seg : getSegments()) {
			sb.append(seg.toMbrolaLine());
		}
		return sb;
	}
	
	public void appendMaryXML(StringBuilder sb) {
		if ("<sil>".equals(toPayLoad())) {
			sb.append("<boundary duration='");
			sb.append(duration());
			sb.append("'/>\n");
		} else { 
			sb.append("<t>\n");
			sb.append(toPayLoad().replace("<", "&lt;").replace(">", "&lt;"));
			sb.append("\n<syllable>\n");
			for (SegmentIU seg : getSegments()) {
				seg.appendMaryXML(sb);
			}
			sb.append("</syllable>\n</t>\n");
		}
	}


	
	/** returns a new list with all silent words removed */
	public static List<WordIU> removeSilentWords(List<WordIU> words) {
		List<WordIU> outList = new ArrayList<WordIU>(words);
		Iterator<WordIU> iter = outList.iterator();
		while (iter.hasNext()) {
			if (iter.next().isSilence())
				iter.remove();
		}
		return outList;
	}
	
	public static boolean spellingEqual(List<WordIU> a, List<WordIU> b) {
		boolean equality = (a.size() == b.size());
		if (equality) // only bother if lists are of same size
			for (int i = 0; i < a.size(); i++)
				if (!a.get(i).spellingEquals(b.get(i)))
					equality = false;
		return equality;
	}
	
	/** 
	 * calculate the WER between two lists of words based on the levenshtein distance 
	 * code adapted from http://www.merriampark.com/ldjava.htm
	 * list a is taken as the reference for word error rate normalization 
	 */
	public static double getWER(List<WordIU> a, List<WordIU> b) {
		if (a == null || b == null)
			throw new IllegalArgumentException("Strings must not be null");
		int n = a.size();
		int m = b.size();
		// handle special cases with empty lists 
		if (n == 0)
			return m;
		else if (m == 0)
			return n;
		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int c[] = new int[n + 1]; // cost array, horizontally
		// indexes into strings s and t
		// initialize costs
		for (int i = 0; i <= n; i++) {
			p[i] = i;
		}
		for (int j = 1; j <= m; j++) {
			WordIU bCurrent = b.get(j - 1);
			c[0] = j;
			for (int i = 1; i <= n; i++) {
				int cost = a.get(i - 1).spellingEquals(bCurrent) ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up + cost
				c[i] = min(c[i - 1] + 1, 
						   p[i] + 1, 
						   p[i - 1] + cost);
			}
			// swap current and previous cost arrays
			int s[];
			s = p;
			p = c;
			c = s;
		}
		return ((double) p[n]) / a.size();
	}
	
	/** used internally in getWER */
	private static final int min(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	
}
