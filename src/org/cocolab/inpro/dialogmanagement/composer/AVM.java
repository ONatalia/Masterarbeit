package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing an AVM with methods for equality, unification and setting
 * attributes.
 * @author okko
 */
public class AVM {

	// Implementing classes must define these in their constructors.
	private final String type;
	private HashMap<String, Object> attributes;
	private boolean monotonic = false;					//Defaults to false.  This is really "is part of a monotonic collection of AVMs".

	/**
	 * Empty constructor.
	 */
	AVM() {
		this.type = null;
	}

	/**
	 * String type constructor.
	 */
	AVM(String type) {
		this.type = type;
		this.initFromType(this.type);
	}

	/**
	 * Copy constructor.
	 * @param avm - the AVM to copy.
	 */
	protected AVM (AVM avm) {
		this.type = new String(avm.type);
		this.attributes = new HashMap<String, Object>(avm.attributes);
		this.monotonic = avm.monotonic;
	}

	private void initFromType(String type) {
		this.attributes = new HashMap<String, Object>();
		if (type.equals("tile")) {
			this.attributes.put("name", null);
			this.attributes.put("color", null);
			this.attributes.put("label", null);
			ArrayList<AVM> list0 = new ArrayList<AVM>();
			list0.add(new AVM("loc_spec"));
			this.attributes.put("loc", list0);
			ArrayList<AVM> list = new ArrayList<AVM>();
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			this.attributes.put("rel_loc", list);
		} else if (type.equals("field")) {
			this.attributes.put("color", null);
			ArrayList<AVM> list0 = new ArrayList<AVM>();
			list0.add(new AVM("loc_spec"));
			this.attributes.put("loc", list0);
			ArrayList<AVM> list = new ArrayList<AVM>(4);
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			this.attributes.put("rel_loc", list);
		} else if (type.equals("loc_spec")) {
			this.attributes.put("desc", null);
			this.attributes.put("lr", null);
			this.attributes.put("tb", null);
			ArrayList<AVM> list = new ArrayList<AVM>(4);
			list.add(new AVM("rc_spec"));
			list.add(new AVM("rc_spec"));
			list.add(new AVM("rc_spec"));
			list.add(new AVM("rc_spec"));
			this.attributes.put("rc", list);
		} else if (type.equals("rel_loc_spec")) {
			this.attributes.put("arg", null);
			this.attributes.put("relation", null);
		} else if (type.equals("rc_spec")) {
			this.attributes.put("ord", null);
			this.attributes.put("orient", null);
			this.attributes.put("row_col", null);
			this.monotonic = true;
		} else if (type.equals("dialog_act")) {
			this.attributes.put("act", null);
		} else {
			throw new IllegalArgumentException("Unknown AVM type: " + type);
		}
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
		//METHOD 2
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
	 * @param avm - an AVM to check subsumption against.
	 * @return true if <b>avm</b> is subsumed.
	 */
	@SuppressWarnings("unchecked")
	public boolean subsumes(AVM avm) {
		if (this == avm) {
			return true;
		} else if ((this.type != avm.type) || !avm.type.equals(this.type)) {
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
							boolean subsumes = false;
							for (AVM avm1 : (ArrayList<AVM>) value) {
								for (AVM avm2 : (ArrayList<AVM>) this.attributes.get(attribute)) {
									if (avm2.subsumes(avm1)) {
										subsumes = true;
									}
								}
								if (!subsumes) {
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
	 * Checks if unification is possible for two AVMs.
	 * @param avm - an AVM to check against whether unification would be successful.
	 * @return true if so, false otherwise
	 */
	public boolean unifies(AVM avm) {
		return (this.subsumes(avm));
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
			} else if (value instanceof AVM) {
				if (this.attributes.get(attribute) instanceof AVM) {
					AVM avm = (AVM) this.attributes.get(attribute); 
					if (avm.equals((AVM) value)) {
						return true;
					} else if (avm.unifies((AVM) value)) {
						avm.unify((AVM) value);
						this.attributes.put(attribute, (AVM) value);
						return true;
					}
				} else if (this.attributes.get(attribute) instanceof ArrayList) {
					for (AVM avm : (ArrayList<AVM>) this.attributes.get(attribute)) {
						if (avm.setAttribute(avp)) {
							return true;
						}
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
		String str = new String();
		str += "[";
		str += this.type;
		str += " ";
		for (String attribute : this.attributes.keySet()) {
			Object value = attributes.get(attribute);
			if (value instanceof String) {
				if (value != null) {
					str += attribute;
					str += ":";
					str += value;
					str += " ";
				}
			} else if (value instanceof AVM) {
				if (!((AVM) value).isEmpty()) {
					str += attribute;
					str += ":";
					str += value.toString();
					str += " ";
				}
			} else if (value instanceof ArrayList) {
				boolean printList = false;
				String listStr = attribute;
				listStr += ":[";
				for (AVM avm : (ArrayList<AVM>) value) {
					if (!avm.isEmpty()) {
						printList = true;
						listStr += avm.toString();
						listStr += " ";
					}
				}
				if (printList) {
					listStr += "]";
					str += listStr;
				}
			}
		}
		str += "]";
		return str;
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