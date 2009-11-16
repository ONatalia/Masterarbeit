package org.cocolab.inpro.dialogmanagement.composer;

public class AVPair {

	private String attribute;
	private Object value;

	AVPair(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	/**
	 * Handles interfacing AVM construction/unification 
	 * Convenience constructor to create AVPair from one string,
	 * where A and V are separated by a colon, like "size:big"
	 * or from attribute-value <String, Object> input for more
	 * complex AVMs.
	 * @author okko, timo
	 */
	AVPair(String attval) {
		assert (attval.contains(":"));
		String[] tokens = attval.split("\\s*:\\s*");
		assert (tokens.length == 2);
		this.attribute = tokens[0];
		this.value = tokens[1];
	}

	AVPair(String attribute, Object value) {
		this.attribute = attribute;
		this.value = value;
	}

	public String getAttribute() {
		return this.attribute;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public String toString() {
		return getAttribute() + " : " + getValue(); 
	}

}
