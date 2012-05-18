package inpro.incremental.unit;

import inpro.annotation.Label;
import inpro.audio.DDS16kAudioInputStream;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.PitchMark;
import inpro.synthesis.hts.FullPFeatureFrame;
import inpro.synthesis.hts.FullPStream;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import work.inpro.incremental.unit.HesitationIU;

/**
 * TODO: add support for canned audio (i.e. read from WAV and TextGrid, maybe even transparently)
 * @author timo
 */
public class SysInstallmentIU extends InstallmentIU {
	
	Logger logger = Logger.getLogger(SysInstallmentIU.class);
	
	public SysInstallmentIU(String tts) {
		super(null, tts);
		// handle <hes> marker at the end separately
		boolean addFinalHesitation;
		if (tts.endsWith(" <hes>")) {
			addFinalHesitation = true;
			tts = tts.replaceAll(" <hes>$", "");
		} else 
			addFinalHesitation = false;
		try {
			groundedIn = MaryAdapter.getInstance().text2IUs(tts);
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		// remove utterance final silences
		IU pred = groundedIn.get(groundedIn.size() - 1);
		while (((WordIU) pred).isSilence) {
			groundedIn.remove(pred);
			pred.getSameLevelLink().removeAllNextSameLevelLinks(); // cut GRIN-NextSLLs
			pred = pred.getSameLevelLink();
		}
		if (addFinalHesitation) {
			HesitationIU hes = new HesitationIU(null);
			hes.shiftBy(pred.endTime());
			hes.connectSLL(pred);
			pred.setAsTopNextSameLevelLink("<hes>");
			groundedIn.add(hes);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts, List<WordIU> words) {
		this(tts);
		groundedIn = (List) words;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(List<? extends IU> words) {
		super(null, "");
		groundedIn = (List) words;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void scaleDeepCopyAndStartAtZero(double scale) {
		List<WordIU> newWords = new ArrayList<WordIU>();
		WordIU prevWord = null;
		double startTime = startTime();
		for (WordIU w : getWords()) {
			List<SysSegmentIU> newSegments = new ArrayList<SysSegmentIU>();
			for (SegmentIU seg : w.getSegments()) {
				// TODO: these will have to become SysSegementIUs when I add pitch-scaling!
				newSegments.add(new SysSegmentIU(new Label(
						(seg.l.getStart() - startTime) * scale, 
						(seg.l.getEnd() - startTime) * scale, 
						seg.l.getLabel()
				), seg instanceof SysSegmentIU ? ((SysSegmentIU) seg).pitchMarks : Collections.<PitchMark>emptyList(),
				   seg instanceof SysSegmentIU ? ((SysSegmentIU) seg).hmmSynthesisFeatures : Collections.<FullPFeatureFrame>emptyList()));
			}
			// connect same-level-links
			Iterator<SysSegmentIU> it = newSegments.iterator();
			SysSegmentIU first = it.next();
			while (it.hasNext()) {
				SysSegmentIU next = it.next();
				next.setSameLevelLink(first);
				first = next;
			}
			WordIU newWord = new WordIU(w.word, prevWord, (List) newSegments);
			newWords.add(newWord);
			prevWord = newWord;
		}
		groundedIn = (List) newWords;
	}
	
	/**
	 * return the HMM parameter set for this utterance (based on getWords())
	 */
	public FullPStream getFullPStream() {
		return new IUBasedFullPStream(getInitialWord());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<SysSegmentIU> getSegments() {
		List<SysSegmentIU> segments = new ArrayList<SysSegmentIU>();
		for (WordIU word : getWords()) {
			segments.addAll((List) word.getSegments());
		}
		return segments;
	}
	
	public CharSequence toLabelLines() {
		StringBuilder sb = new StringBuilder();
		for (SysSegmentIU seg : getSegments()) {
			sb.append(seg.toLabelLine());
			sb.append("\n");
		}
		return sb;
	}
	
	public AudioInputStream getAudio() {
        boolean immediateReturn = true;
		VocodingAudioStream vas = new VocodingAudioStream(getFullPStream(), immediateReturn);
        return new DDS16kAudioInputStream(vas);
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
	
	public WordIU getInitialWord() {
		return (WordIU) groundedIn.get(0);
	}
	
	public WordIU getFinalWord() {
		List<WordIU> words = getWords();
		return words.get(words.size() - 1);
	}
	
	public List<WordIU> getWords() {
		List<WordIU> activeWords = new ArrayList<WordIU>();
		WordIU word = getInitialWord();
		while (word != null) {
			activeWords.add(word);
			word = (WordIU) word.getNextSameLevelLink();
		}
		return activeWords;
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
	
	public String toMarkedUpString() {
		StringBuilder sb = new StringBuilder();
		for (WordIU word : getWords()) {
			String payload = word.toPayLoad().replace(">", "&gt;").replace("<", "&lt;");
			if (word.isCompleted()) {
				sb.append("<strong>");
				sb.append(payload);
				sb.append("</strong>");
			} else if (word.isOngoing()) {
				sb.append("<em>");
				sb.append(payload);
				sb.append("</em>");
			} else
				sb.append(payload);
			sb.append(" ");
		}
		return sb.toString();
	}
	
}
