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
	public String type;
	public HashMap<String, Object> attributes;
//	public ArrayList<String> attributes;
//	public HashMap<String, String> stringAttributes;
//	public HashMap<String, AVM> AVMAttributes;
//	public HashMap<String, ArrayList<AVM>> listAttributes;
//	public HashMap<String, ArrayList<AVM>> fixedListAttributes;

	/**
	 * Empty constructor.
	 */
	AVM() {}

	/**
	 * String type constructor.
	 */
	AVM(String type) {
		this.type = type;
		this.setEmptyAttributesFromType(this.type);
	}

	/**
	 * Copy constructor.
	 * @param avm - the AVM to copy.
	 */
	protected AVM (AVM avm) {
		this.type = new String(avm.type);
		this.attributes = new HashMap<String, Object>(avm.attributes);
	}

	private void setEmptyAttributesFromType(String type) {
//		System.err.println("new " + type + " avm");
		this.attributes = new HashMap<String, Object>();
		if (type.equals("tile")) {
			this.attributes.put("name", null);
			this.attributes.put("color", null);
			this.attributes.put("label", null);
			this.attributes.put("loc", new AVM("loc_spec"));
			ArrayList<AVM> list = new ArrayList<AVM>();
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			list.add(new AVM("rel_loc_spec"));
			this.attributes.put("rel_loc", list);
		} else if (type.equals("field")) {
			this.attributes.put("color", null);
			this.attributes.put("loc", new AVM("loc_spec"));
			ArrayList<AVM> list = new ArrayList<AVM>(4);
			list.add(new AVM("rel_loc_spec"));
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
		} else {
			throw new IllegalArgumentException("Unknown AVM type: " + type);
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
		int hash;
		hash = 37 * this.type.hashCode() * this.attributes.hashCode();
		return hash;
	}

	/**
	 * Attempts unifying this AVM with the input to this method.
	 * @param a - an AVM to unify with this AVM.
	 * @return a new unified AVM if unification was successful,
	 * null otherwise.
	 */
	public AVM unify(AVM avm) {
		for (AVPair avp : avm.getAVPairs()) {
			if (!this.setAttribute(avp)) {
				return null;
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
	
	@SuppressWarnings("unchecked")
	public boolean setAttribute(AVPair avp) {
		String attribute = avp.getAttribute();
		Object value = avp.getValue();
		if (this.attributes.keySet().contains(attribute)) {
			if (value instanceof String) {
				if (this.attributes.get(attribute) == null) {
					this.attributes.put(attribute, (String) value);
					return true;
				} else if (this.attributes.get(attribute).equals((String) value)) {
					return true;
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
						if (avm1.unifies(avm2)) {
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
	
	public ArrayList<AVPair> getAVPairs() {
		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		for (String attribute : this.attributes.keySet()) {
			/* Use null? */
//			avps.add(new AVPair(attribute, this.attributes.get(attribute)));
			/* Maybe use non-null? */
			if (this.attributes.get(attribute) != null) {
				avps.add(new AVPair(attribute, this.attributes.get(attribute)));	
			}
			
		}
		return avps;
	}
	
	public boolean isEmpty() {
		return false;
	}

	/**
	 * Builds a string representation of the AVM with nested brackets.
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
				str += attribute;
				str += ":";
				for (AVM avm : (ArrayList<AVM>) value) {
					str += avm.toString();
					str += " ";
				}
			}
		}
		str += "]";
		return str;
	}
	
}