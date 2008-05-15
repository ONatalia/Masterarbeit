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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.batch.BatchModeRecognizer;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.BatchItem;
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
    public final static String PROP_BATCH_PROCESSOR = "batchProcessor";
    public final static String PROP_STEP_WIDTH = "step";
    
    public final static String PROP_WORD_ALIGNMENT = "wordAlignment";
    public final static String PROP_PHONE_ALIGNMENT = "phoneAlignment";
    
    public final static String PROP_FILE_OUTPUT = "fileOutput";
    public final static String PROP_FILE_BASE_NAME = "fileBaseName";

    public final static String PROP_ZEITGEIST_OUTPUT = "zeitgeistOutput";
    public final static String PROP_ZEITGEIST_PORT = "zeitgeistPort";
    
    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    
    private boolean intermediateResults = false;
    private boolean finalResult = true;
    
    private boolean fileOutput = false;
    private String fileBaseName = "";
    
    private boolean zeitgeistOutput = false;
    private int zeitgeistPort = 2000;
    
    // when in batch mode, get filenames from the batch processor
    private BatchModeRecognizer batchProcessor;
    
    private boolean wordAlignment = true;
    private boolean phoneAlignment = true;
    
    private String lastWordAlignment = "";
    private String lastPhoneAlignment = "";
    
    private String lastFileName = null;
    
    private PrintStream wordAlignmentStream;
    private PrintStream phoneAlignmentStream;

    /**
     * counts the number of recognition steps
     */
    private int step = 0;
    private int stepWidth = 1;
    
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
        registry.register(PROP_BATCH_PROCESSOR, PropertyType.COMPONENT);
        registry.register(PROP_STEP_WIDTH, PropertyType.INT);
        registry.register(PROP_WORD_ALIGNMENT, PropertyType.BOOLEAN);
        registry.register(PROP_PHONE_ALIGNMENT, PropertyType.BOOLEAN);
        registry.register(PROP_FILE_OUTPUT, PropertyType.BOOLEAN);
        registry.register(PROP_FILE_BASE_NAME, PropertyType.STRING);
        registry.register(PROP_ZEITGEIST_OUTPUT, PropertyType.BOOLEAN);
        registry.register(PROP_ZEITGEIST_PORT, PropertyType.INT);
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
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
        }
        
        intermediateResults = ps.getBoolean(PROP_INTERMEDIATE_RESULTS, false);
        finalResult = ps.getBoolean(PROP_FINAL_RESULT, true);
        
        fileOutput = ps.getBoolean(PROP_FILE_OUTPUT, false);
        fileBaseName = ps.getString(PROP_FILE_BASE_NAME, "");
        try {
        	batchProcessor = (BatchModeRecognizer) ps.getComponent(PROP_BATCH_PROCESSOR, BatchModeRecognizer.class);
        } catch (PropertyException e) {
        	batchProcessor = null;
        }
        
        stepWidth = ps.getInt(PROP_STEP_WIDTH, 1);

        wordAlignment = ps.getBoolean(PROP_WORD_ALIGNMENT, false);
        phoneAlignment = ps.getBoolean(PROP_PHONE_ALIGNMENT, false);

    	if (wordAlignment) {
    		wordAlignmentStream = setStream("wordalignment");
    	}
    	if (phoneAlignment) {
    		phoneAlignmentStream = setStream("phonealignment");
    	}
        
        zeitgeistOutput = ps.getBoolean(PROP_ZEITGEIST_OUTPUT, false);
        zeitgeistPort = ps.getInt(PROP_ZEITGEIST_PORT, 2000);
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
			String filename = null;
			if (batchProcessor != null) {
				BatchItem bi = batchProcessor.getCurrentBatchItem();
				if (bi != null) {
					filename = bi.getFilename();
					filename = filename.replaceAll(".wav", "");
				}
				else {
					filename = null;
				}
			} else {
				filename = fileBaseName;
			}
			// if old and new filenames are different
//			if (((lastFileName == null) && (filename != null)) || !lastFileName.equals(filename)) {
				filename += ("." + extension);
				try {
					output = new PrintStream(filename); 
				} catch (FileNotFoundException e) {
					System.err.println("Unable to open file " + filename);
					output = System.out;
				}
