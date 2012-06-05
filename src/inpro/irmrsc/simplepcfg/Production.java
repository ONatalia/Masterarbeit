package inpro.irmrsc.simplepcfg;

import java.util.ArrayList;
import java.util.List;

/**
 * A weighted production rule of a {@link Grammar} expanding a lefthandside
 * {@link Symbol} into a list of righthandside symbols. Each production should
 * have an ID unique in the grammar.
 * @author Andreas Peldszus
 */
public class Production {

	private String mID;
	private Symbol mLHS;
	private List<Symbol> mRHS;
	private double mProbability;
	
	public Production(String ID, Symbol mLHS, List<Symbol> mRHS,
			double mProbability) {
		super();
		this.mID = ID;
		this.mLHS = mLHS;
		this.mRHS = new ArrayList<Symbol>(mRHS);
		this.mProbability = mProbability;
	}

	/** @return the production ID */
	public String getID() {
		return mID;
	}

	/** @return the lefthandside symbol */
	public Symbol getLHS() {
		return mLHS;
	}

	/** @param lhs the lefthandside symbol to set */
	public void setLHS(Symbol lhs) {
		this.mLHS = lhs;
	}

	/** @return the list of righthandside symbols */
	public List<Symbol> getRHS() {
		return mRHS;
	}

	/** @param rhs the list of righthandside symbols */
	public void setRHS(List<Symbol> rhs) {
		this.mRHS = new ArrayList<Symbol>(rhs);
	}

	/** @return the probability of the production */
	public double getProbability() {
		return mProbability;
	}

	/** @param probability the probability to set
	 */
	public void setProbability(double probability) {
		this.mProbability = probability;
	}

	@Override
	public String toString() {
		String s = "[" + mID + ": " + mLHS + " --> " + mProbability + " ";
		for (Symbol sym : mRHS)
			s += (sym + " ");
		return s + "]";
	}	
}