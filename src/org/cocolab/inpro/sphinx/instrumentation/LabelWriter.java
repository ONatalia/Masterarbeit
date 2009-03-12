/*
 * 
 * Copyright 2007 InPro Project, Timo Baumann  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package org.cocolab.inpro.sphinx.instrumentation;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * inspects the current results and writes the phone-alignment to stdout or to a file
 */
public class LabelWriter implements Configurable,
						            ResultListener,
						            Resetable,
						            StateListener {
    /**
     * A Sphinx property that defines which recognizer to monitor
     */
    public final static String PROP_RECOGNIZER = "recognizer";
    public final static String PROP_INTERMEDIATE_RESULTS = "intermediateResults";
    public final static String PROP_FINAL_RESULT = "finalResult";
    public final static String PROP_STEP_WIDTH = "step";
    
    public final static String PROP_WORD_ALIGNMENT = "wordAlignment";
    public final static String PROP_PHONE_ALIGNMENT = "phoneAlignment";
    
    public final static String PROP_FILE_OUTPUT = "fileOutput";
    public final static String PROP_FILE_BASE_NAME = "fileBaseName";
    
    public final static String PROP_N_BEST = "nBest";

    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    
    protected boolean intermediateResults = false;
    protected boolean finalResult = true;
    
    private boolean fileOutput = false;
    private String fileBaseName = "";
    
    private int nBest = 1;
    
    protected boolean wordAlignment = true;
    protected boolean phoneAlignment = true;
    
    private String lastWordAlignment = "";
    private String lastPhoneAlignment = "";
    
    private PrintStream wordAlignmentStream;
    private PrintStream phoneAlignmentStream;

    /**
     * counts the number of recognition steps
     */
    protected int step = 0;
    protected int stepWidth = 1;
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_INTERMEDIATE_RESULTS, PropertyType.BOOLEAN);
        registry.register(PROP_FINAL_RESULT, PropertyType.BOOLEAN);
        registry.register(PROP_STEP_WIDTH, PropertyType.INT);
        registry.register(PROP_WORD_ALIGNMENT, PropertyType.BOOLEAN);
        registry.register(PROP_PHONE_ALIGNMENT, PropertyType.BOOLEAN);
        registry.register(PROP_FILE_OUTPUT, PropertyType.BOOLEAN);
        registry.register(PROP_FILE_BASE_NAME, PropertyType.STRING);
        registry.register(PROP_N_BEST, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                Recognizer.class);
        
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        }
        
        intermediateResults = ps.getBoolean(PROP_INTERMEDIATE_RESULTS, false);
        finalResult = ps.getBoolean(PROP_FINAL_RESULT, true);
        
        fileOutput = ps.getBoolean(PROP_FILE_OUTPUT, false);
        fileBaseName = ps.getString(PROP_FILE_BASE_NAME, "");
        
        nBest = ps.getInt(PROP_N_BEST, 1);
        
        stepWidth = ps.getInt(PROP_STEP_WIDTH, 1);

        wordAlignment = ps.getBoolean(PROP_WORD_ALIGNMENT, false);
        phoneAlignment = ps.getBoolean(PROP_PHONE_ALIGNMENT, false);

    	if (wordAlignment) {
    		wordAlignmentStream = setStream("wordalignment");
    	}
    	if (phoneAlignment) {
    		phoneAlignmentStream = setStream("phonealignment");
    	}
    }


    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.instrumentation.Resetable
     */
    public void reset() {
    	step = 0;
    	lastWordAlignment = "";
    	lastPhoneAlignment = "";
    }
    
    private PrintStream setStream(String extension) {
    	PrintStream output = null;
    	if (fileOutput) {
			String filename = fileBaseName + "." + extension;
			try {
				output = new PrintStream(filename); 
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open file " + filename);
				output = System.out;
			}
    	} else {
    		output = System.out;
    	}
    	return output;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
    
    Token lastBestToken = new Token(null, 0);
    
    /**
     * get the word-tokens on the best path
     *
     * @param result the result to analyse
     * @return List of word tokens on the best path
     */
    public List<Token> getBestWordTokens(Token token) {
		List<Token> list = new ArrayList<Token>();
    	while (token != null) {
			SearchState searchState = token.getSearchState(); 
            if ((searchState instanceof WordSearchState) // each word starts with a PronunciationState
            || ((searchState instanceof UnitSearchState) // each pause starts with a unit that is a filler 
//            	&& ((ExtendedUnitState) searchState).getUnit().isFiller())) 
            	&& list.isEmpty())) {
            	// add these tokens to the list
        	    list.add(0, token);
            }
            token = token.getPredecessor();
        }
		return list;
    }
    
    /**
     * get the phone-tokens on the best path
     *
     * @param result the result to analyse
     * @return List of phone tokens on the best path
     */
    public static List<Token> getBestPhoneTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
    	Token token = result.getBestToken();
		// recover the visited segmental tokens in the best path
    	list.add(token);
    	while (token != null) {
			SearchState searchState = token.getSearchState(); 
            if (searchState instanceof UnitSearchState) {
            	// add these tokens to the list
        	    list.add(token);
            }
            token = token.getPredecessor();
        }
    	return list;
    }
    
    
    /**
     * get all tokens on the best path
     *
     * @param result the result to analyse
     * @return List of all tokens on the best path
     */
    public static List<Token> getAllBestTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
    	Token token = result.getBestToken();
		// recover the path of visited word- and silence-tokens in the best token
		while (token != null) {
    	    list.add(token);
            token = token.getPredecessor();
		}
		return list;
    }
    
    public static String stringForSearchState(SearchState state) {
        String event;
        if (state instanceof WordSearchState) {
        	WordSearchState pronunciationState = (WordSearchState) state;
        	event = pronunciationState.getPronunciation().getWord().toString();
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
    
    /**
     * convert a token list to an alignment string 
     * 
     * @param list list of tokens
     * @return 
     */
    public static String tokenListToAlignment(List<Token> list) {
		StringBuffer sb = new StringBuffer(); 
		if (list.size() > 0) {
			// iterate over the list and print the associated times
			Token lastToken = list.get(0);
	        for (int i = 1; i < list.size() - 1; i++) {
	            Token token = list.get(i);
	            sb.append(lastToken.getFrameNumber() / 100.0); // a frame always lasts 10ms 
	            sb.append("\t");
	            sb.append(token.getFrameNumber() / 100.0); // dito
	            sb.append("\t");
	            // depending on whether word, filler or other, dump the string-representation
	            SearchState state = token.getSearchState();
	            sb.append(stringForSearchState(state)); 
	            sb.append("\n");
	            lastToken = token;
	        }
		}
        return sb.toString();
    }

    /**
     * write labels of aligned segments or words  
     * to stream if it differs from lastAlignment
     * @param list
     * @param stream
     * @param lastAlignment
     * @param timestamp
     * @return
     */
    private String writeAlignment(List<Token> list, PrintStream stream, String lastAlignment, boolean timestamp) {
    	String alignment = tokenListToAlignment(list);
		if (timestamp) {
			stream.print("Time: ");
			stream.println(step / 100.0);
		}
		if (alignment.equals("")) {
			stream.println("0.0\t" + step / 100.0 + "\t<sil>\n");
		} else {
			stream.println(alignment);
		}
		lastAlignment = alignment;
    	return alignment;
    }

    public void dumpAllStates(Result result) {
    	List<Token> list = getAllBestTokens(result);
		// iterate over the list and dump the tokens
        for (int i = list.size() - 1; i >= 0; i--) {
            Token token = list.get(i);
        	System.out.println(token.toString());
        }
    }
    
    /*
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    @SuppressWarnings("unchecked")
	public void newResult(Result result) {
    	if ((intermediateResults == !result.isFinal()) 
    	|| (finalResult && result.isFinal())) {
    		boolean timestamp = !result.isFinal();
    		List<Token> nBestList = result.getResultTokens();
    		if (nBestList.size() < 0) {
    			System.out.println("# reverting to active tokens...");
    			nBestList = result.getActiveTokens().getTokens();
    		}
    		Collections.sort(nBestList, Token.COMPARATOR);
    		if (nBestList.size() > nBest) {
    			nBestList = nBestList.subList(0, nBest);
    		}
    		Iterator<Token> nBestIt = nBestList.iterator();
    		while (nBestIt.hasNext()) {
    			Token nBestToken = nBestIt.next();
	    		if (wordAlignment) {
	    			List<Token> wordTokenList = getBestWordTokens(nBestToken);
	    			lastWordAlignment = writeAlignment(wordTokenList, wordAlignmentStream, lastWordAlignment, timestamp);
	    		}
	    		if (phoneAlignment) {
	    			List<Token> phoneTokenList = getBestPhoneTokens(result);
	    			lastPhoneAlignment = writeAlignment(phoneTokenList, phoneAlignmentStream, lastPhoneAlignment, timestamp);
	    		}
    		}
    	}
    	step += stepWidth;
    }

    public void statusChanged(RecognizerState status) {
		if (status == RecognizerState.RECOGNIZING) {
			step = 0;
		}
    }
    
}
