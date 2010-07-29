package org.cocolab.inpro.nlu;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing an AVM with methods for equality, unification and setting
 * attributes.
 * @author okko
 */
public class AVM {

	private final String type;
	private HashMap<String, Object> attributes;
	private boolean monotonic;

	/**
	 * String type, HashMap avmStructures constructor.
	 */
	public AVM(String type, HashMap<String, HashMap<String, String>> avmStructures) {
		this(type, false, avmStructures);
	}

	/**
	 * String type, boolean monotonic, HashMap avmStructures constructor.
	 */
	public AVM(String type, boolean monotonic, HashMap<String, HashMap<String, String>> avmStructures) {
		this.type = type;
		this.attributes = this.unpackStructures(avmStructures, type);
		this.monotonic = monotonic;
	}

	/**
	 * Copy constructor.
	 * @param avm - the AVM to copy.
	 */
	protected AVM (AVM avm) {
		type = avm.type;
		monotonic = avm.monotonic;
		// do a deep copy of attributes
		attributes = new HashMap<String, Object>();
		for (String attribute : avm.attributes.keySet()) {
			Object value = avm.attributes.get(attribute);
			if (value instanceof AVM) {
				value = new AVM((AVM) value);
			}
			attributes.put(attribute, value);
		}
	}

	/**
	 * Builds attribute map from flat string map (read from file by AVMStructureUtil).
	 * @param avmStructures HashMap 
	 * @param type of the AVM
	 */
	private HashMap<String, Object> unpackStructures(HashMap<String, HashMap<String, String>> avmStructures, String type) {
		assert (type != null && !type.equals("")) : "Must specify AVM type! (config error?)";
		assert (avmStructures.containsKey(type)) : "No AVM structure known for type " + type + "! (config error?)";
		HashMap<String, Object> attributes = new HashMap<String, Object>();
		HashMap<String, String> avmStructure = avmStructures.get(type);
		for (String attribute : avmStructure.keySet()) {
			if (avmStructure.get(attribute).equals("String")) {						// String attribute
				attributes.put(attribute, null);
			} else if (((String) avmStructure.get(attribute)).startsWith("(")) {	// AVM List attribute
				String[] tokens = ((String) avmStructure.get(attribute)).replaceAll("[\\(\\)]", "").split(",");
				String childType = tokens[0];
				int num = Integer.parseInt(tokens[1]);
				boolean monotonic = false;
				if (tokens[2].equals("true"))
					monotonic = true;
				ArrayList<AVM> list = new ArrayList<AVM>(1);
				for (int i=0; i<num; i++) {
					list.add(new AVM(childType, monotonic, avmStructures));
				}
				attributes.put(attribute, list);
			} else {																// AVM attribute
				ArrayList<AVM> list = new ArrayList<AVM>(1);
				list.add(new AVM((String) avmStructure.get(attribute), avmStructures));
				attributes.put(attribute, list);
			}
		}
		return attributes;
	}

	/**
	 * Convenience method checking equality of two AVMs.
	 * @param avm - an AVM to compare this AVM with.
	 * @return true if they are the same.
	 */
	public boolean equals(AVM avm) {
		if (this == avm) {
			return true;
		} else if ((this.type != avm.type) || !avm.type.equals(this.type)) {
			return false;
		} else {
			for (String attribute : this.attributes.keySet()) {
				if (!avm.attributes.keySet().contains(attribute)) {
					return false;
				} else if (this.attributes.get(attribute) != avm.attributes.get(attribute)) {
					return false;
				}
			}
			return (this.attributes.hashCode() == avm.attributes.hashCode());	
		}
	}

	/**
	 * hashCode method for use by Collections.
	 */
	public int hashCode() { 
		int hash;
		hash = 37 * this.type.hashCode() * this.attributes.hashCode();
		return hash;
	}

	/**
	 * Attempts unifying AVM with the input to this method.
	 * @param avm - the AVM to unify with this AVM.
	 * @return the resulting unified AVM if unification was successful,
	 * null otherwise.
	 */
	@SuppressWarnings("unchecked")
	public AVM unify(AVM avm) {
		for (AVPair avp : avm.getAVPairs()) {
			if (avp.getValue() instanceof String) {
				if (!this.setAttribute(avp)) {
					return null;
				}				
			} else if (avp.getValue() instanceof ArrayList) {
				for (AVM avm1 : (ArrayList<AVM>) avp.getValue()) {
					for (AVPair avp2 : avm1.getAVPairs()) {
						if (avp2.getValue() instanceof String) {
							if (!this.setAttribute(avp2)) {
								return null;
							}				
						} else if (avp2.getValue() instanceof ArrayList) {
							for (AVM avm2 : (ArrayList<AVM>) avp2.getValue()) {
								for (AVPair avp3 : avm2.getAVPairs()) {
									if (avp3.getValue() instanceof String) {
										if (!this.setAttribute(avp3)) {
											return null;
										}				
									}
								}
							}
						}
					}
				}
			}
		}
		return this;
	}

