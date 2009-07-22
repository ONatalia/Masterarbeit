package org.cocolab.inpro.sphinx.decoder;


import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.annotation.TextGrid;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.flat.PronunciationState;
import edu.cmu.sphinx.linguist.flat.UnitState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;

/**
 * recognizing is so time consuming 
 * 
 * why not have a recognizer that doesn't recognize at all,
 * fakes results from a given transcription,
 * just consumes frames from the frontend 
 * and calls event listeners and monitors as needed
 * 
 * @author timo
 *
 */

public class FakeSearch extends NoSearch {

	@S4Component(type = Dictionary.class)
	public static final String PROP_DICTIONARY = "dictionary";
	
	@S4String(defaultValue = "")
	public static final String PROP_TRANSCRIPT_FILE = "textGrid";

	@S4String(defaultValue = "ORT:")
	public static final String PROP_WORD_TIER = "asrWords";

	@S4String(defaultValue = "MAU:")
	public static final String PROP_UNIT_TIER = "asrUnits";
	
	private Dictionary dictionary;
	
	private String wordTierName;
	private String unitTierName;
	private String transcriptName;
	
	private TokenList sortedTokenList;
	private int frameNumber = 0;

    public void allocate() {
    	frameNumber = 0;
    	try {
			dictionary.allocate();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
        if (!transcriptName.equals("")) {
        	loadTranscript(transcriptName);
        } else {
        	sortedTokenList = new TokenList();
        }
	}

    /**
     * consume the given number of frames from the frontend,
     * find the Token for the result (completely ignoring the data...)
     * @param nFrames the number of frames to recognize
     * @return a result that contains information from the transcription up to the current time 
     */
	public Result recognize(int nFrames) {
		boolean finalResult = false;
		for (int i = 0; i < nFrames; i++) {
			try {
				Data d = fe.getData();
				if (d instanceof DataEndSignal) 
					finalResult = true;
				// only send results for new data, not for signals
				while ((d != null) && (d instanceof Signal)) {
					d = fe.getData();
					if (d instanceof DataEndSignal) 
						finalResult = true;
				}
				frameNumber++;
			} catch (DataProcessingException e) {
				e.printStackTrace();
			}
		}
		Token resultToken = sortedTokenList.get(frameNumber);
		return new Result(null, Collections.nCopies(1, resultToken), frameNumber, finalResult, null);
	}
	
	/**
	 * create the internal sortedTokenList from the given Textgrid
	 * 
	 * this works as follows:
	 * - iterate over the words in the word tier
	 * 		- find pronunciation from the dictionary
	 * 		- generate a PronunciationState and Token for this word and add it to the sortedTokenList
	 * 		- for each unit in the pronunciation
	 * 			- assert that the given unit labels match the units in the pronunciation
	 * 			- generate a UnitState and Token for this unit and add it to the sortedTokenList
	 * 			 
	 * @param tg given TextGrid
	 */
	public void loadTranscript(TextGrid tg) {
		Iterator<Label> wordIt = tg.getTierByName(wordTierName).iterator();
		ListIterator<Label> unitIt = tg.getTierByName(unitTierName).listIterator();
		sortedTokenList = new TokenList();
		Token t = null;
		int frameNumber = 0;
		while (wordIt.hasNext()) {
			Label wordLabel = wordIt.next();
			String spelling = wordLabel.getLabel();
			Word word = (spelling.equals("")) ? dictionary.getSilenceWord() : dictionary.getWord(spelling);
			if (word == null) {
				throw new RuntimeException("don't know pronunciation for " + wordLabel.getLabel());
			}
			Pronunciation pron = word.getMostLikelyPronunciation();
			SearchState searchState = new PronunciationState(wordLabel.getLabel(), pron, 0);
			frameNumber = (int) Math.round(wordLabel.getStart() * 100);
			if (t == null) {
				t = new Token(searchState, frameNumber);
			} else {
				t = t.child(searchState, 0.0f, 0.0f, 0.0f, frameNumber);
			}
			sortedTokenList.add(t);
			Unit[] units = pron.getUnits();
			for (Unit unit : units) {
				Label unitLabel = unitIt.next();
				assert unit.getName().equals(unitLabel.getLabel()) : "Problem with pronunciation of word " + wordLabel.toString();
				searchState = new UnitState(unit, null);
				frameNumber = (int) Math.round(unitLabel.getStart() * 100);
				t = t.child(searchState, 0.0f, 0.0f, 0.0f, frameNumber);
				sortedTokenList.add(t);
			}
		}
		unitIt.previous();
		frameNumber = (int) Math.round(unitIt.next().getEnd() * 100);
		sortedTokenList.add(t.child(null, 0.0f, 0.0f, 0.0f, frameNumber));
	}
	
	public void loadTranscript(String filename) {
		try {
			loadTranscript(TextGrid.newFromTextGridFile(filename));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void setTranscript(String filename) {
		transcriptName = filename;
	}

	public void newProperties(PropertySheet ps) throws PropertyException {
        fe = (FrontEnd) ps.getComponent(PROP_FRONTEND);
        dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);
        wordTierName = ps.getString(PROP_WORD_TIER);
        unitTierName = ps.getString(PROP_UNIT_TIER);
        transcriptName = ps.getString(PROP_TRANSCRIPT_FILE);
	}

	private class TokenList {
		// you have to make sure yourself, that the sortedList is sorted!
		LinkedList<Token> sortedList = new LinkedList<Token>();
		ListIterator<Token> listIt;
		
		void add(Token t) {
			listIt = null;
			sortedList.add(t);
		}
		
		/**
		 * @param frameNumber
		 * @return the last Token in the list that still equals the framenumber
		 */
		Token get(int frameNumber) {
			if (listIt == null) 
				listIt = sortedList.listIterator();
			while (listIt.hasNext() && listIt.next().getFrameNumber() <= frameNumber) {}
			return listIt.hasPrevious() ? listIt.previous() : null;
		}
	}
	
}
