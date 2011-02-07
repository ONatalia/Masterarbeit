package org.cocolab.inpro.nlu;

/**
 * A simple attribute-value pair. Attributes are always strings,
 * values can be any object.
 * @author okko
 */
public class AVPair {

	/**
	 * Attribute and values
	 */
	private String attribute;
	private Object value;
	// TODO: restrict to only specific object types

	/**
	 * Constructor building AVPair from two strings for 
	 * attribute and value
	 */
	public AVPair(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}

	/**
	 * Constructor building AVPair from attribute String and 
	 * Object value
	 */
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
		assert (tokens.length == 2) : "Malformed AVPair string: " + attval;
		this.attribute = tokens[0];
		this.value = tokens[1];
	}

	public String getAttribute() {
		return this.attribute;
	}
	
	/**
	 * Gets this AVPair's value
	 * @return this avp's value
	 */
	public Object getValue() {
		return this.value;
	}
	
	/**
	 * Sets this AVPair's value
	 * @param value
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Clears this AVPair's value
	 */
	public void clearValue() {
		this.value = null;
	}
	
	/**
	 * Builds a string representation of this AVPair
	 * in the form of "attribute : value".
	 * @return this avp's string rep
	 */
	public String toString() {
		String att = this.getAttribute();
		String val = this.getValue().toString();
		if (att == null)
			att = "null";
		if (val == null)
			val = null;
		return att + " : " + val; 
	}
	
	/**
	 * Compares this AVPair to another
	 * @param avp the AVPair to compare to
	 * @return true if they have the same attribute and value, else false
	 */
	public boolean equals(AVPair avp) {
		if (avp == null)
			return false;
		if (this.value == null && avp.getValue() == null)
			return (this.attribute.equals(avp.getAttribute()));		
		return (this.attribute.equals(avp.attribute) &&
				this.value.equals(avp.value));
	}
	
	/**
	 * Checks if this AVP matches a string representation
	 * @param string the string to compare to
	 * @return true if this AVPair's string representation matches the string.
	 */
	public boolean equals(String string) {
		return (this.toString().equals(string) || (this.toString().replace(" : ", ":").equals(string))) ;
	}


}
