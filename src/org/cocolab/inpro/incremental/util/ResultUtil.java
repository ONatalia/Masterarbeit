package org.cocolab.inpro.incremental.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.flat.PronunciationState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;

public class ResultUtil {
	
	public static double FRAME_TO_SECOND_FACTOR = 0.01;
	public static double SECOND_TO_MILLISECOND_FACTOR = 1000.0;
	
	/**
	 * return a list of word and/or unit tokens extracted from a linked token list
	 * 
	 * if both words and units are to be returned, then the word token 
	 * will always precede the unit tokens belonging to this word
	 * 
	 * the algorithm iterates through the tokens *against* temporal order 
	 * (as this is the natural order of the tokens)   
	 * and finally reverses the list before it is output.
	 * 
	 * This means that even though word tokens precede their segment tokens in 
	 * the final output, the opposite is the case while the list is being 
	 * constructed.
	 * 
	 * @param currentToken the start token (that is, the last in time)
	 * @param words whether word tokens should be returned
	 * @param units whether unit tokens hsould be returned
	 * @return a list of tokens of word and/or unit tokens NOTICE: that no provisions
	 * are taken to ensure that there are the "right" segment tokens for every word,
	 * especially, no provisions are taken to assert that each silence segment is 
	 * accompanied by a silence word
	 */
	public static List<Token> getTokenList(Token inputToken, boolean words, boolean units) {
		ArrayList<Token> outputTokens = new ArrayList<Token>();
		if (inputToken == null) 
			return outputTokens;
		Token currentToken = inputToken; 
		if ((currentToken.getSearchState() instanceof WordSearchState)
		 && ((WordSearchState) currentToken.getSearchState()).getPronunciation().getWord().isSentenceEndWord()) {
			currentToken = currentToken.getPredecessor(); // skip the final sentence-end token (</s>)
		}
		boolean hasWordTokensLast = hasWordTokensLast(currentToken);
		Token cachedWordToken = null;
		while (currentToken != null) {
			// determine type of searchState and add to list if appropriate
			if (words && isWordToken(currentToken)) {
           		if (hasWordTokensLast) {
               		// don't be fooled: as the list is reversed later on, segment
               		// tokens actually precede their word tokens at this point
               		// of the algorithm
           			if (cachedWordToken != null) {
           				outputTokens.add(cachedWordToken);
           			}
       				cachedWordToken = currentToken;
           		} else {
           			outputTokens.add(currentToken);
           		}
			} else if (units && isSegmentToken(currentToken)) {
        	    outputTokens.add(currentToken);
				
			}
			currentToken = currentToken.getPredecessor();
		}
		if (cachedWordToken != null) {
			outputTokens.add(cachedWordToken);
		}
		Collections.reverse(outputTokens);
		// this removes leading silence when nothing has been recognized yet
		if (((WordSearchState) outputTokens.get(0).getSearchState()).getPronunciation().getWord().isSentenceStartWord()
		) {
			outputTokens.remove(0);
			if ((outputTokens.get(0).getSearchState() instanceof UnitSearchState)
			 && ((UnitSearchState) outputTokens.get(0).getSearchState()).getUnit().isFiller()
			) {
				outputTokens.remove(0);
			} 
		}
		return outputTokens;
	}
	
	private static boolean isWordToken(Token t) {
		SearchState s = t.getSearchState();
		return (s instanceof WordSearchState);		
	}
	
	private static boolean isSegmentToken(Token t) {
		SearchState s = t.getSearchState();
		return ((s instanceof UnitSearchState) && 
		        !(s instanceof LexTreeLinguist.LexTreeEndUnitState));

	}
	
	/**
	 * try to guess the ordering of tokens 
	 * the ordering of word/segment tokens depends on the sphinx decoder used;
	 * this operation guesses the decoder from the employed search state 
	 * subclasses and returns the word-order for the types of decoders it knows
	 * @param token this token's search state will be inspected
	 * @return true if word tokens are preceded by their segments
	 */
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
			assert searchState != null : token;
			assert false : searchState.getClass().toString();
		    hasWordTokensLast = false;
		}
		return hasWordTokensLast; 
	}
	
	
	/** ancient code, but still used in the sphinx-native LabelWriter and TEDviewNotifier */
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
