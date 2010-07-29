package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.nlu.AVPair;

public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU(); 
	
	private AVPair avp;

	@SuppressWarnings("unchecked")
	public SemIU() {
		this(FIRST_SEM_IU, Collections.EMPTY_LIST, null);
	}

	public SemIU(IU sll, List<IU> groundedIn, AVPair avp) {
		super(sll, groundedIn);
		this.avp = avp;
	}

	public SemIU(IU sll, IU groundedIn, AVPair avp) {
		super(sll, Collections.singletonList(groundedIn));
		this.avp = avp;
	}

	public AVPair getAVPair() {
		return this.avp;
	}

	public boolean isEmpty() {
		return (this.avp == null);
	}
	
	/**
	 * Compares payload of two SemIUs.
	 * @param siu the SemIU to compare against
	 * @return true if each SemIUs string representations of their payload (AVPairs)
	 */
	public boolean samePayload(SemIU siu) {
		return (this.avp.equals(siu.avp));
	}

	@Override
	public String toPayLoad() {
		String payLoad;
		if (this == FIRST_SEM_IU) 
			payLoad = "Root SemIU";
		else if (isEmpty())
			payLoad = "empty";
		else
			payLoad = avp.toString();
			
		return "<" + payLoad + ">";
	}

}
