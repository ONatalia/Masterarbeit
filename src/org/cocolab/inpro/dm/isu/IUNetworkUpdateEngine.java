package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

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
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.unit.IUList;

/**
 * A rule-based update engine that keeps a set of rules and an information
 * state to apply its rules to. Keeps a list of inputs and outputs to
 * interface with a dialogue manager.
 * @author okko
 *
 */
public class IUNetworkUpdateEngine extends AbstractUpdateEngine {

	private IUNetworkInformationState is;
	private IUList<WordIU> input = new IUList<WordIU>();
//	private IUList<DialogueActIU> output = new IUList<DialogueActIU>();
	private List<EditMessage<DialogueActIU>> edits = new ArrayList<EditMessage<DialogueActIU>>();

	/**
	 * A simple constructor initiating an empty information state and 
	 * a generic top-down search update mechanism producing GROUND,
	 * CLARIFY and REQUEST output.
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

	/**
	 * Getter method for output.
	 * @return all output DialogueActIUs
	 */
//	public IUList<DialogueActIU> getOutput() {
//		return this.output;
//	}

	/**
	 * Getter method for output edits.
	 * @return all output DialogueActIUs edit messages
	 */
	public List<EditMessage<DialogueActIU>> getEdits() {
		return this.edits;
	}

	/**
	 * 
	 * @return
	 */
	public IUNetworkInformationState getInformationState() {
		return this.is;
	}

	/**
	 * Adds a word to input then applies update rules for any input
	 * word that needs (un-)integration.
	 * @param iu
	 */
	public void applyRules(WordIU iu) {
		if (!this.input.contains(iu)) 
			this.input.add(iu);
		System.err.println("Processing new word " + iu.toString());
		is.setNextInput(iu);
		this.applyRules();			
		// TODO: only process words that need processing (remove once integrated uniquely).
		IUList<WordIU> keep = new IUList<WordIU>();
		for (WordIU word : this.input) {
			System.err.println(word.toString());
			if (word.getAVPairs() == null) {  // Do not process word without semantics
				System.err.println("Skipping meaningless " + word.toString());
				continue;
			} else if (word.isRevoked() && word.grounds().isEmpty()) { // Do not process revoked words that don't ground anything 
				System.err.println("Skipping revoked but irrelevant " + word.toString());
				continue;
			} else if (!word.isRevoked() && !word.grounds().isEmpty()) { // Do not process added words that already ground something
				System.err.println("Skipping added but integrated " + word.toString());
				continue;
			} else { // Process all other words
				keep.add(word);
				System.err.println("Processing new word " + word.toString());
				is.setNextInput(word);
				this.applyRules();				
			}
		}
		this.input = keep;
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
				if (r.apply(is)) {
					this.edits = is.getEdits();
//					if (is.getNextOutput() != null)
//						if (!this.output.contains(is.getNextOutput()))
//							this.output.add(is.getNextOutput());
					restart = true;
					break;
				}
			}
		}
		if (restart) {
			this.applyRules();
		} else {
			System.err.println("No rules applied. Stopping.");
		}
	}


}
