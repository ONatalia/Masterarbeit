package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.dm.isu.rule.AbstractRule;
import org.cocolab.inpro.dm.isu.rule.ClarifyNextInputRule;
import org.cocolab.inpro.dm.isu.rule.ConfirmLastOutputRule;
import org.cocolab.inpro.dm.isu.rule.DisconfirmLastOutputRule;
import org.cocolab.inpro.dm.isu.rule.IntegrateNextInputRule;
import org.cocolab.inpro.dm.isu.rule.MarkContribIfIntegratesRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchDownRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchLeftRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchRightRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchUpRule;
import org.cocolab.inpro.dm.isu.rule.RequestMoreInfoRule;
import org.cocolab.inpro.dm.isu.rule.UnintegrateRevokedInputRule;

import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.SemIU;

/**
 * A rule-based update engine that keeps a set of rules and an information
 * state to apply its rules to. Keeps a list output IU edits to
 * interface with a dialogue manager.
 * @author okko
 *
 */
public class IUNetworkUpdateEngine extends AbstractUpdateEngine {

	/** The information state to update*/
	private IUNetworkInformationState is;
	/** The IU edits to return on request */
	private List<EditMessage<DialogueActIU>> edits = new ArrayList<EditMessage<DialogueActIU>>();
	/** The logger */
	private Logger logger;

	/**
	 * A simple constructor initiating an empty information state and 
	 * a generic top-down search update mechanism producing GROUND,
	 * CLARIFY and REQUEST output. Can handle YesNo input for system-
	 * and user-self-correction. Sensitive to input revokes and output
	 * commits.
	 */
	public IUNetworkUpdateEngine(IUNetworkDomainUtil u) {
		rules.add(new UnintegrateRevokedInputRule());			// REVOKE output grounded in revoked input
		rules.add(new MarkContribIfIntegratesRule());			// Remember the ContribIU we're currently looking at if it integrates input
		rules.add(new MoveSearchDownRule());					// Look further down in the contribution network (if we haven't yet.)
		rules.add(new MoveSearchRightRule());					// Look further right in the contribution network (if we haven't yet.)
		rules.add(new MoveSearchLeftRule());					// Look further left in the contribution network
		rules.add(new MoveSearchUpRule());						// Look further up in the contribution network
		rules.add(new IntegrateNextInputRule());				// Integrate marked input, if one was marked
		rules.add(new ConfirmLastOutputRule());					// If a Yes didn't explicitly integrate, use it to confirm last output (do not clarify it)
		rules.add(new DisconfirmLastOutputRule());				// If a No didn't explicitly integrate, use it to dis-confirm last output (do not clarify it)
		rules.add(new ClarifyNextInputRule());					// Clarify next input, if more than one was marked
		rules.add(new RequestMoreInfoRule());					// Request more input about current focus contribution
		this.is = new IUNetworkInformationState(u.getContributions());
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
		this.is.setLogger(this.logger);
	}
	
	/**
	 * Getter method for output edits. Returns and clears current list of edit messages.
	 * @return all new output DialogueActIUs edit messages
	 */
	public List<EditMessage<DialogueActIU>> getNewEdits() {
		List<EditMessage<DialogueActIU>> ret = new ArrayList<EditMessage<DialogueActIU>>();
		ret.addAll(this.edits);
		this.edits.clear();
		return ret;
	}

	/**
	 * Getter for the information state.
	 * @return
	 */
	public IUNetworkInformationState getInformationState() {
		return this.is;
	}

	/**
	 * Sets the input word on the information state and calls the update rules.
	 * @param iu the input word IU
	 */
	public void applyRules(SemIU iu) {
		String status = iu.isRevoked() ? "revoked" : "added";
		if (iu.getAVPair() == null) {
			logger.info("Skipping meaningless " + status + " sem " + iu.toString());
		} else {
			logger.info("Processing " + status + " sem " + iu.toString());
			is.setNextInput(iu);
			this.applyRules();
		}
	}

	/**
	 * Applies all rules that trigger to the information state, in
	 * the order in which they were loaded.
	 * If applying a rule changes the information state, the process is
	 * restarted from the top and repeated until no more changes take effect.
	 * (The risk of infinite recursion must be tested by the rule designer.)
	 */
	@Override
	public void applyRules() {
		boolean restart = false;
		for (AbstractRule r : rules) {
			if (r.triggers(is)) {
				restart = r.apply(is);
				if (restart) {
					this.edits.addAll(is.getNewEdits());
					break;
				}
			}
		}
		if (restart) {
			this.applyRules();
		}
	}

}
