/* 
 * Copyright 2007, 2008, 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.incremental.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cocolab.inpro.annotation.Label;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.flat.PronunciationState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;

public class ResultUtil {
	
	/**
	 * return a list of word and/or unit tokens extracted from a linked token list
	 * 
	 * if both words and units are to be returned, then the word token 
	 * will always precede the unit tokens belonging to this word
	 *
	 * @param token the start token (that is, the last in time)
	 * @param words whether word tokens should be returned
	 * @param units whether unit tokens hsould be returned
	 * @return a list of tokens of word and/or unit tokens
	 */
	public static LinkedList<Token> getTokenList(Token token, boolean words, boolean units) {
		LinkedList<Token> tokenList = new LinkedList<Token>();
		if (token == null) 
			return tokenList;
		if ((token.getSearchState() instanceof WordSearchState)
		 && ((WordSearchState) token.getSearchState()).getPronunciation().getWord().isSentenceEndWord()) {
			token = token.getPredecessor(); // skip the final sentence-end token (</s>)
		}
		boolean hasWordTokensLast = hasWordTokensLast(token);
		Token cachedWordToken = null;
		while (token != null) {
			SearchState searchState = token.getSearchState();
			// determine type of searchState and add to list if appropriate
			if ((searchState instanceof WordSearchState)) {
               	if (words) {
               		if (hasWordTokensLast) {
               			if (cachedWordToken != null) {
               				tokenList.add(cachedWordToken);
               			}
           				cachedWordToken = token;
               		} else {
               			tokenList.add(token);
               		}
               	}
			} else if (units && 
					  (searchState instanceof UnitSearchState) && 
					 !(searchState instanceof LexTreeLinguist.LexTreeEndUnitState)){
        	    tokenList.add(token);
            }
			token = token.getPredecessor();
		}
		if (cachedWordToken != null) {
			tokenList.add(cachedWordToken);
		}
		Collections.reverse(tokenList);
		// this removes leading silence when nothing has been recognized yet
		if (((WordSearchState) tokenList.get(0).getSearchState()).getPronunciation().getWord().isSentenceStartWord()
		) {
			tokenList.remove(0);
			if ((tokenList.get(0).getSearchState() instanceof UnitSearchState)
			 && ((UnitSearchState) tokenList.get(0).getSearchState()).getUnit().isFiller()
			) {
				tokenList.remove(0);
			} 
		}
		return tokenList;
	}
	
	public static boolean hasWordTokensLast(Token token) {
		boolean hasWordTokensLast;
		SearchState searchState = token.getSearchState();
		if (searchState instanceof LexTreeLinguist.LexTreeHMMState
		 || searchState instanceof LexTreeLinguist.LexTreeUnitState
		 || searchState instanceof LexTreeLinguist.LexTreeEndUnitState
		 || searchState instanceof LexTreeLinguist.LexTreeWordState) {
			// this is lextree
			hasWordTokensLast = true;
		} else if (searchState instanceof SentenceHMMState
				|| searchState instanceof PronunciationState) {
			// this is flat linguist
			hasWordTokensLast = false;
		} else {
			// if you see this error, then you must extend the above.
			assert false : searchState.getClass().toString();
		    hasWordTokensLast = false;
		}
		return hasWordTokensLast; 
	}
	
	public static List<Label> getWordLabelSequence(Token token) {
		return getWordLabelSequence(token, true);
	}
	
	public static List<Label> getWordLabelSequence(Token token, boolean wantFiller) {
		// first part is to traverse the token list (backwards) and find the relevant tokens
		LinkedList<Token> wordTokenList = getTokenList(token, true, false);
		// second part: traverse the tokenList backwards -- thus in time-increasing order
		Iterator<Token> it = wordTokenList.descendingIterator();
		List<Label> returnList = new ArrayList<Label>(wordTokenList.size());
		double start = 0.0;
		while (it.hasNext()) {
			Token t = it.next();
			double end = t.getFrameNumber() / 100.0;
			if (!((WordSearchState) t.getSearchState()).getPronunciation().getWord().isFiller() || wantFiller) {
				Label l = new Label(start, end, stringForSearchState(t.getSearchState()));
				returnList.add(l);
			}
			// the next label starts when the current label ends
			start = end;
		}
		return returnList;
	}
	
	public static List<String> getWordSequence(Token token) {
		return getWordSequence(token, true);
	}
	
	/** 
	 * shortcut for calling getWordLabelSequence and then using labelsToWords(); could probably be removed?
	 * @param token
	 * @param wantFiller
	 * @return
	 */
	public static List<String> getWordSequence(Token token, boolean wantFiller) {
		List<String> returnList = new LinkedList<String>();
		while (token != null) {
            if (token.isWord()) {
                WordSearchState wordState = (WordSearchState) token.getSearchState();
                Word word = wordState.getPronunciation().getWord();
                if (wantFiller || !word.isFiller()) {
                	returnList.add(0, word.getSpelling());
                }
            }
            token = token.getPredecessor();
        }
		return returnList;
	}
	
	public static List<String> labelsToWords(List<Label> ll) {
		List<String> returnList = new ArrayList<String>(ll.size());
		Iterator<Label> listIt = ll.iterator();
		while (listIt.hasNext()) {
			returnList.add(listIt.next().getLabel());
		}
		return returnList;
	}
	
    public static String stringForSearchState(SearchState state) {
        String event;
        if (state instanceof WordSearchState) {
        	WordSearchState pronunciationState = (WordSearchState) state;
        	event = pronunciationState.getPronunciation().getWord().getSpelling();
        }
        else if (state instanceof UnitSearchState) {
        	UnitSearchState unitState = (UnitSearchState) state;
        	event = unitState.getUnit().getName();
        }
        else {
        	event = state.toString();
        }
        return event;
    }
    
}
