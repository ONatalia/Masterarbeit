package inpro.irmrsc.parser;

import inpro.irmrsc.simplepcfg.Production;
import inpro.irmrsc.simplepcfg.Symbol;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A candidate analysis entertained by the {@link SITDBSParser}, most importantly consisting of
 * a stack, a derivation history and the probability of the derivation.
 * <p />
 * The main parsing operations modifying a candidate analysis are implemented here: The classic
 * top down parser actions predict ({@link #expand(Production)}) and match ({@link #match(Symbol)}),
 * as well as the robust operations for insertions ({@link #insert(Symbol)}), deletions
 * ({@link #deletion(Symbol)}) and repairs ({@link #repair(Symbol)}). The probability of a derivation
 * can be externally degraded, as in the reference-pruning-setting.
 * 
 * @author Andreas Peldszus
 */
public class CandidateAnalysis implements Comparable<CandidateAnalysis>{
	
	/** the sequence of all rules used in this derivation */
	private List<String> mDerivation;
	
	/** the sequence of rules used in the last incremental step of the derivation */
	private List<String> mLastDerive;
	
	/** the former analysis of which this is an extension */
	private CandidateAnalysis mAntecedent;
	
	/** the sequence of nonterminal symbols that need to be accounted for */
	private Deque<Symbol> mStack;
	
	/** the product of all rule probabilities used in the derivation and all external mali */
	private double mProbability;
	
	/** the product of the derivations probability and the lookahead probability -- not used at the moment */
	private double mFigureOfMerit;
	
	public CandidateAnalysis(List<String> mDerivation, List<String> mLastDerive, CandidateAnalysis mAntecedent,
			Deque<Symbol> mStack, double mProbability, double mFigureOfMerit) {
		this.mDerivation = new ArrayList<String>(mDerivation);
		this.mLastDerive = new ArrayList<String>(mLastDerive);
		this.mAntecedent = mAntecedent; // link not copy
		this.mStack = new ArrayDeque<Symbol>(mStack);
		this.mProbability = mProbability;
		this.mFigureOfMerit = mFigureOfMerit;
	}
	
	/** constructor to initiate a chain of derivations */
	public CandidateAnalysis(Deque<Symbol> mStack) {
		this.mDerivation = new ArrayList<String>();
		this.mLastDerive = new ArrayList<String>();
		this.mAntecedent = null; // first element
		this.mStack = new ArrayDeque<Symbol>(mStack);
		this.mProbability = 1.0;
		this.mFigureOfMerit = 1.0;
	}
	
	/** copy constructor */
	public CandidateAnalysis(CandidateAnalysis ca) {
		this.mDerivation = new ArrayList<String>();
		for (String rule : ca.mDerivation) this.mDerivation.add(rule);
		this.mLastDerive = new ArrayList<String>();
		for (String rule : ca.mLastDerive) this.mLastDerive.add(rule);
		this.mAntecedent = ca.mAntecedent; // link not copy
		this.mStack = new ArrayDeque<Symbol>();
		for (Symbol s : ca.mStack) this.mStack.addLast(new Symbol(s));
		this.mProbability = ca.mProbability;
		this.mFigureOfMerit = ca.mFigureOfMerit;
	}
	
	/**
	 * The 'predict' parsing action: The top stack element is popped and
	 * the symbols on the righthandside of the predicte rule are pushed
	 * onto the stack. The derivations probability is multiplied with the
	 * probability of the predicted rule.
	 * @param p the grammar rule that is predicted
	 * @return the resulting new candidate analysis 
	 */
	public CandidateAnalysis expand(Production p) {
		// obsolete test
//		Symbol LHS = p.getLHS();
//		if (! (LHS.equals(mStack.peek()))) {
//			return null;
//		}
		// prepare derivation
		List<String> newDerivation = new ArrayList<String>(mDerivation);
		newDerivation.add(p.getID());
		List<String> newLastDerive = new ArrayList<String>(mLastDerive);
		newLastDerive.add(p.getID());
		// prepare stack
		Deque<Symbol> newStack = new ArrayDeque<Symbol>(mStack);
		newStack.pop();
		List<Symbol> RHS = p.getRHS();
		ListIterator<Symbol> i = RHS.listIterator(RHS.size());
		while(i.hasPrevious()) {
			newStack.push(i.previous());
		}
		// prepare weights
		double newProbability = mProbability * p.getProbability();
		double newFigureOfMerit = mFigureOfMerit; // TODO: insert calculation here
		// build object
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, newProbability, newFigureOfMerit);
		return ca;
	}

	/**
	 * The robust 'repair' parsing action: An unexpected token is replaced
	 * by the expected token and simply matched. The stack's top element is
	 * popped.
	 * @param requiredToken the token that was expected next in the derivation
	 * @return the resulting new candidate analysis 
	 */
	public CandidateAnalysis repair(Symbol requiredToken) {
		String s = "r("+requiredToken.getSymbol()+")";
		return match_intern(s);
	}
	
	/**
	 * The robust 'deletion' parsing action: An expected but non-existing token
	 * is considered to be existing and simply matched. The stack's top element
	 * is popped.
	 * @param deletedToken the token that was expected but not found
	 * @return the resulting new candidate analysis
	 */
	public CandidateAnalysis deletion(Symbol deletedToken) {
		String s = "d("+deletedToken.getSymbol()+")";
		return match_intern(s);
	}
	
	/**
	 * The 'match' parsing action: An expected token is matched. The stack's
	 * top element is popped.
	 * @param nextToken the expected token
	 * @return the resulting new candidate analysis
	 */
	public CandidateAnalysis match(Symbol nextToken) {
		String s = "m("+nextToken.getSymbol()+")";
		return match_intern(s);
	}
	
	private CandidateAnalysis match_intern(String deriveIdentifier) {
		// prepare derivation
		List<String> newDerivation =  new ArrayList<String>(mDerivation);
		newDerivation.add(deriveIdentifier);
		List<String> newLastDerive =  new ArrayList<String>(mLastDerive);
		newLastDerive.add(deriveIdentifier);
		// prepare stack
		Deque<Symbol> newStack = new ArrayDeque<Symbol>(mStack);
		newStack.pop();
		// prepare weights
		double newProbability = mProbability; // TODO: there should be no terminal probability, right?
		double newFigureOfMerit = mFigureOfMerit; // TODO: insert calculation here
		// build object
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, newProbability, newFigureOfMerit);
		return ca;
	}
	
	/**
	 * The robust 'insertion' parsing action: A unexpected token is added to the
	 * derivation without altering the stack.
	 * @param insertedToken the unexpected token to be inserted
	 * @return the resulting new candidate analysis
	 */
	public CandidateAnalysis insert(Symbol insertedToken) {
		String deriveIdentifier = "i("+insertedToken.getSymbol()+")";
		List<String> newDerivation =  new ArrayList<String>(mDerivation);
		newDerivation.add(deriveIdentifier);
		List<String> newLastDerive =  new ArrayList<String>(mLastDerive);
		newLastDerive.add(deriveIdentifier);
		Deque<Symbol> newStack = new ArrayDeque<Symbol>(mStack);
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, mProbability, mFigureOfMerit);
		return ca;
	}

	/** simply adds a filler to this derivation without changing anything else */
	public void consumeFiller(String fillername) {
		this.mDerivation.add(fillername);
		this.mLastDerive.add(fillername);
	}
	
	/** links this analysis to an antecedent analysis */
	public void newIncrementalStep(CandidateAnalysis oldCA) {
		this.mLastDerive = new ArrayList<String>();
		this.mAntecedent = oldCA;
	}
	
	public CandidateAnalysis getAntecedent() {
		return this.mAntecedent;
	}
	
	public boolean isComplete() {
		return mStack.isEmpty();
	}
	
	public boolean isCompletable(Set<Symbol> epsilonproductions) {
		boolean completable = true;
		for (Symbol sym : mStack) {
			if (! epsilonproductions.contains(sym)) {
				completable = false;
				break;
			}
		}
		return completable;
	}
	
	public Symbol getTopSymbol() {
		return mStack.peek();
	}
	
	public Deque<Symbol> getStack() {
		return mStack;
	}
	
	public double getProbability() {
		return mProbability;
	}
	
	public void degradeProbability(double f) {
		mProbability *= f;
	}

	public double getFigureOfMerit() {
		return mFigureOfMerit;
	}

	public List<String> getLastDerive() {
		return mLastDerive;
	}
	
	public boolean hasRobustOperationsLately() {
		for (String rule : mLastDerive) {
			if (rule.startsWith("r(") || rule.startsWith("d(") || rule.startsWith("i(")) {
				return true;
			}
		}
		return false;
	}
	
	public int getNumberOfMatches() {
		int i = 0;
		for (String rule : mDerivation) {
			if (rule.startsWith("m(")) {
				i++;
			}
		}
		return i;
	}
	
	public int getNumberOfRepairs() {
		int i = 0;
		for (String rule : mDerivation) {
			if (rule.startsWith("r(")) {
				i++;
			}
		}
		return i;
	}
	
	public int getNumberOfInsertions() {
		int i = 0;
		for (String rule : mDerivation) {
			if (rule.startsWith("i(")) {
				i++;
			}
		}
		return i;
	}
	
	public int getNumberOfDeletions() {
		int i = 0;
		for (String rule : mDerivation) {
			if (rule.startsWith("d(")) {
				i++;
			}
		}
		return i;
	}
	
	@Override
    public int compareTo(CandidateAnalysis y) {
		if(mProbability < y.getProbability()) {
			return 1;
		}
		if(mProbability > y.getProbability()) {
			return -1;
		}
		return 0;
    }
	
	public String toFinalString() {
		return String.format("%1$20.20f",mProbability) + "\t" + mDerivation + "";
	}
	
	public String toFullString() {
		return "D=" + mDerivation + " "
				+ "LD=" + mLastDerive + " "
				+ "P=" + new DecimalFormat("#.#########").format(mProbability) + "%\\n"
				+ "S=" + mStack;
	}
	
	@Override
	public String toString() {
		return "LD=" + mLastDerive + "\\n"
				+ "P=" + new DecimalFormat("#.#########").format(mProbability) + "%\\n"
				+ "S=" + mStack;
	}

	public String printDerivation() {
		Deque<String> l = new ArrayDeque<String>();
		l.addFirst(this.mLastDerive.toString());
		CandidateAnalysis ca = this.mAntecedent;
		while (ca != null) {
			l.addFirst(ca.mLastDerive.toString());
			ca = ca.mAntecedent;
		}
		return l.toString();
	}
}


