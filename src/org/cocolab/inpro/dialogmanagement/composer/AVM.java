package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class representing an AVM with methods for equality, unification and setting
 * attributes.
 * @author okko
 */
public class AVM {

	// Implementing classes must define these in their constructors.
	public Map<String, Object> attributes = new HashMap<String, Object>();
	protected String type;

	AVM() {}

	/**
	 * Copy constructor.
	 * @param avm - the AVM to copy.
	 */
	protected AVM (AVM avm) {
		this.type = new String(avm.type);
		this.attributes = new HashMap<String, Object>(avm.attributes);
	}

	/**
	 * Set attributes from AVPair.
	 * @param avp - the AVPair to set as attribute & value.
	 */
	AVM(AVPair avp) {
		try {
			this.setAttribute(avp);			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param a - an AVM to compare this AVM with.
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

	public int hashCode() { 
		int hash = 1;
		return hash;
	}
	
	@SuppressWarnings("unchecked")
	public boolean setAttribute(AVPair avp) {
		if (avp.getValue() instanceof String) {
			return setAttribute((String) avp.getAttribute(), (String) avp.getValue());
		} else if (avp.getValue() instanceof AVM) {
			return setAttribute((String) avp.getAttribute(), (AVM) avp.getValue());
		} else if (avp.getValue() instanceof ArrayList) {
			return setAttribute((String) avp.getAttribute(), (ArrayList<AVM>) avp.getValue());
		} else if (avp.getValue() == null) {
			return true;
		} else {
			throw new IllegalArgumentException("AVP value must be one of String, AVM or ArrayList, not " + avp.getValue().getClass().toString());
		}
	}
	
	private boolean setAttribute(String attribute, String value) {
		if (this.attributes.keySet().contains(attribute)) {
			if (this.attributes.get(attribute) == null || !this.attributes.get(attribute).equals(value)) {
				this.attributes.put(attribute, value);
				return true;
			}
		}
		return false;
	}

	private boolean setAttribute(String attribute, AVM value) {
		if (this.attributes.keySet().contains(attribute)) {
			if (this.attributes.get(attribute) != value) {
				if (this.attributes.get(attribute) != null && this.attributes.get(attribute).getClass().equals(value.getClass())) {
					if (this.attributes.get(attribute) == null || this.attributes.get(attribute).equals(this.attributes.get(attribute))) {
						this.attributes.put(attribute, value);
						return true;
					}
				}				
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean setAttribute(String attribute, ArrayList<AVM> values) {
		if (this.attributes.keySet().contains(attribute)) {
			if (this.attributes.get(attribute) != values) {
				if (this.attributes.get(attribute) != null && this.attributes.get(attribute) instanceof ArrayList) {
					ArrayList list = (ArrayList<AVM>) this.attributes.get(attribute);
					for (AVM a : values) {
						if (list.contains(a)) {
							return false;
						}
					}
					list.addAll(values);
					return true;
				}				
			}
		}
		return false;
	}

	/**
	 * Attempts unifying this AVM with the input to this method.
	 * Unification succeeds either if both AVMs share the same attributes
	 * and their values do not conflict (i.e. for simple values they
	 * are either null in at least one or identical, for complex/list
	 * values they must not be included in list value of this AVM) or
	 * the input AVM is a possible attribute of this AVM.
	 * <br />
	 * Unification is achieved by checking the latter case first, or else
	 * attempting setAttribute() for each AVPair of the input AVM.
	 * As soon as setAttribute() is unsuccessful, null is returned.
	 * @param a - an AVM to unify with this AVM.
	 * @return <b>b</b> - a new AVM, which is the unification of
	 * <b>b</b> and this AVM if unification was successful,
	 * null otherwise.
	 */
	public AVM unify(AVM avm) {
		if (this.equals(avm)) {
			return null;
		} else if (avm.getClass().equals(this.getClass())) {
			for (AVPair avp : avm.getAVPairs()) {
				if (!this.setAttribute(avp)) {
					return null;
				}
			}
		} else if (avm.type != null && this.attributes.get(avm.type.replaceAll("_spec", "")) != null) {
			if (!this.setAttribute(avm.type.replaceAll("_spec", ""), avm)) {
				ArrayList<AVM> avmList = new ArrayList<AVM>(1);
				avmList.add(avm);
				if (!this.setAttribute(new AVPair(avm.type.replaceAll("_spec", ""), avmList))) {
					return null;					
				}
			}
		} else {
			for (AVPair avp : avm.getAVPairs()) {
				if (avp.getValue() != null) {
					if (!this.setAttribute(avp)) {
						return null;
					}
				}
			}
		}
		return this;
	}

	public boolean unifies(AVM avm) {
		AVM that = new AVM(this);
		if (that.unify(avm) != null) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Return false if any String attribute has been set,
	 * any AVM attribute is not empty or any
	 * element in an ArrayList<AVM> attribute is not empty.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isEmpty() {
		for (String attribute : this.attributes.keySet()) {
			Object value = this.attributes.get(attribute);
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

	public ArrayList<AVPair> getAVPairs() {
		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		Iterator<String> i = this.attributes.keySet().iterator();
		while (i.hasNext()) {
			String attribute = i.next();
			Object value = this.attributes.get(attribute);
			avps.add(new AVPair(attribute, value));
		}
		return avps;
	}

	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * Builds a string representation of the AVM with nested brackets
	 * delimiting data structures as follows:<br />
	 * <> denotes AVM boundaries<br />
	 * [] denotes AVM list attribute boundaries<br />
	 * {} denotes AVM attributes boundaries<br />
	 * The first word in the AVM structure is its type.<br />
	 * <i>Example:</i> &lt;tile {col=green, rel_loc=[], loc=<...>} &gt;
	 * @return String representation of the AVM
	 */
	@SuppressWarnings("unchecked")
	public String toString() {
		String str = new String();
		str += "[";
		str += this.type;
		for (String attribute : this.attributes.keySet()) {
			Object value = this.attributes.get(attribute);
			if (value instanceof String) {
				if (value != null) {
					str += " ";
					str += attribute;
					str += ":";
					str += value;
				}
			} else if (value instanceof AVM) {
				if (!((AVM) value).isEmpty()) {
					str += " ";
					str += attribute;
					str += ":";
					str += value.toString();
					
				}
			} else if (value instanceof ArrayList) {
				for (AVM avm : (ArrayList<AVM>) value) {
					if (!avm.isEmpty()) {
						str += " ";
						str += attribute;
						str += ":";
						str += value.toString();
					}
				}
			}
		}
		str += "]";
		return str;
//		return "<" + this.type + " " + this.attributes.toString() + ">";
	}
	
}