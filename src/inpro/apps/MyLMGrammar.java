/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package inpro.apps;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Defines a simple grammar based upon a language model. It generates one {@link GrammarNode grammar node}per word. This
 * grammar can deal with unigram and bigram grammars of up to 1000 or so words. Note that all probabilities are in the
 * log math domain.
 */
public class MyLMGrammar extends Grammar {

    /** The property for the language model to be used by this grammar */
    @S4Component(type = LanguageModel.class)
    public final static String PROP_LANGUAGE_MODEL = "languageModel";
    @S4Component(type = LogMath.class)
	public final static String PROP_LOG_MATH = "logMath";
    protected GrammarNode finalNode;
    private final List<String> tokens = new ArrayList<String>();
    private boolean modelRepeats = false;
	private boolean modelSkips = false;
	private float wordRepeatProbability = 0.0f;
	private float wordSkipProbability = 0.0f;
	private int wordSkipRange;
	private LogMath logMath;
	private boolean languageModelIsLoaded=false;
    // ------------------------
    // Configuration data
    // ------------------------
    private LanguageModel languageModel;

    public MyLMGrammar(final LogMath logMath,LanguageModel languageModel, boolean showGrammar, boolean optimizeGrammar, boolean addSilenceWords, boolean addFillerWords, Dictionary dictionary) {
        super(showGrammar,optimizeGrammar,addSilenceWords,addFillerWords,dictionary);
        this.languageModel = languageModel;
        this.logMath = logMath;
        
    }

    public MyLMGrammar() {

    }
    
