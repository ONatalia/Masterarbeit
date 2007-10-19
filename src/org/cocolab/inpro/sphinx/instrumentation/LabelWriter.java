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
import java.util.List;

import org.cocolab.inpro.batch.BatchModeRecognizer;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.flat.ExtendedUnitState;
import edu.cmu.sphinx.linguist.flat.PronunciationState;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
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
public class LabelWriter
        implements
            Configurable,
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
    public final static String PROP_FILE_OUTPUT = "fileOutput";
    public final static String PROP_FILE_BASE_NAME = "fileBaseName";
    public final static String PROP_STEP_WIDTH = "step";
    
    public final static String PROP_WORD_ALIGNMENT = "wordAlignment";
    public final static String PROP_PHONE_ALIGNMENT = "phoneAlignment";
    
    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    
    private boolean intermediateResults = false;
    private boolean finalResult = true;
    
    private boolean fileOutput = false;
    private String fileBaseName = "";

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
        registry.register(PROP_FILE_OUTPUT, PropertyType.BOOLEAN);
        registry.register(PROP_FILE_BASE_NAME, PropertyType.STRING);
        registry.register(PROP_STEP_WIDTH, PropertyType.INT);
        registry.register(PROP_WORD_ALIGNMENT, PropertyType.BOOLEAN);
        registry.register(PROP_PHONE_ALIGNMENT, PropertyType.BOOLEAN);
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
        
        wordAlignment = ps.getBoolean(PROP_WORD_ALIGNMENT, false);
        phoneAlignment = ps.getBoolean(PROP_PHONE_ALIGNMENT, false);

        if (!fileOutput) {
        	if (wordAlignment) {
        		wordAlignmentStream = System.out;
        	}
        	if (phoneAlignment) {
        		phoneAlignmentStream = System.out;
        	}
        }
        stepWidth = ps.getInt(PROP_STEP_WIDTH, 1);
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
			String filename = fileBaseName;
			if (batchProcessor != null) {
				BatchItem bi = batchProcessor.getCurrentBatchItem();
				if (bi != null) {
					filename = bi.getFilename();
					filename = filename.replaceAll(".wav", "");
				}
				else {
					filename = null;
				}
			}
			// if old and new filenames are different
			if (((lastFileName == null) && (filename != null)) 
					|| !lastFileName.equals(filename)) {
				filename += ("." + extension);
				try {
					output = new PrintStream(filename); 
				} catch (FileNotFoundException e) {
					System.err.println("Unable to open file " + filename);
					output = System.out;
				}
			}
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
    
    
    /**
     * get the word-tokens on the best path
     *
     * @param result the result to analyse
     * @return List of word tokens on the best path
     */
    private List<Token> getBestWordTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
    	Token token = result.getBestToken();
		// recover the path of visited word- and silence-tokens in the best token
		while (token != null) {
			SearchState searchState = token.getSearchState(); 
            if ((searchState instanceof PronunciationState) // each word starts with a PronunciationState
            || ((searchState instanceof ExtendedUnitState) // each pause starts with a unit that is a filler 
//            	&& ((ExtendedUnitState) searchState).getUnit().isFiller())) 
            	&& list.isEmpty())) {
            	// add these tokens to the list
        	    list.add(token);
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
    private List<Token> getBestPhoneTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
    	Token token = result.getBestToken();
		// recover the path of visited word- and silence-tokens in the best token
    	list.add(token);
    	while (token != null) {
			SearchState searchState = token.getSearchState(); 
            if (searchState instanceof ExtendedUnitState) {
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
    private List<Token> getAllBestTokens(Result result) {
		List<Token> list = new ArrayList<Token>();
    	Token token = result.getBestToken();
		// recover the path of visited word- and silence-tokens in the best token
		while (token != null) {
    	    list.add(token);
            token = token.getPredecessor();
		}
		return list;
    }
    
    /**
     * convert a token list to an alignment string 
     * 
     * @param list list of tokens
     * @return 
     */
    private String tokenListToAlignment(List<Token> list) {
		StringBuffer sb = new StringBuffer(); 
		
		// iterate over the list and print the associated times
        for (int i = list.size() - 1; i > 0; i--) {
            Token token = list.get(i);
            Token nextToken = list.get(i - 1);
            sb.append(token.getFrameNumber() / 100.0); // a frame always lasts 10ms (FIXME, better be configurable)
            sb.append("\t");
            sb.append(nextToken.getFrameNumber() / 100.0); // dito
            sb.append("\t");
            // depending on whether word, filler or other, dump the string-representation
            SearchState state = token.getSearchState();
            if (state instanceof PronunciationState) {
            	PronunciationState pronunciationState = (PronunciationState) state;
            	sb.append(pronunciationState.getPronunciation().getWord().toString());
            }
            else if (state instanceof ExtendedUnitState) {
            	ExtendedUnitState unitState = (ExtendedUnitState) state;
            	sb.append(unitState.getUnit().getName());           		
            }
            else {
            	sb.append(state.toString());
            }
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
    private String writeAlignment(List<Token> list, PrintStream stream, String lastAlignment, boolean timestamp) {
    	String alignment = tokenListToAlignment(list);
    	if (!alignment.equals(lastAlignment)) {
    		if (timestamp) {
    			stream.print("Time: ");
    			stream.println(step / 100.0);
    		}
    		stream.println(alignment);
    		lastAlignment = alignment;
    	}
    	return alignment;
    }
    
    private void messageZeitGeist(List<Token> list, PrintStream stream, String lastAlignment, boolean timestamp) {
    	// TODO: voilà, hier soll der Zeitgeist benachrichtigt werden.
    	// er müsste jeweils da aufgerufen werden, wo zur Zeit writeAlignment() aufgerufen wird
    }
    
    private void dumpAllStates(Result result) {
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
    			lastWordAlignment = writeAlignment(list, wordAlignmentStream, lastWordAlignment, timestamp);
    		}
    		if (phoneAlignment) {
    			List<Token> list = getBestPhoneTokens(result);
    			if (newfile) {
    				if ((phoneAlignmentStream != null) && fileOutput) { phoneAlignmentStream.close(); }
    				phoneAlignmentStream = setStream("phonealignment");
    			}
    			lastPhoneAlignment = writeAlignment(list, phoneAlignmentStream, lastPhoneAlignment, timestamp);
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
