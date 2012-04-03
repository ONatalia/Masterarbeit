/**
 * 
 */
package org.cocolab.inpro.irmrsc.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.cocolab.inpro.irmrsc.simplepcfg.Grammar;
import org.cocolab.inpro.irmrsc.simplepcfg.Production;
import org.cocolab.inpro.irmrsc.simplepcfg.Symbol;

/**
 * A simple robust incremental top down beam search parser.
 * 
 * Inspired by Brian Roark 2001 Ph.D. Thesis, Department of Cognitive and Linguistic Sciences, Brown University.
 * 
 * @author andreas
 */
public class SITDBSParser {
	
	// counters for evaluations statistics
	public static int cntExpansions = 0;
	public static int cntDegradations = 0;
	public static int cntPrunes = 0;
	public static int cntDerivations = 0;
	
	// capacity limit
	public static int maxCandidatesLimit = 1000;
	
	// robustness
	public static boolean beRobust = true;
	public static int maxRepairs = 2;
	public static double repairMalus = 0.05;
	public static int maxInsertions = 2;
	public static double insertionMalus = 0.02;
	public static int maxDeletions = 2;
	public static double deletionMalus = 0.01;

	public static final String fillerRuleAndTagName = "fill";
	public static final Symbol unknownTag = new Symbol("unknown");
	public static final Symbol endOfUtteranceTag = new Symbol("S!");

	/** the parser main internal data structure **/ 
	private PriorityQueue<CandidateAnalysis> mQueue;
	
	/** the grammar use for parsing **/
	private Grammar mGrammar;
	
	/** the base beam factor **/
	private double mBaseBeamFactor;
	
	static String logPrefix = "[P] ";
	static Logger logger;

	public SITDBSParser(Grammar grammar, double bbf) {
		mGrammar = grammar;
		mBaseBeamFactor = bbf;
		reset();
	}
	
	public SITDBSParser(Grammar grammar) {
		mGrammar = grammar;
		mBaseBeamFactor = 0.001;
		reset();
	}
	
	public SITDBSParser(Grammar g, double bbf, CandidateAnalysis startCA) {
		mQueue = new PriorityQueue<CandidateAnalysis>();
		CandidateAnalysis ca = new CandidateAnalysis(startCA);
		mQueue.add(ca);
	}

	/** copy constructor **/
	public SITDBSParser(SITDBSParser p) {
		if (p != null) {
			this.mGrammar = p.mGrammar;
			this.mBaseBeamFactor = p.mBaseBeamFactor;
			this.mQueue = new PriorityQueue<CandidateAnalysis>();
			for (CandidateAnalysis ca : p.mQueue) 
				mQueue.add(new CandidateAnalysis(ca));
		}
	}

	/** feeds the parser with the next input token **/
	public void feed(String nextToken) {
		this.feed(new Symbol(nextToken));
	}
	
