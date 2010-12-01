package org.cocolab.inpro.incremental.unit;

import java.util.List;

import org.cocolab.inpro.nlu.AVPair;

import edu.emory.mathcs.backport.java.util.Collections;

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

	protected AVPair contribution;
	protected int confidence = 0;
	protected boolean clarify = false;
	protected String requestString;
	protected String clarificationString;
	protected String groundingString;

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
	@SuppressWarnings("unchecked")
	public ContribIU(IU sll, IU grin, AVPair contribution, boolean clarify) {
		this(sll, Collections.singletonList(grin), contribution, clarify, null, null, null);
	}

	/**
	 * Constructor with contribution parameter
	 * @param sll Same level link IU
	 * @param grin Grounded-in IU
 	 * @param contribution AVPair describing this contribution
 	 * @param clarify boolean denoting if this contribution should be disambiguated
	 * @param requestString the default request string for utterance construction
	 * @param clarificationString the default clarification string for utterance construction
	 * @param groundingString the default grounding string for utterance construction

	 */
	@SuppressWarnings("unchecked")
	public ContribIU(IU sll, IU grin, AVPair contribution, boolean clarify,
			String requestString, String clarificationString, String groundingString) {
		this(sll, Collections.singletonList(grin), contribution, clarify, requestString, clarificationString, groundingString);
	}

	/**
	 * Full constructor with contribution, clarification and utterance string
	 * descriptors.
	 * @param sll Same level link IU
	 * @param grin Grounded-in IU
	 * @param contribution AVPair describing this contribution
	 * @param clarify boolean denoting if this contribution should be disambiguated
	 * @param requestString the default request string for utterance construction
	 * @param clarificationString the default clarification string for utterance construction
	 * @param groundingString the default grounding string for utterance construction
	 */
	public ContribIU(IU sll, List<? extends IU> grin, AVPair contribution, boolean clarify,
			String requestString, String clarificationString, String groundingString) {
		super(sll, grin);
		this.contribution = contribution;
		this.clarify = clarify;
		this.requestString = requestString;
		this.clarificationString = clarificationString;
		this.groundingString = groundingString;
	}

	/**
	 * Constructor with contribution and clarification parameters
	 * @param sll Same level link IU
	 * @param grin Grounded-in IUs
	 * @param contribution AVPair describing this contribution
	 * @param clarify boolean denoting if this contribution should be disambiguated
	 */
	public ContribIU(IU sll, List<? extends IU> grin, AVPair contribution) {
		this(sll, grin, contribution, false, null, null, null);
	}
	
	public boolean integratesWith(IU iu) {
		if (this.contribution == null)
			return false;
		if (iu instanceof WordIU) {
			if (((WordIU) iu).getAVPairs() == null) {
				return false;
			}
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
	
	/**
	 * Checks if this contribution needs clarification (or if a DM can assume
	 * that the first match of this type is correct.)
	 * @return true if so
	 */
	public boolean clarify() {
		return this.clarify;
	}
	
	public AVPair getContribution() {
		return this.contribution;
	}
	
	public String getClarificationString() {
		if (this.clarificationString == null) {
			if (this.contribution != null) {
				return this.contribution.getAttribute();
			}
		}
		return this.clarificationString;
	}

	public String getGroundingString() {
		if (this.groundingString == null) {
			for (IU iu : this.groundedIn()) {
				if (iu instanceof WordIU) {
					return ((WordIU) iu).getWord() + " ";
				}
			}			
		}
		return this.groundingString;
	}

	public String getRequestString() {
		if (this.requestString == null)
			return "Wie kann ich Ihnen helfen?";
		return this.requestString;
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
	
	/**
	 * Getter for this contributions end time (which is the latest
	 * end time of any known iu to ground it.
	 */
	@Override
	public double endTime() {
		if ((groundedIn != null) && (groundedIn.size() > 0)) {
			double time = 0;
			for (IU iu : this.groundedIn()) {
				if (iu.endTime() != Double.NaN) {
					if (iu.endTime() > time)
						time = iu.endTime();
				}
			}
			return time;
		} else {
			return Double.NaN;
		}
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
