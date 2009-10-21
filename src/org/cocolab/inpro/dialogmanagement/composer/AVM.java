package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class representing an AVM with methods for equality and unification.
 * Construction
 * @author okko
 */
public class AVM {

	// Implementing classes must define these in their constructors.
	public Map<String, Object> attributes = new HashMap<String, Object>();
	protected String type;
	
	AVM() {
	}

	AVM(AVPair avp) {
		try {
			this.setAttribute(avp);			
		} catch (IllegalArgumentException e) {}
	}

	/**
	 * @param a - an AVM to compare this AVM with.
	 * @return true if they are the same.
	 */
	public boolean equals(AVM a) {
		return this.equals(a);
	}

	@SuppressWarnings("unchecked")
	public AVM setAttribute(AVPair avp) {
		if (this.attributes.keySet().contains(avp.getAttribute())) {
			if (this.attributes.get(avp.getAttribute()) instanceof ArrayList) {
				ArrayList<AVM> list = (ArrayList<AVM>) this.attributes.get(avp.getAttribute());
				ArrayList<AVM> listToAdd = (ArrayList<AVM>) avp.getValue();
				for (AVM a : listToAdd) {
					if (!list.contains(a)) {
						list.add(a);
					} else {
						throw new IllegalArgumentException("AVPair attribute '" + avp.getAttribute() + "' cannot be set for this AVM.  Value already exists.");						
					}
				}
			} else {
				if (this.attributes.get(avp.getAttribute()) == null || this.attributes.get(avp.getAttribute()).equals(this.attributes.get(avp.getAttribute()))) {
					this.attributes.put(avp.getAttribute(), avp.getValue());
				} else {
					throw new IllegalArgumentException("AVPair attribute '" + avp.getAttribute() + "' cannot be set for this AVM.  Value already exists.");
				}
		 	}
		} else {
			throw new IllegalArgumentException("AVPair attribute '" + avp.getAttribute() + "' does not match AVM type '" + this.type + "'.");
		}
		return this;
	}

	/**
	 * Attempts unifying this AVM with the input to this method.
	 * Unification succeeds iff both AVMs share the same attributes and their values do not conflict (i.e. for simple values they are either null in at least one or identical, for complex/list values they can simply be added.)
	 * @param a - an AVM to unify with this AVM.
	 * @return <b>b</b> - an AVM, which is the unification of <b>b</b> and this AVM if unification was successful, null otherwise.
	 */ 
	public AVM unify(AVM a) {
		// TODO: implement by setting attributes on this AVM from a.getAVPairs();
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
		return "[ " + this.type + " " + this.attributes.toString() + "]";
	}
}