	/** feeds the parser with the next input token **/
	public void feed(Symbol nextToken) {
		//logger.info(logPrefix+"feed: "+nextToken);
		
		if (mQueue.size() < 1) {
			// the queue is empty and no further token can be accepted
			return;
		}
		
		// make a new queue for the results of parsing the current token
		PriorityQueue<CandidateAnalysis> newQueue = new PriorityQueue<CandidateAnalysis>(2);
		
		// make a working-copy (deep) of the queue and prepare the next incremental step
		PriorityQueue<CandidateAnalysis> currentQueue = new PriorityQueue<CandidateAnalysis>(mQueue.size());
		for (CandidateAnalysis oldCA : mQueue) {
			CandidateAnalysis newCA = new CandidateAnalysis(oldCA);
			newCA.newIncrementalStep(oldCA);
			currentQueue.add(newCA);
		}
		
		// special case if we have a filler
		if (nextToken.getSymbol().equals(fillerRuleAndTagName)) {
			for (CandidateAnalysis ca : currentQueue) {
				ca.consumeFiller(fillerRuleAndTagName);
			}
			mQueue = currentQueue;
			// store how many analyses survived
			cntDerivations += mQueue.size();
			return;
		}
		
		// begin parsing
		while (true) {
			if (currentQueue.size() >= maxCandidatesLimit) {
				// max size reached; await next token
				//logger.debug(logPrefix+": max size reached");
				break;
			}
			CandidateAnalysis ca = currentQueue.poll();
			if (ca == null) {
				// no more candidate analysis on the queue; await next token
				//logger.debug(logPrefix+": no more ca on queue");
				break;
			}
			if (! newQueue.isEmpty()) {
				if (ca.getProbability() < (newQueue.peek().getProbability() * mBaseBeamFactor * newQueue.size())) {
					// best derivation below threshold; await next token
					//logger.debug(logPrefix+": outside beam");
					cntPrunes++;
					break;
				} else {
					//logger.debug(String.format(logPrefix+": %1$9.4g >= %2$9.4g (n=%3$d).", ca.getProbability(), (newQueue.peek().getProbability() * mBaseBeamFactor * newQueue.size()), newQueue.size()));
					//logger.debug(logPrefix+": "+ca.getProbability()+" >= ("+newQueue.peek().getProbability()+" * "+mBaseBeamFactor+" * "+newQueue.size()+").");
				}
			}
			if (ca.isComplete()) {
				// stack is empty i.e. no more material expected, although we have another token;
				// drop this analysis and continue with next one
				//logger.debug(logPrefix+": completed too early");
				continue;
			}
			Symbol topSymbol = ca.getTopSymbol();
			if (mGrammar.isTerminalSymbol(topSymbol)) {
				// find all matching derivations and push them on the newQueue
				if (topSymbol.equals(nextToken)) {
					newQueue.add(ca.match(nextToken));
				} else {
					// the next token is an unknown tag.
					if (beRobust && (! ca.hasRobustOperationsLately()) && nextToken.equals(unknownTag) && (! topSymbol.equals(endOfUtteranceTag)) && (ca.getNumberOfRepairs() < maxRepairs)) {
						// make a repair hypothesis where the unknown tag is the currently required one.
						CandidateAnalysis newca = ca.repair(topSymbol);
						newca.degradeProbability(repairMalus);
						newQueue.add(newca);
					}
					if (beRobust && (! ca.hasRobustOperationsLately()) && (! topSymbol.equals(endOfUtteranceTag)) && (! nextToken.equals(endOfUtteranceTag)) && (ca.getNumberOfInsertions() < maxInsertions)) {
						// make an insertion hypothesis, where the current symbol is just a simple insertion
						CandidateAnalysis newca = ca.insert(nextToken);
						newca.degradeProbability(insertionMalus);
						newQueue.add(newca);
					}
					if (beRobust && (! ca.hasRobustOperationsLately()) && (! topSymbol.equals(endOfUtteranceTag)) && (ca.getNumberOfDeletions() < maxDeletions)) {
						// make an deletion hypothesis, where the required tag is deleted and thus filled in underspecified
						CandidateAnalysis newca = ca.deletion(topSymbol);
						newca.degradeProbability(deletionMalus);
						currentQueue.add(newca);
					}

				}
			} else {
				// find all expanding derivations and push them on the old Queue
				List<String> relevantProductions = mGrammar.getProductionsExpandingSymbol(topSymbol); //TODO: here: think about how add insertions support
				// TODO: use left corner relation here: relrule = g.getLCsatisfyingProdExpSym(topSym, nextToken)
				for (String id : relevantProductions) {
					cntExpansions++;
					Production p = mGrammar.getProduction(id);
					currentQueue.add(ca.expand(p));
				}
			}
		}
		// store how many analyses will be pruned now
		cntPrunes += currentQueue.size();
		// make newQueue the new mQueue and return the number of analyses in it
		mQueue = newQueue;
		// store how many analyses survived
		cntDerivations += mQueue.size();
	}
	
	
	/** returns the number of analyses that are completable, i.e.
	 * that have no symbols or only eliminable symbols on their stack **/
	public int getNumberOfCompletableAnalyses () {
		int cnt = 0;
		for (CandidateAnalysis ca : mQueue) {
			// check if already complete
			if (ca.isComplete()) {
				cnt++;
				continue;
			}
			// check if completable, i.e. all remaining symbols on the stack are eliminable
			Deque<Symbol> stack = ca.getStack();
			boolean caIsCompletable = true;
			for (Symbol sym : stack) {
				if (! mGrammar.isEliminable(sym)) {
					caIsCompletable = false;
					break;
				}
			}
			if (caIsCompletable) {
				cnt++;
			}
		}
		return cnt;
	}
	
// obsolete code.
//	public boolean complete() {
//		for (CandidateAnalysis ca : mQueue) {
//			// check if already complete
//			if (ca.isComplete()) {
//				continue;
//			}
//			// check if completable
//			Deque<Symbol> stack = ca.getStack();
//			boolean caIsCompletable = true;
//			for (Symbol sym : stack) {
//				if (! mGrammar.isEliminable(sym)) {
//					caIsCompletable = false;
//					break;
//				}
//			}
//			// complete all completable
//			if (caIsCompletable) {}
//		}
//		return true;
//	}
	
	/** returns the parsers queue **/
	public PriorityQueue<CandidateAnalysis> getQueue() {
		return this.mQueue;
	}
	
	/** degrades the probability of a given CandidateAnalysis by a given malus **/
	public CandidateAnalysis degradeAnalysis (CandidateAnalysis ca, double malus) {
		// remove the ca from the queue, degrade it and add it again.
		if (this.mQueue.remove(ca)) {
			cntDegradations++;
			ca.degradeProbability(malus);
			mQueue.add(ca);
			//logger.debug(logPrefix+" CA found, degraded and readded.");
			return ca;
		} else {
			//logger.debug(logPrefix+" CA not found!");
			return null;
		}
	}

	/** resets the parsers internal queue to initial state **/
	public void reset() {
		mQueue = new PriorityQueue<CandidateAnalysis>(5);
		Deque<Symbol> s = new ArrayDeque<Symbol>();
		s.push(mGrammar.getEnd());
		s.push(mGrammar.getStart());
		mQueue.add(new CandidateAnalysis(s));
	}

	/** prints all derivations in the queue, for debugging **/
	public void info() {
		for (CandidateAnalysis ca : mQueue) {
			logger.debug(logPrefix+ca);
			logger.debug(logPrefix+ca.printDerivation());
		}
	}
	
	/** prints some useful information **/
	public void status() {
		logger.info(logPrefix+"status: remains="+mQueue.size()
				+" completable="+ getNumberOfCompletableAnalyses()
				+" expansions="+cntExpansions
				+" degradations="+cntDegradations
				+" prunes="+cntPrunes
				+" derivations="+cntDerivations);
	}
	
	public void setLogger(Logger l) {
		logger = l;
	}

	/** sets whether the parser is allowed to use robust operations or not **/
	public static void setRobust(boolean v) {
		beRobust = v;
	}
	
}
