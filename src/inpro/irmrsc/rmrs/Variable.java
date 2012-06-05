package inpro.irmrsc.rmrs;

import org.jdom.Element;

/* Mutable class */

/**
 * A variable of a {@link Type} with an integer ID.
 * @author Andreas Peldszus
 */
public class Variable {
	
	public static enum Type { LABEL, ANCHOR, HOLE, INDEX, INDIVIDUAL, EVENT, UNDERSPEC; }
	
	private int mID;
	private Type mType;
	//private List<Pair<String,String>> Features;
	
	public Variable(int ID, Type aType) {
		this.mID = ID;
		this.mType = aType;
	}
	
	public Variable(int ID) {
		this(ID, Type.UNDERSPEC);
	}

	public Variable(Type Type) {
		this(0, Type);
	}
	
	public Variable() {
		this(0, Type.UNDERSPEC);
	}
	
	public Variable(Variable v) {
		this(v.getID(), v.getType());
	}

	public int getID() {
		return mID;
	}

	public Type getType() {
		return mType;
	}
	
	public boolean isSpecifiedIndexVariable() {
		return getType().equals(Type.EVENT) || getType().equals(Type.EVENT);
	}

	public void setID(int ID) {
		this.mID = ID;
	}

	public void setType(Type mType) {
		this.mType = mType;
	}
	
	//TODO: equals, compareTo
		
	public boolean sameTypeAs(Variable v) {
		return this.mType == v.getType();
	}
	
	public boolean sameIDAs(Variable v) {
		return this.mID == v.getID();
	}

	@Override
	public String toString() {
		String t;
		switch (mType) {
		case LABEL:
			t = "l"; break;
		case ANCHOR:
			t = "a"; break;
		case HOLE:
			t = "h"; break;
		case INDEX:
			t = "i"; break;
		case INDIVIDUAL:
			t = "x"; break;
		case EVENT:
			t = "e"; break;
		default:
			t = "?"; break;
		}
		return t+mID;
	}
	
	/**
	 * initializes this variable object from XML element variable definition {@code <vdef>}
	 * @param e the element 
	 */
	public void parseXML(Element e) {
		// id
		mID = Integer.parseInt(e.getAttributeValue("id"));
		// type
		String typeString = e.getAttributeValue("type");
		if (typeString != null && typeString.length() == 1) {
			switch(typeString.charAt(0)) {
			case 'l' : mType = Type.LABEL; break;
			case 'a' : mType = Type.ANCHOR; break;
			case 'h' : mType = Type.HOLE; break;
			case 'i' : mType = Type.INDEX; break;
			case 'x' : mType = Type.INDIVIDUAL; break;
			case 'e' : mType = Type.EVENT; break;
			case '?' : default : mType = Type.UNDERSPEC; break;
			}
		}
		// TODO:features
	}
}
