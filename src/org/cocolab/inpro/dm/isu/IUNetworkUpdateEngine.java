package org.cocolab.inpro.dm.isu;

import org.cocolab.inpro.dm.isu.rule.AbstractRule;
import org.cocolab.inpro.dm.isu.rule.ClarifyNextInputRule;
import org.cocolab.inpro.dm.isu.rule.IntegrateNextInputRule;
import org.cocolab.inpro.dm.isu.rule.MarkContribIfIntegratesRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchDownRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchLeftRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchRightRule;
import org.cocolab.inpro.dm.isu.rule.MoveSearchUpRule;
import org.cocolab.inpro.dm.isu.rule.RequestMoreInfoRule;
import org.cocolab.inpro.dm.isu.rule.UnintegrateRevokedInputRule;

import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.unit.IUList;

public class IUNetworkUpdateEngine extends AbstractUpdateEngine {

	private IUNetworkInformationState is;
	private IUList<WordIU> input = new IUList<WordIU>();
	private IUList<DialogueActIU> output = new IUList<DialogueActIU>();

	public IUNetworkUpdateEngine() {
		rules.add(new UnintegrateRevokedInputRule());
		rules.add(new IntegrateNextInputRule());
		rules.add(new MarkContribIfIntegratesRule());
		rules.add(new MoveSearchDownRule());
		rules.add(new MoveSearchRightRule());
		rules.add(new MoveSearchLeftRule());
		rules.add(new MoveSearchUpRule());
		rules.add(new ClarifyNextInputRule());
		rules.add(new RequestMoreInfoRule());
		this.is = new IUNetworkInformationState();
	}

	public void addInput(IUList<WordIU> ius) {
		IUList<WordIU> oldWords = new IUList<WordIU>(this.input);
		ius.removeAll(oldWords);
		this.input = ius;
	}

	public void addInput(WordIU iu) {
		if (!this.input.contains(iu))
			this.input.add(iu);
	}

	public IUList<DialogueActIU> getOutput() {
		return this.output;
	}
	
	public IUList<DialogueActIU> getNewOutputAndClear() {
		IUList<DialogueActIU> newOutput = new IUList<DialogueActIU>(this.output);
		this.output.clear();
		return newOutput;
	}

	/**
	 * Applies update rules for input that needs (un-)integration.
	 * @param iu
	 */
	public void applyRules(WordIU iu) {
		this.addInput(iu);
		for (WordIU word : this.input) {
			System.err.println(word.toString());
			if (word.getAVPairs() == null) {  // Do not process word without semantics
				System.err.println("Skipping meaningless " + word.toString());
				continue;
			}
			if (word.isRevoked() && word.grounds().isEmpty()) { // Do not process revoked words that don't ground anything 
				System.err.println("Skipping revoked " + word.toString());
				continue;
			}
			if (!word.isRevoked() && !word.grounds().isEmpty()) { // Do not process added words that already ground something
				System.err.println("Skipping added " + word.toString());
				continue;
			}
			System.err.println("Processing " + word.toString());
			is.setNextInput(word);
			this.applyRules();
		}
	}

	/**
	 * Applies all rules that trigger to the information state, in
	 * the order in which they were loaded.
	 * If a rule changes the state when applied, the process is
	 * restarted from the top.
	 */
	@Override
	public void applyRules() {
		System.err.println(this.is.toString());
		boolean restart = false;
		for (AbstractRule r : rules) {
			if (r.triggers(is)) {
				if (r.apply(is)) {
					if (is.getNextOutput() != null)
						if (!this.output.contains(is.getNextOutput()))
							this.output.add(is.getNextOutput());
					restart = true;
					break;
				}
			}
		}
		if (restart) {
			this.applyRules();
		} else {
			System.err.println("No rules applied.");
		}
	}


}
