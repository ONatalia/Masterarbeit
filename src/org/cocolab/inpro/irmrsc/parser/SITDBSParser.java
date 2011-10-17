/**
 * 
 */
package org.cocolab.inpro.irmrsc.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

import org.cocolab.inpro.irmrsc.simplepcfg.Grammar;
import org.cocolab.inpro.irmrsc.simplepcfg.Production;
import org.cocolab.inpro.irmrsc.simplepcfg.Symbol;

/**
 * @author andreas
 * Simple incremental top down beam search parser
 */
public class SITDBSParser {

	public static final int maxCandidatesLimit = 1000;
	public static final int maxDeletions = 2;
	public static final int maxInsertion = 4;
	
	private PriorityQueue<CandidateAnalysis> mQueue;
	private Grammar mGrammar;
	private double mBaseBeamFactor;
	
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

	public SITDBSParser(SITDBSParser p) {
		if (p == null) {
			// move the parameters to config
			Grammar grammar = new Grammar();
			grammar.loadXML("/home/andreas/workspace/ISem/data/PlayGrammar.xml");
			this.mGrammar = grammar;
			this.mBaseBeamFactor = 0.001;
			this.reset();
		} else {
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
		System.out.println("Feed parser: "+nextToken);
		PriorityQueue<CandidateAnalysis> newQueue = new PriorityQueue<CandidateAnalysis>(5);
		while (true) {
			CandidateAnalysis ca = mQueue.poll();
			if (ca == null) {
				// no more candidate analysis on the queue; await next token
				System.out.println(": no more ca on queue");
				break;
			}
			if (mQueue.size() >= maxCandidatesLimit) {
				// max size reached; await next token
				System.out.println(": max size reached");
				break;
			}
			if (! newQueue.isEmpty()) {
				if (ca.getProbability() < (newQueue.peek().getProbability() * mBaseBeamFactor * newQueue.size())) {
					// best derivation below threshold; await next token
					System.out.println(": outside beam");
					break;
				} else {
					System.out.println(": "+ca.getProbability()+" >= ("+newQueue.peek().getProbability()+" * "+mBaseBeamFactor+" * "+newQueue.size()+").");
				}
			}
			if (ca.isComplete()) {
				// stack is empty i.e. no more material expected, although we have another token;
				// drop this analysis and continue with next one
				System.out.println(": completed too early");
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
					Production p = mGrammar.getProduction(id);
					mQueue.add(ca.expand(p));
				}
			}
		}
		// make newQueue the new mQueue
		// problem: wenn newqueue leer, fehler+ende.
		// problem: satz ende aber derivation offen. wir brauch sowas wie isCompletable by with n*eps // closing of XZ-rules. vllt mit einem $S endmarker?
		// 
		mQueue = newQueue;
	}
	
	public PriorityQueue<CandidateAnalysis> getQueue() {
		return this.mQueue;
	}

	public void reset() {
		mQueue = new PriorityQueue<CandidateAnalysis>(5);
		Deque<Symbol> s = new ArrayDeque<Symbol>();
		s.push(mGrammar.getStart());
		mQueue.add(new CandidateAnalysis(s));
	}

	public void info() {
		for (CandidateAnalysis ca : mQueue) {
			System.out.println(ca);
		}
	}
	
}
