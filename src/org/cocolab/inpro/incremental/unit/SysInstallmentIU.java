package org.cocolab.inpro.incremental.unit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.TTSUtil;
import org.cocolab.inpro.tts.MaryAdapter;
import org.cocolab.inpro.tts.PitchMark;

/**
 *
 * TODO: add support for canned audio (i.e. read from WAV and TextGrid, maybe even transparently)
 * TODO: cache previously synthesized audio
 * @author timo
 */
public class SysInstallmentIU extends InstallmentIU {
	
	AudioInputStream synthesizedAudio;
	
	@SuppressWarnings("unchecked") // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts) {
		super(null, tts);
		InputStream is = MaryAdapter.getInstance().text2maryxml(tts);
		List<WordIU> words = Collections.<WordIU>emptyList();
		try {
			words = TTSUtil.wordIUsFromMaryXML(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		groundedIn = (List) words;
	}
	
	@SuppressWarnings("unchecked") // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts, List<WordIU> words) {
		this(tts);
		groundedIn = (List) words;
	}
	
	@SuppressWarnings("unchecked") // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(List<WordIU> words) {
		super(null, "");
		groundedIn = (List) words;
	}
	
	@SuppressWarnings("unchecked")
	public void scaleDeepCopyAndStartAtZero(double scale) {
		List<WordIU> newWords = new ArrayList<WordIU>();
		WordIU prevWord = null;
		double startTime = startTime();
		for (WordIU w : getWords()) {
			List<SegmentIU> newSegments = new ArrayList<SegmentIU>();
			for (SegmentIU seg : w.getSegments()) {
				// TODO: these will have to become SysSegementIUs when I add pitch-scaling!
				newSegments.add(new SysSegmentIU(new Label(
						(seg.l.getStart() - startTime) * scale, 
						(seg.l.getEnd() - startTime) * scale, 
						seg.l.getLabel()
				), seg instanceof SysSegmentIU ? ((SysSegmentIU) seg).pitchMarks : Collections.<PitchMark>emptyList()));
			}
			newWords.add(new WordIU(w.word, prevWord, (List) newSegments));
		}
		groundedIn = (List) newWords;
	}
	
	public void synthesize() {
//		String mbrola = toMbrola();
//		synthesizedAudio = MaryAdapter.getInstance().mbrola2audio(mbrola);
		String maryXML = toMaryXML();
		synthesizedAudio = MaryAdapter.getInstance().maryxml2audio(maryXML);
	}
	
	public AudioInputStream getAudio() {
		return synthesizedAudio;
	}
	
	public String toMaryXML() {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\">\n<p>\n<s>\n<phrase>\n");
		for (WordIU word : getWords()) {
			word.appendMaryXML(sb);
		}
		sb.append("</phrase>\n</s>\n</p>\n</maryxml>");
		return sb.toString();
	}
	
	public String toMbrola() {
		StringBuilder sb = new StringBuilder();
		for (WordIU word : getWords()) {
			sb.append(word.toMbrolaLines());
		}
		sb.append("#\n");
		return sb.toString();
	}
	
	/** 
	 * Is this SysInstallmentIU similar(*) to the given words?
	 * (*) similarity is parameterizable:<br>
	 *   - the WER between the two sequences must be <= maxWER<br>
	 *   - a number of ultimate words in the prefix must be identical<br> 
	 * silence words are ignored in the comparison
	 */
	public FuzzyMatchResult fuzzyMatching(List<WordIU> otherPrefix, double maxWER, int matchingLastWords) {
		// look at matchingLastWords to find out which potential prefixes exist
		otherPrefix = WordIU.removeSilentWords(otherPrefix);
		if (otherPrefix.size() < matchingLastWords) 
			return new FuzzyMatchResult();
		List<WordIU> otherPrefixesLastWords = getLastNElements(otherPrefix, matchingLastWords);
		List<Prefix> myPrefixesMatchingLastWords = getPrefixesMatchingLastWords(otherPrefixesLastWords);
		// find the prefix with lowest error rate
		double wer = Double.MAX_VALUE;
		Prefix myPrefix = null;
		Iterator<Prefix> myPrefIter = myPrefixesMatchingLastWords.iterator();
		while (wer > maxWER && myPrefIter.hasNext()) {
			Prefix myCurrPref = myPrefIter.next();
			double thisWER = WordIU.getWER(WordIU.removeSilentWords(myCurrPref), otherPrefix);
			if (thisWER < wer) {
				wer = thisWER;
				myPrefix = myCurrPref;
			}
		}
		// return no-match if the prefix's WER is higher than maxWER
		if (wer > maxWER)
			myPrefix = null;
		assert myPrefix != null == wer <= maxWER;
		return new FuzzyMatchResult(myPrefix, wer);
	}
	
	/**
	 * class that describes the result of (fuzzily) matching this installment 
	 * against a list of words that potentially form a prefix of this installment
	 * @author timo
	 */
	public class FuzzyMatchResult {
		List<WordIU> prefix = null;
		List<WordIU> remainder = Collections.<WordIU>emptyList();
		double wer = Double.MAX_VALUE;
		
		private FuzzyMatchResult() {}
		private FuzzyMatchResult(Prefix prefix, double wer) {
			this.wer = wer;
			this.prefix = prefix;
			if (prefix != null)
				this.remainder = prefix.getRemainder();
		}
		
		public boolean matches() {
			return prefix != null;
		}
		
		public List<WordIU> getPrefix() {
			return prefix;
		}
		
		public List<WordIU> getRemainder() {
			return remainder;
		}
	}
	
	/** return all prefixes of this installment that end in the given last words */
	private List<Prefix> getPrefixesMatchingLastWords(List<WordIU> lastWords) {
		List<Prefix> returnList = new ArrayList<Prefix>();
		List<WordIU> myWords = getWords();
		for (int i = 0; i <= myWords.size() - lastWords.size(); i++) {
			List<WordIU> subList = myWords.subList(i, i + lastWords.size());
			if (lastWords.size() == 0 || WordIU.spellingEqual(WordIU.removeSilentWords(subList), lastWords)) {
				Prefix myPrefix = new Prefix(myWords.subList(0, i + lastWords.size()), myWords.subList(i + lastWords.size(), myWords.size()));
				returnList.add(myPrefix);
			}
		}
		return returnList;
	}
	
	/**
	 * a prefix of a list of words, which also includes the remainder 
	 * (i.e. the part of the original list that is not part of this prefix)
	 * @author timo
	 */
	@SuppressWarnings("serial")
	private static class Prefix extends ArrayList<WordIU> {
		private List<WordIU> remainder;

		public Prefix(List<WordIU> prefix, List<WordIU> remainder) {
			super(Collections.unmodifiableList(prefix));
			this.remainder = Collections.unmodifiableList(remainder);
		}
		
		List<WordIU> getRemainder() {
			return remainder;
		}
	}
	
	/** utility to return the last N elements from a list as an unmodifiable list */
	private static <T> List<T> getLastNElements(List<T> list, int n) {
		assert n >= 0;
		return Collections.unmodifiableList(list.subList(list.size() - n, list.size()));
	}
	
	@SuppressWarnings("unchecked") // allow cast of groundedIn to List<WordIU> 
	public List<WordIU> getWords() {
		return (List<WordIU>) groundedIn();
	}
	
	@Override
	public String toPayLoad() {
		StringBuilder sb = new StringBuilder();
		for (WordIU word : getWords()) {
			sb.append(word.toPayLoad());
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		SysInstallmentIU installment = new SysInstallmentIU("hallo welt");
		installment.synthesize();
		sun.audio.AudioPlayer.player.start(installment.synthesizedAudio);
	}
}
