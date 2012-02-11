package org.cocolab.inpro.incremental.unit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;
import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.TTSUtil;
import org.cocolab.inpro.tts.MaryAdapter;
import org.cocolab.inpro.tts.MaryAdapter4internal;
import org.cocolab.inpro.tts.PitchMark;
import org.cocolab.inpro.tts.hts.FullPFeatureFrame;
import org.cocolab.inpro.tts.hts.FullPStream;
import org.cocolab.inpro.tts.hts.IUBasedFullPStream;

/**
 * TODO: add support for canned audio (i.e. read from WAV and TextGrid, maybe even transparently)
 * @author timo
 */
public class SysInstallmentIU extends InstallmentIU {
	
	Logger logger = Logger.getLogger(SysInstallmentIU.class);
	
	AudioInputStream synthesizedAudio;
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts) {
		super(null, tts);
		// handle <hes> marker at the end separately
		boolean addFinalHesitation;
		if (tts.endsWith(" <hes>")) {
			addFinalHesitation = true;
			tts = tts.replaceAll(" <hes>$", "");
		} else 
			addFinalHesitation = false;
		InputStream is = MaryAdapter.getInstance().text2maryxml(tts);
		try {
			groundedIn = (List) TTSUtil.wordIUsFromMaryXML(is);
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
		} catch (Exception e) {
			e.printStackTrace();
			groundedIn = (List) Collections.<WordIU>emptyList();
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
	
	public void synthesize() {
		String maryXML = toMaryXML();
		synthesizedAudio = MaryAdapter.getInstance().maryxml2audio(maryXML);
	}
	
	/**
	 * queries mary for the HMM parameter set for this utterance (based on getWords())
	 */
	public FullPStream generateFullPStream() {
		String maryXML = toMaryXML();
		logger.debug("generating pstream for " + maryXML);
		assert MaryAdapter.getInstance() instanceof MaryAdapter4internal;
		FullPStream pstream = ((MaryAdapter4internal) MaryAdapter.getInstance()).maryxml2hmmFeatures(getSegments(), maryXML);
		logger.debug("resulting pstream has maxT=" + pstream.getMaxT());
		return pstream;
	}
	
	public FullPStream getFullPStream() {
		return new IUBasedFullPStream(getInitialWord());
	}
	
	public void addFeatureStreamToSegmentIUs() {
		FullPStream stream = generateFullPStream();
		List<SysSegmentIU> segments = getSegments();
		// we keep the last segment separate to be able to change it's duration
		int t = 0;
		for (SysSegmentIU seg : segments) {
			int frames = (int) ((seg.duration() * FullPStream.FRAMES_PER_SECOND) + 0.1);
			seg.setHmmSynthesisFrames(stream.getFullFrames(t, frames));
			t += frames;
		//	System.err.println(seg.toLabelLine() + " gets " + frames + " frames.");
		}
		if (stream.hasNextFrame()) {
			Logger l = Logger.getLogger(SysInstallmentIU.class);
			l.warn("discarding some unclaimed frames, t=" + t + ", maxT=" + stream.getMaxT());
			//assert t + 10 > stream.getMaxT() : "there are too many unassigned frames!";
		}
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