//			}
    	}
    	else {
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
    public List<Token> getBestWordTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
// <<<<<<< .mine
/*    	Token currentToken = result.getBestToken();
    	Token runningToken = currentToken;
    	while ((lastBestToken != runningToken) && (runningToken != null)) {
    		runningToken = runningToken.getPredecessor();
    	}
    	if (runningToken == null) {
    		System.err.println("new best token");
    	} else if (runningToken == lastBestToken) {
    		System.err.println("new token " + currentToken.toString() + " is extension to token " + lastBestToken.toString());
    	} else { assert (false); }
    	if (currentToken != null) { lastBestToken = currentToken; }
    	assert (lastBestToken != null); */
    	ActiveList al = result.getActiveTokens();
    	System.err.println("Printing " + al.size() + " results: ");
    	Iterator alIter = al.iterator();
		// recover the path of visited word- and silence-tokens in the best token 
		int i = 0;
		Token token;
		while ((alIter.hasNext()) && i <= 5) {
		i++;
		System.err.println("next best hypothesis");
		token = (Token) alIter.next();
    	while (token != null) {
// =======
    	token = result.getBestToken();
		// recover the path of visited word- and silence-tokens in the best token
    	if (token != null) { 
    		System.out.println(token.getSearchState());
    	}
    	while (token != null) {
// >>>>>>> .r408
			SearchState searchState = token.getSearchState(); 
            if ((searchState instanceof WordSearchState) // each word starts with a PronunciationState
            || ((searchState instanceof UnitSearchState) // each pause starts with a unit that is a filler 
//            	&& ((ExtendedUnitState) searchState).getUnit().isFiller())) 
            	&& list.isEmpty())) {
            	// add these tokens to the list
        	    list.add(token);
            }
            token = token.getPredecessor();
        }
	}} /**/
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
		
		// iterate over the list and print the associated times
        for (int i = list.size() - 1; i > 0; i--) {
            Token token = list.get(i);
            Token nextToken = list.get(i - 1);
            sb.append(token.getFrameNumber() / 100.0); // a frame always lasts 10ms 
            sb.append("\t");
            sb.append(nextToken.getFrameNumber() / 100.0); // dito
            sb.append("\t");
            // depending on whether word, filler or other, dump the string-representation
            SearchState state = token.getSearchState();
            sb.append(stringForSearchState(state)); 
            sb.append("\n");
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
    private String writeAlignment(List<Token> list, PrintStream stream, String lastAlignment, boolean timestamp, String origin) {
    	String alignment = tokenListToAlignment(list);
    	if (!alignment.equals(lastAlignment)) {
    		if (timestamp) {
    			stream.print("Time: ");
    			stream.println(step / 100.0);
    		}
    		stream.println(alignment);
    		lastAlignment = alignment;
    		if (zeitgeistOutput) {
    			messageZeitGeist(list, origin);
    		}
    	}
    	return alignment;
    }
    
    private void messageZeitGeist(List<Token> list, String origin) {
    	// er müsste jeweils da aufgerufen werden, wo zur Zeit writeAlignment() aufgerufen wird
    	StringBuffer sb = new StringBuffer();
//    	sb.append("<dialogue id='test-01'>");
    	sb.append("<event time='");
    	sb.append(step * 10);
    	sb.append("' originator='");
    	sb.append(origin);
    	sb.append("'>");
    	
		// iterate over the list and print the associated times
        for (int i = list.size() - 1; i > 0; i--) {
            Token token = list.get(i);
            Token nextToken = list.get(i - 1);
            sb.append("<event time='");
            sb.append(token.getFrameNumber() * 10);
            sb.append("' duration='");
            sb.append((nextToken.getFrameNumber() - token.getFrameNumber()) * 10);
            sb.append("'>");
            // depending on whether word, filler or other, dump the string-representation
            SearchState state = token.getSearchState();
            String event = stringForSearchState(state);
            sb.append(event.replace("<", " ").replace(">", " "));
            sb.append("</event>");
    	}    	
    	sb.append("</event>");
//    	sb.append("</dialogue>");
		try {
			Socket sock = new Socket("localhost", zeitgeistPort);
			PrintWriter writer = new PrintWriter(sock.getOutputStream());
	    	writer.print(sb.toString());
	    	writer.close();
	    	sock.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not open connection to zeitgeist (no further attempts will be made)!");
			zeitgeistOutput = false;
		} 
    }    

    public void dumpAllStates(Result result) {
    	List<Token> list = getAllBestTokens(result);
		// iterate over the list and dump the tokens
        for (int i = list.size() - 1; i >= 0; i--) {
            Token token = list.get(i);
        	System.out.println(token.toString());
        }
    }
    
    private boolean newFile() {
    	String filename = null;
    	if (batchProcessor != null) {
    		filename = batchProcessor.getCurrentBatchItem().getFilename();
    	}
    	if ((filename != null) && !filename.equals(lastFileName)) {
    		lastFileName = filename;
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    public void newResult(Result result) {
    	if ((intermediateResults == !result.isFinal()) 
    	|| (finalResult && result.isFinal())) {
    		boolean newfile = newFile();
    		if (newfile) {
    			step = 0;
    		}
    		boolean timestamp = !result.isFinal();
    		if (wordAlignment) {
    			List<Token> list = getBestWordTokens(result);

    			if (newfile) {
    				if ((wordAlignmentStream != null) && fileOutput) { wordAlignmentStream.close(); }
    				wordAlignmentStream = setStream("wordalignment");
    			}
    			lastWordAlignment = writeAlignment(list, wordAlignmentStream, lastWordAlignment, timestamp, "asr_words");
    		}
    		if (phoneAlignment) {
    			List<Token> list = getBestPhoneTokens(result);
    			if (newfile) {
    				if ((phoneAlignmentStream != null) && fileOutput) { phoneAlignmentStream.close(); }
    				phoneAlignmentStream = setStream("phonealignment");
    			}
    			lastPhoneAlignment = writeAlignment(list, phoneAlignmentStream, lastPhoneAlignment, timestamp, "asr_phones");
    		}
    		
    	}
    	step += stepWidth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.recognizer.StateListener#statusChanged(edu.cmu.sphinx.recognizer.RecognizerState)
     */
    public void statusChanged(RecognizerState status) {

    }
}
