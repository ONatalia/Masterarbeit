package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.dm.acts.AbstractDialogueAct;
import org.cocolab.inpro.dm.acts.ClarifyDialogueAct;
import org.cocolab.inpro.dm.acts.GroundDialogueAct;
import org.cocolab.inpro.dm.acts.InformDialogueAct;
import org.cocolab.inpro.dm.acts.RequestDialogueAct;

public class DialogueActIU extends IU {

	public static final DialogueActIU FIRST_DA_IU = new DialogueActIU(); 
	
	private AbstractDialogueAct act;

	public DialogueActIU() {
		this(FIRST_DA_IU, Collections.<IU>emptyList(), null);
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
	 * @return the dialogue act of this IU.
	 */
	public AbstractDialogueAct getAct() {
		return this.act;
	}
	
	/**
	 * Getter for a string representation of the utterance associated with this IU's act.
	 * @return
	 */
	public String getUtterance() {
		if (this.act instanceof RequestDialogueAct) {
			return ((ContribIU) this.groundedIn.get(0)).getRequestString();
		} else if (this.act instanceof ClarifyDialogueAct) {
			return ((ContribIU) this.groundedIn.get(0)).getClarificationString();
		} else if (this.act instanceof GroundDialogueAct) {
			return ((ContribIU) this.groundedIn.get(0)).getGroundingString();
		} else if (this.act instanceof InformDialogueAct) {
			return ((InformDialogueAct) this.act).getUtterance();
		} else {
			return "";
		}
	}

	/**
	 * Compares payload of two DialogueActIUs.
	 * @param siu the DialogueActIU to compare against
	 * @return true if each DialogueActIUs string representations of their payload are the same.
	 */
	public boolean samePayload(DialogueActIU siu) {
		return this.toPayLoad().equals(siu.toPayLoad());
	}
	
	public void perform() {
		this.commit();
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
