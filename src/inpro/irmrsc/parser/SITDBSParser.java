package inpro.irmrsc.parser;

import inpro.irmrsc.simplepcfg.Grammar;
import inpro.irmrsc.simplepcfg.Production;
import inpro.irmrsc.simplepcfg.Symbol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

/**
 * A Simple (robust) Incremental Top Down Beam Search Parser.
 * <p />
 * The parser is fed incrementally with new input tokens. Its candidate analyses are stored in a priority queue,
 * serving the most probable first. The parser searches top down for analyses and moves those successfully matching
 * the current input token in a new queue. The parser continues to search for further analyses until a probability
 * threshold is reached, dynamically determined by the base beam factor the current size of the new queue and the
 * highest probability in the new queue. All remaining analyses are pruned.
 * <p />
 * Additionally, if robust parsing is activated, three robust operations allow to entertain hypotheses about deletions,
 * insertions and repairs of input token. Robust operations are restricted in many ways: Only one is allowed to occur
 * between two real input tokens. Each operation induces a probability malus. Finally there is a limit of robust operations
 * per sentence.
 * <p />
 * Inspired by Brian Roark 2001 Ph.D. Thesis, Department of Cognitive and Linguistic Sciences, Brown University.
 * 
 * @author Andreas Peldszus
 */
public class SITDBSParser {
	
	static String logPrefix = "[P] ";
	static Logger logger;
	
	// counters for evaluations statistics
	
	/** a counter to keep track of the number of expansions */
	public static int cntExpansions = 0;
	
	/** a counter to keep track of the number of externally degraded analyses (see {@link #degradeAnalysis(CandidateAnalysis, double)}). */
	public static int cntDegradations = 0;
	
	/** a counter to keep track of the number of prunes analyses, i.e. of those that fell out of the beam */
	public static int cntPrunes = 0;
	
	/** a counter to keep track of the number of derivations that survived a parsing step, i.e. of those that did not fell out of the beam */
	public static int cntDerivations = 0;
	
	
	// robustness settings
	
	/** allow or disallow the parser to use robust operations */
	public static boolean beRobust = true;
	
	/** the maximum number of repair hypotheses allowed per sentence */
	public static int maxRepairs = 2;
	
	/** the probability malus a derivations receives for each repair hypothesis */
	public static double repairMalus = 0.05;
	
	/** the maximum number of insertion hypotheses allowed per sentence */
	public static int maxInsertions = 2;
	
	/** the probability malus a derivations receives for each repair insertion */
	public static double insertionMalus = 0.02;
	
	/** the maximum number of deletion hypotheses allowed per sentence */
	public static int maxDeletions = 2;
	
	/** the probability malus a derivations receives for each repair deletion */
	public static double deletionMalus = 0.01;
	
	
	// general settings
	
	/** the maximum number of candidate analysis allowed in the parsers queue */
	public static int maxCandidatesLimit = 1000;

	/** the name of the POS-tag and the (parser internal) syntactic rule for fillers */
	public static final String fillerRuleAndTagName = "fill";
	
	/** the name of the POS-tag for unknown tags */
	public static final Symbol unknownTag = new Symbol("unknown");
	
	/** the name of the POS-tag marking the end of utterance */
	public static final Symbol endOfUtteranceTag = new Symbol("S!");

	
	// members
	
	/** the parser main internal data structure **/ 
	private PriorityQueue<CandidateAnalysis> mQueue;
	
	/** the grammar use for parsing **/
	private Grammar mGrammar;
	
	/** the base beam factor **/
	private double mBaseBeamFactor;
	

	public SITDBSParser(Grammar grammar, double bbf) {
		mGrammar = grammar;
		mBaseBeamFactor = bbf;
		reset();
	}
	
	public SITDBSParser(Grammar grammar) {
		this(grammar, 0.001);
	}
	
	/** copy constructor **/
	public SITDBSParser(SITDBSParser p) {
		assert p != null;
		this.mGrammar = p.mGrammar;
		this.mBaseBeamFactor = p.mBaseBeamFactor;
		this.mQueue = new PriorityQueue<CandidateAnalysis>();
		for (CandidateAnalysis ca : p.mQueue) 
			mQueue.add(new CandidateAnalysis(ca));
	}

	/** feeds the parser with the next input token **/
	public void feed(String nextToken) {
		this.feed(new Symbol(nextToken));
	}
	
	/** feeds the parser with the next input token **/
	public void feed(Symbol nextToken) {
		//logger.info(logPrefix+"feed: "+nextToken);
		
		if (mQueue == null || mQueue.size() < 1) {
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
				List<String> relevantProductions = mGrammar.getProductionsExpandingSymbol(topSymbol);
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
	
	/** returns the parsers queue (or an empty queue if it's null) */
	public PriorityQueue<CandidateAnalysis> getQueue() {
		return mQueue != null ? mQueue : new PriorityQueue<CandidateAnalysis>(1);
	}
	
	/** degrades the probability of a given CandidateAnalysis by a given malus **/
	public void degradeAnalysis(CandidateAnalysis ca, double malus) {
		assert (this.mQueue.remove(ca));
		cntDegradations++;
		ca.degradeProbability(malus);
		mQueue.add(ca);
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
