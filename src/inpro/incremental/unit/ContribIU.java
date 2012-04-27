package inpro.incremental.unit;

import inpro.nlu.AVPair;

import java.util.ArrayList;
import java.util.List;


import java.util.Collections;

/**
 * A contribution to larger dialogue/discourse.<br/>
 * Contributions initially live as a network of ContribIUs that can
 * be integrated with input and that ground output. These actions
 * are performed update mechanisms (e.g. rules).<br />
 * The payload of a contribution consists of an attribute-value
 * pair, with which input can be compared during integration.
 * Contributions further contain strings with which an SDS can confirm,
 * clarify or request information from the user, as well as a boolean
 * clarify to determine whether they should be clarified against
 * other contributions at all or just taken as the correct one
 * as soon as possible. Lastly, it has a confidence, based on the number of
 * grounding in- and outputs.
 * @author okko
 *
 */
public class ContribIU extends IU {

	/** This IU's payload - represented as an attribute-value pair*/
	protected AVPair contribution;
	/** This IU's confidence - initially 0.*/
	protected int confidence = 0;
	/** Boolean for whether this contribution should be clarified against others in case of ambiguous input. */
	protected boolean clarify;
	/** Boolean for whether new input can overwrite existing input. */
	protected boolean overwrite;

	public static final ContribIU FIRST_CONTRIB_IU = new ContribIU(); 

	/**
	 * Empty constructor.
	 */
	public ContribIU() {
		super(null, null);
	}

	/**
	 * Constructor with contribution and clarification parameters
	 * @param sll Same level link IU
	 * @param grin Grounded-in IU
	 * @param contribution AVPair describing this contribution
	 * @param clarify boolean denoting if this contribution should be disambiguated
	 */
	public ContribIU(IU sll, IU grin, AVPair contribution, boolean clarify, boolean overwrite) {
		super(sll, Collections.singletonList(grin));
		this.contribution = contribution;
		this.clarify = clarify;
		this.overwrite = overwrite;
	}
	
	/**
	 * Constructor with contribution and clarification parameters
	 * @param sll Same level link IU
	 * @param grin Grounded-in IU
	 * @param contribution AVPair describing this contribution
	 * @param clarify boolean denoting if this contribution should be disambiguated
	 */
	public ContribIU(IU sll, List<? extends IU> grin, AVPair contribution, boolean clarify, boolean overwrite) {
		super(sll, grin);
		this.contribution = contribution;
		this.clarify = clarify;
		this.overwrite = overwrite;
	}

	public boolean integratesWith(IU iu) {
		if (this.contribution == null)
			return false;
		List<AVPair> avps = new ArrayList<AVPair>();
		if (iu instanceof WordIU) {
			avps = ((WordIU) iu).getAVPairs();
		} else if (iu instanceof SemIU) {
			avps = ((SemIU) iu).getAVPairs();
		} else {
			return false;
		}
		if (avps == null)
			return false;
		for (AVPair avp : avps) {
			if (avp.equals(this.contribution)) {
				return true;
			} else if (avp.getAttribute().equals(this.contribution.getAttribute())) {
				if (this.contribution.getValue() == null ||
						this.contribution.getValue().equals("?") ||
						this.overwrite) {
					return true;
				}
			}
		}
		return false; 
	}
	
	/**
	 * Checks if this contribution should be clarified
	 * in case of ambiguous input
	 * @return true if so
	 */
	public boolean clarify() {
		return this.clarify;
	}

	/**
	 * Checks if this contribution can be grounded in
	 * new input even if already grounded.
	 * @return true if so
	 */
	public boolean overwrite() {
		return this.overwrite;
	}

	/** 
	 * Getter for this IU's contribution's attribute-value pair representation.
	 * @return the AVPair representation
	 */
	public AVPair getContribution() {
		return this.contribution;
	}
	
	public ContribIU getNext() {
//		return (ContribIU) this.nextSameLevelLink;
		return null;
	}

	public ContribIU getPrevious() {
		return (ContribIU) this.previousSameLevelLink;
	}

	public ContribIU getNextUp() {
		for (IU iu : this.grounds) {
			if (iu instanceof ContribIU) {
				return (ContribIU) iu;
			}
		}
		return null;
	}

	public ContribIU getNextDown() {
		for (IU iu : this.groundedIn) {
			if (iu instanceof ContribIU) {
				return (ContribIU) iu;
			}
		}
		return null;		
	}

	/**
	 * Checks if this contribution is grounded in at least one IU that is not another contribution
	 * @return true if so
	 */
	public boolean isIntegrated() {
		for (IU iu : this.groundedIn) {
			if (iu.getClass().equals(this.getClass())) {
				System.err.println(this.toString() + " is integrated.");
				return true;
			}
		}
		System.err.println(this.toString() + " is not integrated.");
		return false;
	}

	/**
	 * Primitive way of increasing the confidence of this contribution
	 * the more it integrates with input.
	 */
	@Override
	public void ground(IU iu) {
		super.ground(iu);
		this.confidence += 25;
		if (this.confidence > 100)
			this.confidence = 100;
	}
	
	/**
	 * Getter for this contribution's confidence.
	 * @return confidence
	 */
	public int confidence() {
		return this.confidence;
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
