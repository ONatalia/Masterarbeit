package org.cocolab.inpro.dialogmanagement.composer;

public class AVPair {

	private String attribute;
	private String value;

	AVPair(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	public String getAttribute() {
		return this.attribute;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public String toString() {
		return getValue() + " : " + getAttribute() + "\n"; 
	}

}