	/**
	 * Checks if unification is possible for two AVMs.
	 * @param avm - an AVM to check against whether unification would be successful.
	 * @return true if so, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean unifies(AVM avm) {
		if (this == avm) {
			return true;
		} else if (!avm.type.equals(this.type)) {
			return false;
		} else {
			for (AVPair avp : avm.getAVPairs()) {
				String attribute = avp.getAttribute();
				Object value = avp.getValue();
				if (!this.attributes.keySet().contains(attribute)) {
					return false;
				} else {
					if (value != null && this.attributes.get(attribute) != null) {
						if (value instanceof String) {
							if (!this.attributes.get(attribute).equals(value)) {
								return false;
							}
						} else if (value instanceof ArrayList) {
							for (AVM avm1 : (ArrayList<AVM>) value) {
								boolean unified = false;
								for (AVM avm2 : (ArrayList<AVM>) this.attributes.get(attribute)) {
									if (avm2.unifies(avm1)) {
										unified = true;
									}
								}
								if (!unified) {
									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Attempts setting an attribute to this AVM.  Recurses through local complex attributes
	 * (ie. AVMs or lists thereof) to attempt setting the attribute to child AVMs.
	 * @param avp - an AVPair to attempt setting against this AVM or its children.
	 * @return true if setting was successful, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	public boolean setAttribute(AVPair avp) {
		String attribute = avp.getAttribute();
		Object value = avp.getValue();
		if (this.attributes.keySet().contains(attribute)) {
			if (value == null) {
				return true;
			} else if (value instanceof String) {
				if (this.attributes.get(attribute) == null) {
					this.attributes.put(attribute, (String) value);
					return true;
				} else if (this.attributes.get(attribute).equals((String) value)) {
					if (!this.monotonic) {
						return true;					
					}
				}
			} else if (value instanceof ArrayList && this.attributes.get(attribute) instanceof ArrayList) {
				for (AVM avm1 : (ArrayList<AVM>) value) {
					boolean unified = false;
					for (AVM avm2 : (ArrayList<AVM>) this.attributes.get(attribute)) {
						if (avm2.unifies(avm1)) {
							avm2.unify(avm1);
							unified = true;
							break;
						}
					}
					if (unified) {
						return true;
					}
				}
			}
		} else {
			for (String localAttribute : this.attributes.keySet()) {
				if (this.attributes.get(localAttribute) instanceof AVM) {
					AVM localAVM = (AVM) this.attributes.get(localAttribute);
					if (localAVM.setAttribute(avp)) {
						return true;
					}
				} else if (this.attributes.get(localAttribute) instanceof ArrayList) {
					for (AVM localAVM : (ArrayList<AVM>) this.attributes.get(localAttribute)) {
						if (localAVM.setAttribute(avp)) {
							return true;
						}
					}
				} 
			}
		}
		return false;
	}
	
	/**
	 * Gets an ArrayList of all AVPairs attributed to this AVM, including null ones.
	 * @return avps - an ArrayList&lt;AVPair&gt;.
	 */
	public ArrayList<AVPair> getAVPairs() {
		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		for (String attribute : this.attributes.keySet()) {
			avps.add(new AVPair(attribute, this.attributes.get(attribute)));	
		}
		return avps;
	}
	
	/**
	 * Returns the type of this AVM.
	 * @return this.type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Returns the attributes of this AVM.  Values can be String or ArrayList.
	 * Classes using this should verify this using instanceof. 
	 * @return a HashMap&lt;String, Object&gt;.
	 */
	public HashMap<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * Returns the type of this AVM.
	 * @return this.type
	 */
	@SuppressWarnings("unchecked")
	public boolean isEmpty() {
		for (String attribute : this.attributes.keySet()) {
			Object value = attributes.get(attribute);
			if (value instanceof String) {
				if (value != null) {
					return false;
				}
			} else if (value instanceof AVM) {
				if (!((AVM) value).isEmpty()) {
					return false;
				}
			} else if (value instanceof ArrayList) {
				for (AVM avm : (ArrayList<AVM>) value) {
					if (!avm.isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Builds a string representation of the AVM with nested brackets.
	 * Empty AVMs are not represented.  Use AVM.toLongString() to do this instead.
	 * @return String representation of the AVM
	 */
	@SuppressWarnings("unchecked")
	public String toString() {
		// FIXME: much more efficient to use StringBuilder instead of += for Strings
		StringBuilder sb = new StringBuilder("[");
		sb.append(this.type);
		sb.append(" ");
		for (String attribute : this.attributes.keySet()) {
			Object value = attributes.get(attribute);
			if (value instanceof String) {
				if (value != null) {
					sb.append(attribute);
					sb.append(":");
					sb.append(value);
					sb.append(" ");
				}
			} else if (value instanceof AVM) {
				if (!((AVM) value).isEmpty()) {
					sb.append(attribute);
					sb.append(":");
					sb.append(value);
					sb.append(" ");
				}
			} else if (value instanceof ArrayList) {
				boolean printList = false;
				StringBuilder sb2 = new StringBuilder(attribute);
				sb.append(":[");
				for (AVM avm : (ArrayList<AVM>) value) {
					if (!avm.isEmpty()) {
						printList = true;
						sb2.append(avm);
						sb2.append(" ");
					}
				}
				if (printList) {
					sb2.append("]");
					sb.append(sb2);
				}
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Builds a short string representation of the AVM containing only its type.
	 * @return Short string representation of the AVM
	 */
	public String toShortString() {
		return "[" + this.type + "]";
	}

	/**
	 * Builds a complete string representation of the AVM with nested brackets.
	 * Use AVM.toString() for something more readible.
	 * @return Long string representation of the AVM
	 */
	public String toLongString() {
		String str = new String();
		str += "[";
		str += this.type;
		if (this.monotonic)
			str +=  "(m)";
		str += " ";
		for (String attribute : this.attributes.keySet()) {
			Object value = attributes.get(attribute);
			if (value == null) {
				str += attribute + ":null ";
			} else {
				str += attribute + ":" + value.toString() + " ";
			}
		}
		str += "]";
		return str;
	}

}