package org.cocolab.inpro.incremental.unit;

import org.cocolab.inpro.annotation.Label;

public class WordIU extends IU {

	// we keep start time, end time and text of the word in a label
	Label l;
	
	public WordIU(String word) {
		super();
		l = new Label(word);
	}
	
	public WordIU(Label l) {
		super();
		this.l = l;
	}
	
	public void updateLabel(Label l) {
		assert (this.l.getLabel().equals(l.getLabel()));
		this.l = l;
	}
	
	public boolean wordEquals(String str) {
		return str.equals(l.getLabel());
	}
	
	public String toTEDviewXML() {
		return l.toTEDViewXML();
	}
	
	public String toLabelLine() {
		return l.toString();
	}

	public String toString() {
		return l.toString();
	}
	
	public String toOAAString() {
		StringBuffer sb = new StringBuffer(Integer.toString(id));
		sb.append(",");
		sb.append(l.getLabel());
		return sb.toString();
	}

}
