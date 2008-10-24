package org.cocolab.inpro.incremental.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Word;

public class ResultUtil {
	
	public static List<String> getWordSequence(Token token) {
		List<String> returnList = new LinkedList<String>();
		boolean wantFiller = false;
		while (token != null) {
            if (token.isWord()) {
                WordSearchState wordState =
                    (WordSearchState) token.getSearchState();
                Word word = wordState.getPronunciation().getWord();
                if (wantFiller || !word.isFiller()) {
                	returnList.add(0, word.getSpelling());
                }
            }
            token = token.getPredecessor();
        }
		return returnList;
	}
	
	public static int diffWordSequences(List<String> a, List<String> b) {
		int i = 0;
		Iterator<String> aIt = a.iterator();
		Iterator<String> bIt = b.iterator();
		while (aIt.hasNext() && bIt.hasNext() && aIt.next().equals(bIt.next())) {
			i++;
		}
		aIt = a.listIterator(i);
		bIt = b.listIterator(i);
		int diff = 0;
		while (aIt.hasNext()) { aIt.next(); diff++; }
		while (bIt.hasNext()) { bIt.next(); diff++; }
		return diff;
	}
	
}