    public void setText(String text) {
		String[] words = text.split(" ");
		tokens.clear();
		for (String word : words) {
				tokens.add(word.toLowerCase());
		}
		
		System.out.println ("tokens size"+tokens.get(0).toString());
		
		
		try {
			createGrammar();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    
	/*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        languageModel = (LanguageModel) ps.getComponent(PROP_LANGUAGE_MODEL);
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH);
    }


    /**
     * Creates the grammar from the language model. This Grammar contains one word per grammar node. Each word (and
     * grammar node) is connected to all other words with the given probability
     *
     * @return the initial grammar node
     */
    @Override
    protected GrammarNode createGrammar() throws IOException {
    	
    	
        languageModel.allocate();
        TimerPool.getTimer(this,"LMGrammar.create").start();
        GrammarNode firstNode = null;
        if (languageModel.getMaxDepth() > 2) {
            System.out.println("Warning: LMGrammar  limited to bigrams");
        }
        List<GrammarNode> nodes = new ArrayList<GrammarNode>();
        Set<String> words = languageModel.getVocabulary();
        // create all of the word nodes
        for (String word : words) {
            GrammarNode node = createGrammarNode(word);
            if (node != null && !node.isEmpty()) {
                if (node.getWord().equals(
                        getDictionary().getSentenceStartWord())) {
                    firstNode = node;
                } else if (node.getWord().equals(
                        getDictionary().getSentenceEndWord())) {
                    node.setFinalNode(true);
                }
                nodes.add(node);
            }
        }
        if (firstNode == null) {
            throw new Error("No sentence start found in language model");
        }
        for (GrammarNode prevNode : nodes) {
            // don't add any branches out of the final node
            if (prevNode.isFinalNode()) {
                continue;
            }
            for (GrammarNode nextNode : nodes) {
                String prevWord = prevNode.getWord().getSpelling();
                String nextWord = nextNode.getWord().getSpelling();
                Word[] wordArray = {getDictionary().getWord(prevWord),
                        getDictionary().getWord(nextWord)};
                float logProbability = languageModel
                        .getProbability((new WordSequence(wordArray)));
                prevNode.add(nextNode, logProbability);
            }
        }
        
        
      
        
        TimerPool.getTimer(this,"LMGrammar.create").stop();
        languageModel.deallocate();
        
      
        if (tokens.size()>0) {
		logger.info("Creating Grammar");
		initialNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
		finalNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
		finalNode.setFinalNode(true);
		final GrammarNode branchNode = createGrammarNode(false);

		final List<GrammarNode> wordGrammarNodes = new ArrayList<GrammarNode>();
		final int end = tokens.size();

		logger.info("Creating Grammar nodes");
		for (final String word : tokens.subList(0, end)) {
			final GrammarNode wordNode = createGrammarNode(word.toLowerCase());
			wordGrammarNodes.add(wordNode);
		}
		
		
	
		
		
	
		
		GrammarArc [] transitions=null;
		if (nodes!=null) {
			
			List<GrammarNode> wordGrammarNodesclone = new ArrayList<GrammarNode>(wordGrammarNodes.size());
        	for(GrammarNode item: wordGrammarNodes)  wordGrammarNodesclone.add((GrammarNode) item);
			
        	
        	for (GrammarNode nextNode : nodes) {
	            	
	            	
	            		
	                
	                	if (nextNode.getWord().getSpelling().equals(wordGrammarNodesclone.get(0).getWord().getSpelling())){
	                		System.out.println ("Node"+nextNode.getWord().getSpelling());
	                		//if it it the last word we are finished
	                		if (wordGrammarNodes.size()==1){
	                			wordGrammarNodes.remove(0);
	                			wordGrammarNodes.add(nextNode);
	                			wordGrammarNodesclone.remove(0);
	                			break;
	                		}
	                		
	                		
	                		
	                	else {//get transitions
	                		System.out.println ("get transitions");
	                		transitions=nextNode.getSuccessors();
	                		break;
	                	}
	       }
        }         	
        	
        	//iterate over transitions and get transitions 
        	if (transitions!=null) {
        	while (wordGrammarNodesclone.size()!=0) {
        		GrammarNode transitionNode=null;
        		for (int i=0; i<transitions.length-1;i++){
            		if (transitions [i].getGrammarNode().getWord().getSpelling().equals(wordGrammarNodesclone.get(0).getWord().getSpelling())){
            			System.out.println ("transitions:"+transitions [i].getGrammarNode().getWord().getSpelling());
            			transitionNode=transitions [i].getGrammarNode();
            			transitions=transitionNode.getSuccessors();
            			
            		}
            	}
        		
        		wordGrammarNodesclone.remove(0);
        		if (wordGrammarNodesclone.size()==0){
        			wordGrammarNodes.remove(wordGrammarNodes.get(wordGrammarNodes.size()-1));
        			wordGrammarNodes.add(wordGrammarNodes.size(),transitionNode);
        			
        		}
        		
        		
        			
        		 
        	}
        	
        	}else {
        		wordGrammarNodes.add(firstNode);
        	}
	        
		}	
			
			//wordGrammarNodes.add(firstNode);
        	logger.info("Done creating grammar node");

			// now connect all the GrammarNodes together
			initialNode.add(branchNode, LogMath.getLogOne());

			createBaseGrammar(wordGrammarNodes, branchNode, finalNode);

			if (modelRepeats) {
				addForwardJumps(wordGrammarNodes, branchNode, finalNode);
			}
			if (modelSkips) {
				addBackwardJumps(wordGrammarNodes, branchNode, finalNode);
			}

			logger.info("Done making Grammar");	
			return initialNode;
		
        }else  {
        	return firstNode;
        }
			
			

	  	       
	       
        
        
    }
    
    private void addBackwardJumps(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode) {
		GrammarNode currNode;
		for (int i = 0; i < wordGrammarNodes.size(); i++) {
			currNode = wordGrammarNodes.get(i);
			for (int j = Math.max(i - wordSkipRange, 0); j < i; j++) {
				GrammarNode jumpToNode = wordGrammarNodes.get(j);
				currNode.add(jumpToNode,
						logMath.linearToLog(wordRepeatProbability));
			}
		}
	}

	private void addForwardJumps(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode) {
		GrammarNode currNode = branchNode;
		for (int i = -1; i < wordGrammarNodes.size(); i++) {
			if (i > -1) {
				currNode = wordGrammarNodes.get(i);
			}
			for (int j = i + 2; j < Math.min(wordGrammarNodes.size(), i
					+ wordSkipRange); j++) {
				GrammarNode jumpNode = wordGrammarNodes.get(j);
				currNode.add(jumpNode,
						logMath.linearToLog(wordSkipProbability));
			}
		}
		for (int i = wordGrammarNodes.size() - wordSkipRange; i < wordGrammarNodes
				.size() - 1; i++) {
			int j = wordGrammarNodes.size();
			currNode = wordGrammarNodes.get(i);
			currNode.add(
					finalNode,
					logMath.linearToLog(wordSkipProbability
							* Math.pow(Math.E, j - i)));
		}

	}

	private void createBaseGrammar(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode) {
		GrammarNode currNode = branchNode;
		ListIterator<GrammarNode> iter = wordGrammarNodes.listIterator();
		while (iter.hasNext()) {
			GrammarNode nextNode = iter.next();
			currNode.add(nextNode, LogMath.getLogOne());
			currNode = nextNode;
		}
		currNode.add(finalNode, LogMath.getLogOne());
	}
}
