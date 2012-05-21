package done.inpro.system.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.WordIU;

public class CompletionInstallmentIU extends SysInstallmentIU {

	public CompletionInstallmentIU(String fullUtterance) {
		super(fullUtterance);
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
	
	/** utility to return the last N elements from a list as an unmodifiable list */
	private static <T> List<T> getLastNElements(List<T> list, int n) {
		assert n >= 0;
		return Collections.unmodifiableList(list.subList(list.size() - n, list.size()));
	}
	

}
