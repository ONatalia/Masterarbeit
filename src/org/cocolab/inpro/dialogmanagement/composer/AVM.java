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
	public boolean equals(AVM a) {
		// TODO: make real checks for equality.  First define what they are.
		return this.equals(a);
	}

	@SuppressWarnings("unchecked")
	public boolean setAttribute(AVPair avp) {
		if (avp.getValue() instanceof String) {
			return setAttribute((String) avp.getAttribute(), (String) avp.getValue());
		} else if (avp.getValue() instanceof AVM) {
			return setAttribute((String) avp.getAttribute(), (AVM) avp.getValue());
		} else if (avp.getValue() instanceof ArrayList) {
			return setAttribute((String) avp.getAttribute(), (ArrayList<AVM>) avp.getValue());
		} else {
			throw new IllegalArgumentException("AVP value must be one of String, AVM or ArrayList.");
		}
	}
	
	private boolean setAttribute(String attribute, String value) {
		if (this.attributes.keySet().contains(attribute)) {
			if (this.attributes.get(attribute) == null || this.attributes.get(attribute).equals(value)) {
				this.attributes.put(attribute, value);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private boolean setAttribute(String attribute, AVM value) {
		if (this.attributes.keySet().contains(attribute)) {
			if (this.attributes.get(attribute) != null && this.attributes.get(attribute).getClass().equals(value.getClass())) {
				if (this.attributes.get(attribute) == null || this.attributes.get(attribute).equals(this.attributes.get(attribute))) {
					this.attributes.put(attribute, value);
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean setAttribute(String attribute, ArrayList<AVM> values) {
		ArrayList list = (ArrayList<AVM>) this.attributes.get(attribute);
		for (AVM a : values) {
			if (list.contains(a)) {
				return false;
			}
		}
		list.addAll(values);
		return true;
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
		if (avm.type != null && this.attributes.get(avm.type.replaceAll("_spec", "")) != null) {
			if (!this.setAttribute(avm.type.replaceAll("_spec", ""), avm)) {
//				throw new IllegalArgumentException("Attribute '" + avm.type.replaceAll("_spec", "") + "' with value '" + avm.toString() + "'cannot be set on AVM " + this.toString());
				return null;
			}
		} else {
			for (AVPair avp : avm.getAVPairs()) {
				if (avp.getValue() != null) {
					if (!this.setAttribute(avp)) {
//						throw new IllegalArgumentException("Attribute '" + avp.getAttribute() + "' with value '" + avp.getValue().toString() + "'cannot be set on AVM " + this.toString());
						return null;
					}
				}
			}
		}
		return this;
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

	public String toString() {
		return "<" + this.type + " " + this.attributes.toString() + ">";
	}
}