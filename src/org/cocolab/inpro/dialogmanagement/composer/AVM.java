package org.cocolab.inpro.dialogmanagement.composer;

public class AVM {

	/**
	 * Abstract class representing an AVM with methods
	 * for equality and unification.
	 */

	public boolean equals(AVM a) {
		return false;
	}

	public AVM unify(AVM a) {
		return a;
	}
	
	public boolean validateAttribute(String[] strs, String str) {
		/*
		 * Used by constructors and setters of AVM implementations.
		 * Takes an array of valid Strings and a string as arguments.
		 * Former is defined by AVM class.
		 * If former contains latter, attribute is valid.
		 */
		for (String s: strs) {
			if (s.equals(str)) {
				return true;
			}
		}
		return false;
	}

	public boolean validateAttribute(AVM[] avms, AVM avm) {
		/*
		 * Used by constructors and setters of AVM implementations.
		 * Takes an array of valid AVMs and an AVM as arguments.
		 * Former is defined by AVM class.
		 * If former contains latter, attribute is valid.
		 */
		for (AVM a: avms) {
			if (avm.equals(a)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return "";
	}
}