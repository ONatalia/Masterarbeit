package org.cocolab.inpro.irmrsc.rmrs;

import java.util.Set;
import java.util.TreeSet;

import org.jdom.Element;


public class Hook implements VariableIDsInterpretable {

	private int mLabel;
	private int mAnchor;
	private int mIndex;
	
	public Hook(int Label, int Anchor, int Index) {
		super();
		mLabel = Label;
		mAnchor = Anchor;
		mIndex = Index;
	}
	
	public Hook(Hook h) {
		this(h.getLabel(), h.getAnchor(), h.getIndex());
	}
	
	// only for xml loading
	public Hook() {
		super();
	}
	
	public int getLabel() {
		return mLabel;
	}

	public int getAnchor() {
		return mAnchor;
	}

	public int getIndex() {
		return mIndex;
	}

	public Set<Integer> getVariableIDs() {
	 Set<Integer> l = new TreeSet<Integer>();
	 l.add(mLabel);
	 l.add(mAnchor);
	 l.add(mIndex);
	 return l;
	}
	
	public void replaceVariableID(int oldID, int newID) {
		if (mLabel  == oldID) mLabel  = newID;
		if (mAnchor == oldID) mAnchor = newID;
		if (mIndex  == oldID) mIndex  = newID;
	}
	
	// This does not show the correct type, because this depends on the
	// VarID evaluation environment.
	@Override
	public String toString() {
		return "[#v" + mLabel + ":#v" + mAnchor + ":#v" + mIndex + "]";
	}
	
	// initializes this object from XML element: <hook>
	public void parseXML(Element e) {
		mLabel  = Integer.parseInt(e.getAttributeValue("l"));
		mAnchor = Integer.parseInt(e.getAttributeValue("a"));
		mIndex  = Integer.parseInt(e.getAttributeValue("i"));
	}
}
