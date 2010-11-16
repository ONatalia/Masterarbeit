package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.dm.acts.AbstractDialogueAct;
import org.cocolab.inpro.dm.acts.SimpleDialogueAct;

public class DialogueActIU extends IU {

	public static final DialogueActIU FIRST_DA_IU = new DialogueActIU(); 
	
	private AbstractDialogueAct act;

	@SuppressWarnings("unchecked")
	public DialogueActIU() {
		this(FIRST_DA_IU, Collections.EMPTY_LIST, null);
	}

	public DialogueActIU(IU sll, List<IU> groundedIn, SimpleDialogueAct act) {
		super(sll, groundedIn);
		this.act = act;
	}

	public DialogueActIU(IU sll, List<IU> groundedIn, AbstractDialogueAct act) {
		super(sll, groundedIn);
		this.act = act;
	}

	public DialogueActIU(IU sll, IU groundedIn, AbstractDialogueAct act) {
		super(sll, Collections.singletonList(groundedIn));
		this.act = act;
	}
	
	private boolean isEmpty() {
		return false;
	}

	/**
	 * @return the RNLA act of this IU.
	 */
	public AbstractDialogueAct getAct() {
		return this.act;
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
