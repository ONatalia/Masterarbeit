package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.nlu.AVPair;

public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU() {}; 
	
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
		if (this.avp != null) {
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("< ");
		sb.append(this.avp.toString());
		sb.append(" >");
		return super.toString() + " " + sb.toString();
	}

	public String toTEDviewXML() {
		double startTime = startTime();
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("' duration='");
		sb.append(Math.round((endTime() - startTime) * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("'> ");
		sb.append(this.avp.toString());
		sb.append(" </event>");
		return sb.toString();
	}
	
	/**
	 * Compares payload of two SemIUs.
	 * Note: I'm comparing string representations of lists, because .equals() of contents of lists seems to differ.
	 * @param siu the SemIU to compare against
	 * @return true if each SemIUs string representations of their payload (three array lists) are the same.
	 */
	public boolean samePayload(SemIU siu) {
		return (this.avp.equals(siu.avp));
	}

}
