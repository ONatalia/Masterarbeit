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
 * @author andreas
 * Simple incremental top down beam search parser
 */
public class SITDBSParser {
	
	// counters for evaluations statistics
	public static int cntExpansions = 0;
	public static int cntDegradations = 0;
	
	
	public static final int maxCandidatesLimit = 1000;
	public static final int maxDeletions = 2;
	public static final int maxInsertion = 4;
	public static final String fillerRuleAndTagName = "fill";
	//public static final String EOSMarker = "</S>";

	private PriorityQueue<CandidateAnalysis> mQueue;
	private Grammar mGrammar;
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

	public SITDBSParser(SITDBSParser p) {
		if (p != null) {
			this.mGrammar = p.mGrammar;
			this.mBaseBeamFactor = p.mBaseBeamFactor;
			this.mQueue = new PriorityQueue<CandidateAnalysis>();
			for (CandidateAnalysis ca : p.mQueue) 
				mQueue.add(new CandidateAnalysis(ca));
		}
	}

	public void feed(String nextToken) {
		this.feed(new Symbol(nextToken));
	}
	
	public void feed(Symbol nextToken) {
		logger.debug(logPrefix+"feed: "+nextToken);
		
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
			return;
		}
		
		// begin parsing
		while (true) {
			if (currentQueue.size() >= maxCandidatesLimit) {
				// max size reached; await next token
				logger.debug(logPrefix+": max size reached");
				break;
			}
			CandidateAnalysis ca = currentQueue.poll();
			if (ca == null) {
				// no more candidate analysis on the queue; await next token
				logger.debug(logPrefix+": no more ca on queue");
				break;
			}
			if (! newQueue.isEmpty()) {
				if (ca.getProbability() < (newQueue.peek().getProbability() * mBaseBeamFactor * newQueue.size())) {
					// best derivation below threshold; await next token
					logger.debug(logPrefix+": outside beam");
					break;
				} else {
					logger.debug(String.format(logPrefix+": %1$9.4g >= %2$9.4g (n=%3$d).", ca.getProbability(), (newQueue.peek().getProbability() * mBaseBeamFactor * newQueue.size()), newQueue.size()));
					//logger.debug(logPrefix+": "+ca.getProbability()+" >= ("+newQueue.peek().getProbability()+" * "+mBaseBeamFactor+" * "+newQueue.size()+").");
				}
			}
			if (ca.isComplete()) {
				// stack is empty i.e. no more material expected, although we have another token;
				// drop this analysis and continue with next one
				logger.debug(logPrefix+": completed too early");
				continue;
			}
			Symbol topSymbol = ca.getTopSymbol();
			if (mGrammar.isTerminalSymbol(topSymbol)) {
				// find all matching derivations and push them on the newQueue
				if (topSymbol.equals(nextToken)) {
					newQueue.add(ca.match(nextToken));
				} else {
//					//TODO: else handle deletions by assuming underspec token and downrating the derivation
//					if (ca.getNumberOfDeletions() < maxDeletions) {
//						mQueue.add(ca.deletion(nextToken));
//					}
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
		// make newQueue the new mQueue and return the number of analyses in it
		mQueue = newQueue;
	}
	
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
	
	public boolean complete() {
		for (CandidateAnalysis ca : mQueue) {
			// check if already complete
			if (ca.isComplete()) {
				continue;
			}
			// check if completable
			Deque<Symbol> stack = ca.getStack();
			boolean caIsCompletable = true;
			for (Symbol sym : stack) {
				if (! mGrammar.isEliminable(sym)) {
					caIsCompletable = false;
					break;
				}
			}
			// complete all completable
			if (caIsCompletable) {}
		}
		return true;
	}
	
	public PriorityQueue<CandidateAnalysis> getQueue() {
		return this.mQueue;
	}
	
	public CandidateAnalysis degradeAnalysis (CandidateAnalysis ca, double malus) {
		// remove the ca from the queue, degrade it and add it again.
		if (this.mQueue.remove(ca)) {
			cntDegradations++;
			ca.degradeProbability(malus);
			mQueue.add(ca);
			logger.debug(logPrefix+" CA found, degraded and readded.");
			return ca;
		} else {
			logger.debug(logPrefix+" CA not found!");
			return null;
		}
	}

	public void reset() {
		mQueue = new PriorityQueue<CandidateAnalysis>(5);
		Deque<Symbol> s = new ArrayDeque<Symbol>();
		s.push(mGrammar.getEnd());
		s.push(mGrammar.getStart());
		mQueue.add(new CandidateAnalysis(s));
	}

	public void info() {
		for (CandidateAnalysis ca : mQueue) {
			logger.debug(logPrefix+ca);
			logger.debug(logPrefix+ca.printDerivation());
		}
	}
	
	public void status() {
		logger.debug(logPrefix+"status: remains="+mQueue.size()
				+" completable="+ getNumberOfCompletableAnalyses()
				+" expansions="+cntExpansions
				+" degradations="+cntDegradations);
	}
	
	public void setLogger(Logger l) {
		logger = l;
	}

}
