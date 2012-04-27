package inpro.incremental.unit;

import inpro.nlu.AVPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU(); 
	
	private List<AVPair> avps = new ArrayList<AVPair>();

	@SuppressWarnings("unchecked")
	public SemIU() {
		this(FIRST_SEM_IU, Collections.EMPTY_LIST, null);
	}

	public SemIU(IU sll, List<IU> groundedIn, AVPair avp) {
		super(sll, groundedIn);
		this.avps.add(avp);
	}

	public SemIU(IU sll, IU groundedIn, AVPair avp) {
		super(sll, Collections.singletonList(groundedIn));
		this.avps.add(avp);
	}

	public SemIU(IU sll, IU groundedIn, List<AVPair> avps) {
		super(sll, Collections.singletonList(groundedIn));
		this.avps = avps;
	}

	public AVPair getAVPair() {
		return this.avps.get(0);
	}
	
	public List<AVPair> getAVPairs() {
		return this.avps;
	}

	public boolean isEmpty() {
		return this.avps.isEmpty();
	}
	
	/**
	 * Compares payload of two SemIUs.
	 * @param siu the SemIU to compare against
	 * @return true if each SemIUs string representations of their payload (AVPairs)
	 */
	public boolean samePayload(SemIU siu) {
		return (this.avps.equals(siu.avps));
	}

	@Override
	public String toPayLoad() {
		String payLoad;
		if (this == FIRST_SEM_IU) 
			payLoad = "Root SemIU";
		else if (isEmpty())
			payLoad = "empty";
		else
			payLoad = avps.toString();
		return "<" + payLoad + ">";
	}

}
