package org.cocolab.inpro.incremental.unit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.incremental.util.TTSUtil;
import org.cocolab.inpro.tts.MaryAdapter;

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
	
	public void synthesize() {
		String mbrola = toMbrola();
		synthesizedAudio = MaryAdapter.getInstance().mbrola2audio(mbrola);
	}
	
	private String toMbrola() {
		List<WordIU> words = myWords();
		StringBuilder sb = new StringBuilder();
		for (WordIU word : words) {
			sb.append(word.toMbrolaLines());
		}
		sb.append("#\n");
		return sb.toString();
	}
	
	/** 
	 * Is this SysInstallmentIU similar(*) to the given words?
	 * similarity is parameterizable:
	 *   - the WER between the two sequences must be <= maxWER
	 *   - a number of ultimate words in the prefix must be identical 
	 * silence words are ignored in the comparison
	 */
	public FuzzyMatchResult fuzzyMatching(List<WordIU> otherPrefix, double maxWER, int matchingLastWords) {
		// look at matchingLastWords to find out which potential prefixes exist
		otherPrefix = WordIU.removeSilentWords(otherPrefix);
		if (otherPrefix.size() < matchingLastWords) 
			return new FuzzyMatchResult();
		List<WordIU> otherPrefixesLastWords = getLastNElements(otherPrefix, matchingLastWords);
		List<List<WordIU>> myPrefixesMatchingLastWords = getPrefixesMatchingLastWords(otherPrefixesLastWords);
		// find the prefix with lowest error rate
		double wer = Double.MAX_VALUE;
		List<WordIU> myPrefix = null;
		Iterator<List<WordIU>> myPrefIter = myPrefixesMatchingLastWords.iterator();
		while (wer > maxWER && myPrefIter.hasNext()) {
			List<WordIU> myCurrPref = myPrefIter.next();
			double thisWER = WordIU.getWER(WordIU.removeSilentWords(myCurrPref), otherPrefix);
			if (thisWER < wer) {
				wer = thisWER;
				myPrefix = myCurrPref;
			}
		}
		// return true if the prefix's WER is lower than maxWER
		// we should probably rather return an object to encapsulate  
		// the truth value, the prefix and the remainder (if applicable)  
		assert myPrefix != null == wer <= maxWER;
		return new FuzzyMatchResult(myPrefix, wer);
	}
	
	public boolean fuzzyMatchesPrefix(List<WordIU> otherPrefix, double maxWER, int matchingLastWords) {
		return fuzzyMatching(otherPrefix, maxWER, matchingLastWords).matches();
	}
	
	public class FuzzyMatchResult {
		List<WordIU> prefix = null;
		// TODO
		List<WordIU> remainder = Collections.<WordIU>emptyList();
		double wer = Double.MAX_VALUE;
		
		FuzzyMatchResult() {}
		FuzzyMatchResult(List<WordIU> prefix, double wer) {
			this.wer = wer;
			this.prefix = prefix;
		}
		
		public boolean matches() {
			return prefix != null;
		}
		
		public List<WordIU> getPrefix() {
			return prefix;
		}
		
		// TODO
		public List<WordIU> getRemainder() {
			return null;
		}
	}
	
	/** return all prefixes of this installment that end in the given last words */
	private List<List<WordIU>> getPrefixesMatchingLastWords(List<WordIU> lastWords) {
		List<List<WordIU>> returnList = new ArrayList<List<WordIU>>();
		List<WordIU> myWords = myWords();
		for (int i = 0; i <= myWords.size() - lastWords.size(); i++) {
			List<WordIU> subList = myWords.subList(i, i + lastWords.size());
			if (lastWords.size() == 0 || WordIU.spellingEqual(WordIU.removeSilentWords(subList), lastWords)) {
				List<WordIU> myPrefix = myWords.subList(0, i+ lastWords.size());
				returnList.add(Collections.unmodifiableList(myPrefix));
			}
		}
		return returnList;
	}
	
	/** utility to return the last N elements from a list as an unmodifiable list */
	private static <T> List<T> getLastNElements(List<T> list, int n) {
		assert n >= 0;
		return Collections.unmodifiableList(list.subList(list.size() - n, list.size()));
	}
	
	/** 
	 * returns true if this SysInstallmentIU starts with the given words
	 * silence words are ignored in the comparison
	 */
	public boolean matchesPrefix(List<WordIU> prefix) {
		Iterator<WordIU> myIter = myWords().iterator();
		boolean isPrefix = true;
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) {
				WordIU myWord = myIter.hasNext() ? myIter.next() : null;
				while (myWord != null && myIter.hasNext() && myWord.isSilence()) {
					myWord = myIter.next();
				}
				if (!prefWord.spellingEquals(myWord)) {
					isPrefix = false;
				}
			}
		}
		return isPrefix;
	}
	
	public List<WordIU> getPrefix(List<WordIU> prefix) {
		assert matchesPrefix(prefix);
		List<WordIU> myPrefix = new ArrayList<WordIU>();
		Iterator<WordIU> myIter= myWords().iterator();
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) { // skip silences in user input
				WordIU myWord = myIter.next();
				while (myWord.isSilence()) { // skip silences in tts input
					myWord = myIter.next();
				}
				if (prefWord.spellingEquals(myWord)) {
					myPrefix.add(myWord);
				} else {
					break;
				}
			}
		}
		return myPrefix;
	}
	
	/** get all the words (including silences) that follow the prefix
	 * TODO: THIS IS NOT TESTED (OR USED) YET 
	 * */
	public List<WordIU> getRemainder(List<WordIU> prefix) {
		assert matchesPrefix(prefix);
		Iterator<WordIU> myIter= myWords().iterator();
		List<WordIU> remainder = new ArrayList<WordIU>();
		WordIU myWord = null;
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) {
				myWord = myIter.next();
				if (!myWord.isSilence() && !prefWord.spellingEquals(myWord)) {
					break;
				}
			}
		}
		if (myWord != null) {
			remainder.add(myWord);
		}
		while (myIter.hasNext()) {
			remainder.add(myIter.next());
		}
		return Collections.<WordIU>unmodifiableList(remainder);
	}
	
	@SuppressWarnings("unchecked") // allow cast of groundedIn to List<WordIU> 
	private List<WordIU> myWords() {
		return (List<WordIU>) groundedIn();
	}
	
	public static void main(String[] args) {
		SysInstallmentIU installment = new SysInstallmentIU("hallo welt");
		installment.synthesize();
		sun.audio.AudioPlayer.player.start(installment.synthesizedAudio);
	}
}
