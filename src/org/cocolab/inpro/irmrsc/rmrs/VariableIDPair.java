package org.cocolab.inpro.irmrsc.rmrs;

import java.util.Set;
import java.util.TreeSet;

public class VariableIDPair implements VariableIDsInterpretable {
	
	private int mLeft;
	private int mRight;
	
	public VariableIDPair (int left, int right) {
		mLeft = left;
		mRight = right;
	}
	
	public VariableIDPair (VariableIDPair p) {
		this(p.mLeft, p.mRight);
	}
	
	public int getLeft() {
		return mLeft;
	}

	public int getRight() {
		return mRight;
	}

	@Override
	public Set<Integer> getVariableIDs() {
		Set<Integer> l = new TreeSet<Integer>();
		l.add(mLeft);
		l.add(mRight);
		return l;
	}

	@Override
	public void replaceVariableID(int oldID, int newID) {
		if (mLeft  == oldID) mLeft  = newID;
		if (mRight == oldID) mRight = newID;
	}

	@Override
	public String toString() {
		return "[#v" + mLeft + ", #v" + mRight + "]";
	}


	
}
