package org.cocolab.inpro.nlu;

/**
 * A simple attribute-value pair.
 * @author okko
 */
public class AVPair {

	private String attribute;
	/* what kind of Objects can be values? */
	private Object value;

	public AVPair(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}

	public AVPair(String attribute, Object value) {
		this.attribute = attribute;
		this.value = value;
	}

	/**
	 * Copy constructor
	 * @param avp to copy
	 */
	public AVPair(AVPair avp) {
		this.attribute = avp.attribute;
		this.value = avp.value;
	}

	/**
	 * Handles interfacing AVM construction/unification 
	 * Convenience constructor to create AVPair from one string,
	 * where A and V are separated by a colon, like "size:big"
	 * or from attribute-value <String, Object> input for more
	 * complex AVMs.
	 */
	public AVPair(String attval) {
		assert (attval.contains(":"));
		String[] tokens = attval.split("\\s*:\\s*");
		assert (tokens.length == 2);
		this.attribute = tokens[0];
		this.value = tokens[1];
	}

	public String getAttribute() {
		return this.attribute;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public String toString() {
		return getAttribute() + " : " + getValue(); 
	}
	
	public boolean equals(AVPair avp) {
		return (this.attribute.equals(avp.attribute) &&
				this.value.equals(avp.value));
	}

}
