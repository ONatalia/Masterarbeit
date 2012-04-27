/**
 * 
 */
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


// TODO:
// - copy constructor
// - funktionalität in kleine methoden auslagern (z.B. popTop, pushSyms, addDerive...? 

/**
 * @author andreas
 *
 */
public class CandidateAnalysis implements Comparable<CandidateAnalysis>{
	
	private List<String> mDerivation; // sequence of all rules used
	private List<String> mLastDerive; // last incremental part of the derivation
	private CandidateAnalysis mAntecedent; // former analysis of which this is an extension
	private Deque<Symbol> mStack; // sequence of nonterminal symbols that need to be accounted for
	private double mProbability; // product of all probabilities of rules used in mDerivation
	private double mFigureOfMerit; // product of mProbability and a lookahead probability 
	private List<String> remainingString;
	
	public CandidateAnalysis(List<String> mDerivation, List<String> mLastDerive, CandidateAnalysis mAntecedent,
			Deque<Symbol> mStack, double mProbability, double mFigureOfMerit,
			List<String> remainingString) {
		this.mDerivation = new ArrayList<String>(mDerivation);
		this.mLastDerive = new ArrayList<String>(mLastDerive);
		this.mAntecedent = mAntecedent; // link not copy
		this.mStack = new ArrayDeque<Symbol>(mStack);
		this.mProbability = mProbability;
		this.mFigureOfMerit = mFigureOfMerit;
		this.remainingString = new ArrayList<String>(remainingString);
	}
	
	public CandidateAnalysis(Deque<Symbol> mStack) {
		this.mDerivation = new ArrayList<String>();
		this.mLastDerive = new ArrayList<String>();
		this.mAntecedent = null; // first element
		this.mStack = new ArrayDeque<Symbol>(mStack);
		this.mProbability = 1.0;
		this.mFigureOfMerit = 1.0;
		this.remainingString = new ArrayList<String>();
	}
	
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
		this.remainingString = new ArrayList<String>();
		for (String s : ca.remainingString) this.remainingString.add(s); 
	}
	
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
		// prepare rest
		List<String> newRemainingString =  new ArrayList<String>(remainingString);
		// build object
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, newProbability, newFigureOfMerit, newRemainingString);
		return ca;
	}
	
	public CandidateAnalysis repair(Symbol requiredToken) {
		String s = "r("+requiredToken.getSymbol()+")";
		return match_intern(s);
	}
	
	public CandidateAnalysis deletion(Symbol deletedToken) {
		String s = "d("+deletedToken.getSymbol()+")";
		return match_intern(s);
	}
	
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
		// prepare rest
		List<String> newRemainingString = new ArrayList<String>(remainingString);
		// build object
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, newProbability, newFigureOfMerit, newRemainingString);
		return ca;
	}
	
	// similar to match, but the top stack symbol is not popped.
	public CandidateAnalysis insert(Symbol insertedToken) {
		String deriveIdentifier = "i("+insertedToken.getSymbol()+")";
		List<String> newDerivation =  new ArrayList<String>(mDerivation);
		newDerivation.add(deriveIdentifier);
		List<String> newLastDerive =  new ArrayList<String>(mLastDerive);
		newLastDerive.add(deriveIdentifier);
		Deque<Symbol> newStack = new ArrayDeque<Symbol>(mStack);
		List<String> newRemainingString = new ArrayList<String>(remainingString);
		CandidateAnalysis ca = new CandidateAnalysis(newDerivation, newLastDerive, this.mAntecedent, newStack, mProbability, mFigureOfMerit, newRemainingString);
		return ca;
	}

	public void consumeFiller(String fillername) {
		this.mDerivation.add(fillername);
		this.mLastDerive.add(fillername);
	}
	
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
	
	/**
	 * @return the mProbability
	 */
	public double getProbability() {
		return mProbability;
	}
	
	public void degradeProbability(double f) {
		mProbability *= f;
	}

	/**
	 * @return the mFigureOfMerit
	 */
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
				+ "P=" + new DecimalFormat("###.######").format(100*mProbability) + "%\\n"
				+ "S=" + mStack;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LD=" + mLastDerive + "\\n"
				+ "P=" + new DecimalFormat("###.######").format(100*mProbability) + "%\\n"
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

