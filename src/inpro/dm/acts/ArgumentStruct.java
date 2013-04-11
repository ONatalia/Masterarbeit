package inpro.dm.acts;

import inpro.nlu.AVPair;

/**
 * A convenience class for bundling intransitive, transitive, ditransitive
 * argument structures for use in dialogue acts.
 * Arguments are simple String representations.
 * @author okko
 *
 */
public class ArgumentStruct {
	
	/**
	 * The arguments
	 */
	private String arg1;
	private String arg2;
	
	/**
	 * Transitive argument constructor 
	 * @param arg
	 */
	public ArgumentStruct(String arg) {
		this.arg1 = arg;
	}
	
	/**
	 * Ditransitive argument constructor
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	public ArgumentStruct(String arg1, String arg2) {
		this.arg1 = arg1; this.arg2 = arg2;
	}
	
	/**
	 * Copy constructor
	 * @param as the argument structure to copy.
	 */
	public ArgumentStruct(ArgumentStruct as) {
		this.arg1 = as.arg1;
		this.arg2 = as.arg2;
	}
	
	/**
	 * Checks of this structure is transitive
	 * @return true if only the first argument is initialized, else false
	 */
	public boolean isTransitive() {
		return (this.arg1 != null && this.arg2 == null);
	}

	/**
	 * Checks of this structure is transitive
	 * @return true if both arguments are initialized, else false 
	 */
	public boolean isDitransitive() {
		return (this.arg1 != null && this.arg2 != null);
	}

	/**
	 * Checks if this structure is intransitive
	 * @return true if both arguemnts are initialized, else false
	 */
	public boolean isIntransitive() {
		return (this.arg1 == null && this.arg2 == null);
	}
	
	/**
	 * Compares this argument structure with another.
	 * @param o the argument structure to compare against
	 * @return true if o is an ArgumentStruct and they have the same arguments, false if not.
	 */
	public boolean equals(Object o) {
		assert o instanceof ArgumentStruct;
		if (!(o instanceof ArgumentStruct)) {
			return false;
		} else {
			ArgumentStruct as = (ArgumentStruct) o;
			if (this.isIntransitive() && as.isIntransitive()) {
				return true;
			} else if (this.isTransitive() && as.isTransitive()) {
				return (this.arg1.equals(as.arg1));
			}
			return (this.arg1.equals(as.arg1) && this.arg2.equals(as.arg2));
		}
	}
	
	@Override
	public int hashCode() {
		if (this.isIntransitive())
			return 0;
		if (this.isTransitive())
			return arg1.hashCode();
		return arg1.hashCode() * 71 + 73 * arg2.hashCode(); 
	}


	/**
	 * Getter for this structure's first argument
	 * @return arg1 the first argument
	 */
	public String getArg1() {
		return this.arg1;
	}


	/**
	 * Getter for this structure's second argument
	 * @return arg2 the first argument
	 */
	public String getArg2() {
		return this.arg2;
	}
	
	/**
	 * Setter for this structure's first argument
	 * @param arg the String to set arg1 to
	 */
	public void setArg1(String arg) {
		this.arg1 = arg;
	}

	/**
	 * Setter for this structure's second argument
	 * @param arg the String to set arg2 to
	 */
	public void setArg2(String arg) {
		this.arg2 = arg;
	}

	/**
	 * Attempts replacing the arguments of this structure from an AVPair.
	 * Succeeds if the current value of either argument equals the attribute of
	 * the AVPair and results in replacement by its value.
	 * For example if this structure has arg1 "take" and arg2 "tile-1" and
	 * the AVPair "tile-1:tile-2" will reset arg2 to "tile-2".
	 * @param avp the AVPair to reset from
	 * @return true if successful, false if not
	 */
	public boolean resetArgumentfromAVPair(AVPair avp) {
		if (this.isIntransitive()) {
			return false;
		} else if (this.isTransitive()) {
			if (this.arg1.equals(avp.getAttribute()))
				this.arg1 = (String) avp.getValue();
		} else {
			if (this.arg1.equals(avp.getAttribute())) {
				this.arg1 = (String) avp.getValue();
			} else if (this.arg2.equals(avp.getAttribute())) {
				this.arg2 = (String) avp.getValue();
			}
		}
		return false;
	}
	
	/**
	 * Builds a string representation of this arguemnt structure.
	 * @return the string rep
	 */
	public String toString() {
		if (this.isIntransitive()) {
			return "()";
		} else if (this.isTransitive()) {
			return "(" + this.arg1 + ")";
		} else {
			return "(" + this.arg1 + "," + this.arg2 + ")";
		}
		
	}

}
