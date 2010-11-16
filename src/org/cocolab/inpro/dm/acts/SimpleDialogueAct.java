package org.cocolab.inpro.dm.acts;

/**
 * A dialogue act with previsions for REQUEST, CLARIFY and GROUND acts
 * @author okko
 *
 */
public class SimpleDialogueAct extends AbstractDialogueAct {

	private Act act;
	public enum Act {
		REQUEST, CLARIFY, GROUND;
	}
	
	public SimpleDialogueAct(Act act) {
		this.act = act;		
	}

	/**
	 * Gets the Act for this RNLA.
	 * @return act
	 */
	public Act getAct() {
		return this.act;
	}

	@Override
	public String toString() {
		return this.act.toString();
	}

}
