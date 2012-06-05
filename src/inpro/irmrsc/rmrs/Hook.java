package inpro.irmrsc.rmrs;

import java.util.Set;
import java.util.TreeSet;

import org.jdom.Element;

/** 
 * A hook, i.e. the main logical index, of a RMRS {@link Formula}
 * consisting of a label, an anchor and an index-variable. 
 * @author Andreas Peldszus
 */
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
	
	/** default constructor, only used for xml loading */
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

	@Override
	public Set<Integer> getVariableIDs() {
	 Set<Integer> l = new TreeSet<Integer>();
	 l.add(mLabel);
	 l.add(mAnchor);
	 l.add(mIndex);
	 return l;
	}
	
	@Override
	public void replaceVariableID(int oldID, int newID) {
		if (mLabel  == oldID) mLabel  = newID;
		if (mAnchor == oldID) mAnchor = newID;
		if (mIndex  == oldID) mIndex  = newID;
	}

	@Override
	public String toString() {
		// This does not generate the correct type prefix, because this depends on the
		// variable environment. Thus all variables are prefixed with a '#v'.
		return "[#v" + mLabel + ":#v" + mAnchor + ":#v" + mIndex + "]";
	}
	
	/**
	 * initializes this hook object from XML element variable definition {@code <hook>}
	 * @param e the element 
	 */
	public void parseXML(Element e) {
		mLabel  = Integer.parseInt(e.getAttributeValue("l"));
		mAnchor = Integer.parseInt(e.getAttributeValue("a"));
		mIndex  = Integer.parseInt(e.getAttributeValue("i"));
	}
}
