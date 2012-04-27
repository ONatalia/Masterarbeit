package inpro.irmrsc.simplepcfg;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * @return the mID
	 */
	public String getID() {
		return mID;
	}

	/**
	 * @return the mLHS
	 */
	public Symbol getLHS() {
		return mLHS;
	}

	/**
	 * @param mLHS the mLHS to set
	 */
	public void setLHS(Symbol mLHS) {
		this.mLHS = mLHS;
	}

	/**
	 * @return the mRHS
	 */
	public List<Symbol> getRHS() {
		return mRHS;
	}

	/**
	 * @param mRHS the mRHS to set
	 */
	public void setRHS(List<Symbol> mRHS) {
		this.mRHS = new ArrayList<Symbol>(mRHS);
	}

	/**
	 * @return the mProbability
	 */
	public double getProbability() {
		return mProbability;
	}

	/**
	 * @param mProbability the mProbability to set
	 */
	public void setProbability(double mProbability) {
		this.mProbability = mProbability;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = "[" + mID + ": " + mLHS + " --> " + mProbability + " ";
		for (Symbol sym : mRHS)
			s += (sym + " ");
		return s + "]";
	}	
}