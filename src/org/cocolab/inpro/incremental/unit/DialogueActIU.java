package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.dm.RNLA;
import org.cocolab.inpro.nlu.AVPair;

public class DialogueActIU extends IU {

	public static final DialogueActIU FIRST_DA_IU = new DialogueActIU(); 
	
	private RNLA act;

	@SuppressWarnings("unchecked")
	public DialogueActIU() {
		this(FIRST_DA_IU, Collections.EMPTY_LIST, null);
	}

	public DialogueActIU(IU sll, List<IU> groundedIn, AVPair avp) {
		super(sll, groundedIn);
	}

	public DialogueActIU(IU sll, IU groundedIn, AVPair avp) {
		super(sll, Collections.singletonList(groundedIn));
	}
	
	private boolean isEmpty() {
		return false;
	}

	/**
	 * Compares payload of two DialogueActIUs.
	 * @param siu the DialogueActIU to compare against
	 * @return true if each DialogueActIUs string representations of their payload are the same.
	 */
	public boolean samePayload(DialogueActIU siu) {
		return this.toPayLoad().equals(siu.toPayLoad());
	}

	@Override
	public String toPayLoad() {
		String payLoad;
		if (this == FIRST_DA_IU) 
			payLoad = "Root DialogueActIU";
		else if (isEmpty())
			payLoad = "empty";
		else
			payLoad = this.act.toString();
			
		return "<" + payLoad + ">";
	}

}
