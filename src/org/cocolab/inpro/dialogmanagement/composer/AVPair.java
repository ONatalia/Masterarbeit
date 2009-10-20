package org.cocolab.inpro.dialogmanagement.composer;

public class AVPair {

	private String attribute;
	private String value;

	AVPair(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	/**
	 * convenience constructor to create AVPair from
	 * one string, where A and V are separated by a colon,
	 * like "size:big"
	 */
	AVPair(String attval) {
		assert (attval.contains(":"));
		String[] tokens = attval.split("\\s*:\\s*");
		assert (tokens.length == 2);
		this.attribute = tokens[0];
		this.value = tokens[1];
	}
	
	public String getAttribute() {
		return this.attribute;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public String toString() {
		return getValue() + " : " + getAttribute(); 
	}

}
