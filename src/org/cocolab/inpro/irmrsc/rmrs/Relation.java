package org.cocolab.inpro.irmrsc.rmrs;

import java.util.Set;
import java.util.TreeSet;

import org.jdom.Element;

public class Relation implements VariableIDsInterpretable {
	
	public static enum Type { LEXICAL, NONLEXICAL, ARGREL; }
	
	private int mLabel;
	private int mAnchor;
	private int mArgument;
	private String mName;
	private Type mType;
	private boolean mArgumentGiven;
	
	public Relation(int Label, int Anchor, int Argument, String Name, Type Type) {
		super();
		this.mLabel = Label;
		this.mAnchor = Anchor;
		this.mArgument = Argument;
		this.mArgumentGiven = true;
		this.mName = Name;
		this.mType = Type;
	}
	
	public Relation(int Label, int Anchor, String Name, Type Type) {
		super();
		this.mLabel = Label;
		this.mAnchor = Anchor;
		this.mArgument = Integer.MAX_VALUE; // just to make sure
		this.mArgumentGiven = false;
		this.mName = Name;
		this.mType = Type;
	}
	
	public Relation(Relation r) {
		this.mLabel = r.getLabel();
		this.mAnchor = r.getAnchor();
		this.mArgument = r.getArgument();
		this.mArgumentGiven = r.hasArgument();
		this.mName = r.getName();
		this.mType = r.getType();
	}
	
	public Relation() {
		super();
	}
	
	public int getLabel() {
		return mLabel;
	}

	public int getAnchor() {
		return mAnchor;
	}

	public boolean hasArgument() {
		return mArgumentGiven;
	}
	
	public int getArgument() {
		return mArgument;
	}

	public String getName() {
		return mName;
	}

	public Type getType() {
		return mType;
	}

	public Set<Integer> getVariableIDs() {
		Set<Integer> l = new TreeSet<Integer>();
		// Argument relations have no label
		if (mType != Type.ARGREL)
			l.add(mLabel);
		// Some relations may not have an argument
		if (mArgumentGiven)
			l.add(mArgument);
		// all relations have an anchor
		l.add(mAnchor);
		return l;
	}

	public void replaceVariableID(int oldID, int newID) {
		if (mLabel    == oldID) mLabel    = newID;
		if (mAnchor   == oldID) mAnchor   = newID;
		if (mArgument == oldID) mArgument = newID;
	}
	
	// Warning: Types are not correctly shown.
	@Override
	public String toString() {
		String s = "";
		switch (mType) {
			case LEXICAL:
				s += "#v"+mLabel+":#v"+mAnchor+":_"+mName+"(";
				if (mArgumentGiven) s += "#v"+mArgument;
				s += ")";
				break;
			case NONLEXICAL:
				s += "#v"+mLabel+":#v"+mAnchor+":"+mName+"(";
				if (mArgumentGiven) s += "#v"+mArgument;
				s += ")";
				break;
			case ARGREL:
				s += mName+"(#v"+mAnchor+",#v"+mArgument+")";
				break;
		}
		return s;
	}

	// initializes this object from XML element: <rel>
	public void parseXML(Element e) {
		mName = e.getAttributeValue("name");
		String typeString = e.getAttributeValue("type");
		if (typeString.equals("lex")) {
			mType = Type.LEXICAL;
		}
		if (typeString.equals("gram")) {
			mType = Type.NONLEXICAL;
		}
		// set label (argument relations have no label)
		if (typeString.equals("arg")) {
			mType = Type.ARGREL;
			mLabel = Integer.MAX_VALUE;
		} else {
			mLabel = Integer.parseInt(e.getAttributeValue("l"));			
		}
		// set anchor (obligatory)
		mAnchor = Integer.parseInt(e.getAttributeValue("a"));
		// set index/argument (optional for lexical and grammatical relations)
		String argString = e.getAttributeValue("i");
		if (argString == null || argString.equals("none")) {
			mArgumentGiven = false;
			mArgument = Integer.MAX_VALUE;
		} else {
			mArgumentGiven = true;
			mArgument = Integer.parseInt(e.getAttributeValue("i"));
		}
	}
}
