package org.cocolab.inpro.incremental.unit;

import java.util.List;

import org.cocolab.inpro.nlu.AVPair;

import edu.emory.mathcs.backport.java.util.Collections;

public class ContribIU extends IU {

	private AVPair contribution;

	public static final ContribIU FIRST_CONTRIB_IU = new ContribIU(); 

	public ContribIU() {
		super(null, null);
	}

	@SuppressWarnings("unchecked")
	public ContribIU(IU sll, IU grin, AVPair contribution) {
		this(sll, Collections.singletonList(grin), contribution);
	}

	public ContribIU(IU sll, List<? extends IU> grin, AVPair contribution) {
		super(sll, grin);
		this.contribution = contribution;
	}
	
	public boolean integratesWith(IU iu) {
		if (this.contribution == null)
			return false;
		if (iu instanceof WordIU) {
			for (AVPair avp: ((WordIU) iu).getAVPairs()) {
				if (avp.equals(this.contribution)) {
					return true;
				} else if (avp.getAttribute().equals(this.contribution.getAttribute())) {
					if (this.contribution.getValue() == null || this.contribution.getValue().equals("?")) {
						// Overwrites an inital "?" value...
//						this.contribution.setValue(avp.getValue());
						return true;
					}
				}
			}
		} else if (iu instanceof SemIU) {
			AVPair avp = ((SemIU) iu).getAVPair();
			if (avp.equals(this.contribution)) {
				return true;
			} else if (avp.getAttribute().equals(this.contribution.getAttribute())) {
				if (this.contribution.getValue() == null) {
					return true;
				} else if (this.contribution.getValue().equals("?")) {
					// Overwrites an inital "?" value...
//					this.contribution.setValue(avp.getValue());
					return true;
				}
			}
		}			
		return false; 
	}

	@Override
	public String toPayLoad() {
		if (this.contribution == null)
			return "null";
		return contribution.toString();
	}
	
	public boolean equals(ContribIU iu) {
		return this.contribution.equals(iu.contribution);
	}

}
