package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cocolab.inpro.dm.acts.AbstractDialogueAct;
import org.cocolab.inpro.dm.acts.ClarifyDialogueAct;
import org.cocolab.inpro.dm.acts.GroundDialogueAct;
import org.cocolab.inpro.dm.acts.RequestDialogueAct;
import org.cocolab.inpro.dm.acts.UndoDialogueAct;
import org.cocolab.inpro.dm.isu.rule.AbstractIUNetworkRule;
import org.cocolab.inpro.incremental.unit.ContribIU;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;

// FIXME: at the first unintegrate rule doesn't reintegrate when 'no' is revoked
// FIXME: contributions that are grounded after a commit, not an add do not overwrite.
// FIXME: Focus, move right/down when necessary, i.e. system-initiated topic/focus-changes when current focus is fully integrated.
// TODO: YesNo - make integration/unintegration separate rules, with different scopes: 1. self-correction/confirmation, 2. performed output, 3. explicit confirmation, 4. partial clarification
// TODO: AM.signalListeners() - implement 'done' update rules
// TODO: Make IU queries generic, move IU-specific queries to IU.java

/**
 * An information state consisting of a network of ContribIU contributions that can integrate with new input.
 * Provides query methods for preconditions of update rule triggers and update methods for applying
 * update rule effects.
 * Update rules specify the manner in which the IS is searched for nodes to integrate input with. They can make recourse
 * to these query and update methods, which in turn query the IU network or a number of special purpose placeholders for
 * currently looked-at contributions, which contributions have been visited during recent search, which contributions
 * integrate new input and what the nextOutput is. 
 * @author okko
 *
 */
public class IUNetworkInformationState extends AbstractInformationState implements AbstractIUNetworkRule.Triggers, AbstractIUNetworkRule.Effects {

	/** The next input variable - a WordIU that represents the next input word that was added or revoked to process */
	private WordIU nextInput;
	/** The contributions network - a List of (ideally) networked ContribIU representing the total set of available nodes with which new input can be integrated */
	private IUList<ContribIU> contributions = new IUList<ContribIU>();
	/** The root ContribIU that represents the first node in the contributions network. */
	private ContribIU root;
	/** The focus ContribIU that represents the last node in the contributions network to integrate. */
	private ContribIU focus;
	/** A variable that holds the contribution currently looked at while searching for contributions to integrate new input with. */
	private ContribIU currentContrib;
	/** The list of contributions with which input can be integrated. Filled during search and cleared afterwards. */
	private IUList<ContribIU> integrateList = new IUList<ContribIU>();
	/** A list of contributions that were visited during search. Filled during search and cleared afterwards.  */
	private IUList<ContribIU> visited = new IUList<ContribIU>();
	/** The DialogueActIU representing nextOutput added or revoked. Remains 'current' output until a new one is created. */
	private DialogueActIU nextOutput;
	/** A list of edit messages produced as a result of (all previous and present) changes to the IS. */
	private List<EditMessage<DialogueActIU>> outputEdits = new ArrayList<EditMessage<DialogueActIU>>();
	/** A logger */
	private Logger logger;

	/**
	 * Generic constructor for an IUNetworkInformationState
	 */
	public IUNetworkInformationState() {}
	
	/**
	 * Constructor building contributions network from a list of
	 * ContribIU's. Root is assumed to be the first one if none have
	 * a payload designating it as root.
	 * @param contributions the list
	 */
	public IUNetworkInformationState(IUList<ContribIU> contributions) {
		this.contributions = contributions;
		for (ContribIU iu : this.contributions) {
			if (iu.getContribution().equals("root:true")) {
				this.root = iu;
				break;
			}
		}
		// Root is the first known if not specified
		if (this.root == null) 
			this.root = this.contributions.get(0);
		// Always start search and focus at root
		this.currentContrib = this.root;
		this.focus = this.root;
		// Generate request output about next focus.
		this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, this.root, new RequestDialogueAct());
		this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.nextOutput));
	}
	
	/**
	 * Constructor building contributions network from a
	 * single root IU's grounded in links.
	 * @param root
	 */
	public IUNetworkInformationState(ContribIU root) {
		this.root = root;
		this.addGrinIUs(root);

	}

	/**
	 * Convenience method that recursively adds any new IUs grounded in a given IU
	 * to the contributions network.
	 * @param iu the IU and its GrIns to add to the contribution network.
	 */
	private void addGrinIUs(ContribIU iu) {
		if (!this.contributions.contains(iu)) {
			this.contributions.add(iu);
			for (IU grin : iu.grounds()) {
				if (grin instanceof ContribIU) {
					this.addGrinIUs((ContribIU) grin);
				}
			}
		}
	}

	/** 
	 * Sets the logger
	 * @param logger the logger to log to.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	/**
	 * Setter method for the IS's next input to process.
	 * @param word the next word input to process.
	 */
	public void setNextInput(WordIU word) {
		this.nextInput = word;
	}

	/**
	 * Getter for the next output to perform.
	 * @return nextOutput
	 */
	public DialogueActIU getNextOutput() {
		return this.nextOutput;
	}

	/**
	 * Getter for the current focus
	 * @return focus
	 */
	public ContribIU getFocus() {
		return this.focus;
	}

	/**
	 * Getter for the next output to perform.
	 * @return
	 */
	public List<EditMessage<DialogueActIU>> getNewEdits() {
		List<EditMessage<DialogueActIU>> ret = new ArrayList<EditMessage<DialogueActIU>>();
		ret.addAll(outputEdits);
		this.outputEdits.clear();
		return ret;
	}

	/**
	 * Getter for this IS's contributions network
	 * @return contributions the list of ContribIUs 
	 */
	public IUList<ContribIU> getContributions() {
		return this.contributions;
	}
	
	/**
	 * Getter method returning the IS's current contribution
	 * @return currentContrib this IS's current ContribIU
	 */
	public ContribIU getCurrentContrib() {
		if (this.currentContrib == null)
			return this.root;
		return this.currentContrib;
	}

	/**
	 * A convenience method to look through currently active contributions
	 * returning whatever integrated last.
	 * @return the last contribution to integrate
	 */
	private ContribIU getLastIntegrated() {
		double et = 0;
		ContribIU ret = this.root;
		for (ContribIU iu : this.contributions) {
			for (IU wiu : iu.groundedIn()) {
				if (wiu instanceof WordIU) {
					if (wiu.endTime() > et) {
						et = wiu.endTime();
						ret = iu;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's next input was revoked.
	 * @return true if so
	 */
	@Override
	public boolean nextInputIsRevoked() {
		if (this.nextInput == null)
			return false;
		return this.nextInput.isRevoked();
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * The current contribution is added to the list of potential candidates for integration.
	 * @return true if successful
	 */
	@Override
	public boolean addCurrentContribToIntegrateList() {
		if (this.integrateList.contains(this.getCurrentContrib()))
			return false;
		this.integrateList.add(this.getCurrentContrib());
		return true;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
 	 * Moves the current contribution right to its next SLL contribution. 
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribRight() {
		if (this.nextInput == null)
			return false;
		if (this.currentContrib.equals(this.focus) && 
				!this.integrateList.isEmpty()) {
			// continue search only if nothing below focus integrated
			return false;
		}
		for (ContribIU iu : this.contributions) {
			if (iu.getSameLevelLink() != null) {
				if (iu.getSameLevelLink().equals(this.currentContrib)) {
					this.currentContrib = iu;
					if (!this.visited.contains(this.currentContrib)) {
						this.visited.add(this.currentContrib);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Moves the current contribution up to its first grounding contribution. 
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribUp() {
		if (this.nextInput == null)
			return false;
		if (this.currentContrib.equals(this.focus) && 
				!this.integrateList.isEmpty()) {
			// continue search only if nothing below focus integrated
			return false;
		}
		for (IU iu : this.currentContrib.grounds()) {
			if (iu instanceof ContribIU) {
				this.currentContrib = (ContribIU) iu;
				if (!this.visited.contains(this.currentContrib)) {
					this.visited.add(this.currentContrib);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Attempts moving the current contrib to the left (SLL).
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribLeft() {
		if (this.nextInput == null)
			return false;
		if (this.currentContrib.equals(this.focus) && 
				!this.integrateList.isEmpty()) {
			// continue search only if nothing below focus integrated
			return false;
		}
		if (this.currentContrib.getSameLevelLink() != null) {
			this.currentContrib = (ContribIU) this.getCurrentContrib().getSameLevelLink();
			if (!this.visited.contains(this.currentContrib)) {
				this.visited.add(this.currentContrib);
				return true;
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Attempts moving the current contrib down to the first grounded-in contribution.
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribDown() {
		if (this.nextInput == null)
			return false;
		for (IU iu : this.getCurrentContrib().groundedIn()) {
			if (iu instanceof ContribIU) {
				this.currentContrib = (ContribIU) iu;
				if (!this.visited.contains(this.currentContrib)) {
					this.visited.add(this.currentContrib);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Removes grin links between input and any contributions grounded in it.
	 * Also re-grounds previous input.
	 * @true if successful
	 */
	@Override
	public boolean unintegrateNextInput() {
		boolean restart = false;
		for (ContribIU iu : this.contributions) {
			// Find what contributions are grounded in revoked input 
			if (iu.groundedIn().contains(this.nextInput)) {
				if (this.nextInput.getAVPairs().get(0).equals("bool:false") && (iu.getContribution().equals("undo:true"))) {
					// if a 'no' is being unitegrated
					// the contribution that 'no' referred to
					// (attached to a glue-contribution with undo:true),
					// should be re-integrated
					// and the 'no' itself unintegrated (revoked for now, removed below).
					this.focus = (ContribIU) iu.groundedIn().get(0);
					iu.removeGrin(this.nextInput);
					iu.revoke();
					for (IU input : iu.groundedIn()) {
						// reintegrate by assigning old input to nextInput and restarting rules.
						if (!(input instanceof ContribIU)) {
							this.nextInput = (WordIU) input;
							restart = true;
						}
					}
				} else if (iu.getContribution().equals("clarify:true")) {
					iu.removeGrin(this.nextInput);
					iu.revoke();
					this.nextInput = null;
					this.focus = this.getLastIntegrated();
					restart = true;
				} else {
					// Other input simply get their grounding contribution links removed
					iu.removeGrin(this.nextInput);
//					iu.getContribution().setValue("?");
					this.nextInput = null;
					this.focus = this.getLastIntegrated();
					restart = true;
				}
				this.currentContrib = this.focus;
				// Revoke output dialogue act ius that are grounded in revoked input.
				List<DialogueActIU> unground = new ArrayList<DialogueActIU>();
				for (IU daiu : iu.grounds()) {
					if (daiu instanceof DialogueActIU) {
						logger.info("Revoking " + daiu.toString());
						this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.REVOKE, (DialogueActIU) daiu));
						this.nextOutput = (DialogueActIU) this.nextOutput.getSameLevelLink(); //FIXME: potential scope issues in assuming the previous output here
						unground.add((DialogueActIU) daiu);
					}
				}
				// and remove grin links to contributions (because the edit message created here may not be applied in time).
				for (DialogueActIU daiu : unground) {
					daiu.removeGrin(iu);
				}
			}
		}
		// Remove any contributions that may have been revoked above
		List<ContribIU> revoked = new ArrayList<ContribIU>();
		for (IU iu : this.contributions) {
			if (iu instanceof ContribIU && iu.isRevoked()) {
				revoked.add((ContribIU) iu);
			}
		}
		this.contributions.removeAll(revoked);
		return restart;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Integrates the current contribution with the next input if possible.
	 * Clears next input and integrated and visited contributions list to restart the
	 * search with new input.
	 * @true if successful
	 */
	@Override
	public boolean integrateNextInput() {
		ContribIU marked = this.integrateList.get(0);
		// If marked contribution isn't already grounded in next input
		if (!marked.groundedIn().contains(this.nextInput)) {
			// do so now, after deciding output.
			AbstractDialogueAct newAct = new GroundDialogueAct();
			for (IU iu : marked.groundedIn()) {
				if (iu instanceof ContribIU) {
					newAct = new RequestDialogueAct();
				}
			}
			this.nextInput.ground(marked);
			marked.groundIn(this.nextInput); 
			marked.getContribution().setValue(this.nextInput.getAVPairs().get(0).getValue());
			// output a grounding dialogue act
			this.nextOutput = new DialogueActIU(this.nextOutput, marked, newAct);
			this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.nextOutput));
			// move focus to the newly integrated contribution
			this.currentContrib = marked;
			this.focus = marked;
			// clear next input (we just integrated some)
			this.nextInput = null;
			// clear the integrate list (we just integrated some)
			this.integrateList.clear();
			// clear the visited list for the next search
			this.visited.clear();
			// revoke uncommitted clarifications grounded in integrated contribution (these are now assumed clarified)
			for (IU iu : marked.grounds()) {
				if (iu instanceof DialogueActIU) {
					AbstractDialogueAct act = ((DialogueActIU) iu).getAct();
					if (act instanceof ClarifyDialogueAct) {
						if (!iu.isCommitted()) {
							this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.REVOKE, (DialogueActIU) iu));
							marked.removeGrin(iu);
						}						
					}
				} else if (iu instanceof ContribIU) {
					if (((ContribIU) iu).getContribution().equals("clarify:true")) {
						iu.revoke();
						this.contributions.remove(iu);
//						for (IU wiu : iu.groundedIn()) {
//							if (wiu instanceof WordIU) {
////								this.nextInput = (WordIU) wiu;
//								break;
//							}
//						}
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Ground contributions grounded-in any last output in current 'yes' input.
	 * @true if successful
	 */
	@Override
	public boolean integrateYesEllipsis() {
		// Do this for all types of output?
//		if (!(this.nextOutput.getAct() instanceof GroundDialogueAct)) {
//			return false;
//		}
		for (IU iu : this.nextOutput.groundedIn()) {
			if (iu instanceof ContribIU) {
				this.nextInput.ground(iu);
				this.nextInput = null;
				this.currentContrib = (ContribIU) iu;
				this.focus = (ContribIU) iu;
				this.integrateList.clear();
				this.visited.clear();
				return true;
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Revokes last output and grin links to any contributions it grounds.
	 * @true if successful
	 */
	@Override
	public boolean integrateNoEllipsis() { 
		logger.info("Next output: " + this.nextOutput.toString());
		logger.info(" Next input: " + this.nextInput.toString());
		if (this.nextOutput.getAct() instanceof GroundDialogueAct) {  	// FIXME: scope issues with nextOutput
			// only do this for grounding or undoable DAs
			for (IU iu : this.nextOutput.groundedIn()) {
				if (iu instanceof ContribIU) {
					// unground contribution in lb
					List<IU> remove = new ArrayList<IU>();
					for (IU grin : iu.groundedIn()) {
						if (!(grin instanceof ContribIU)) {
							remove.add(grin);
						}
					}
					iu.removeGrin(remove);
					((ContribIU) iu).getContribution().setValue("?");
					// add a glue contribution for integrating input
					ContribIU glue = new ContribIU(null, iu, new AVPair("undo:true"), false, false);
					glue.groundIn(this.nextInput);
					glue.groundIn(iu);
					glue.groundIn(remove);
					this.contributions.add(glue);
					// and creating undo output
					this.nextOutput = new DialogueActIU(this.nextOutput, glue, new UndoDialogueAct());
					this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.nextOutput));
					// set focus and restart search 
					this.nextInput = null;
					this.currentContrib = (ContribIU) iu;  // may not want this...
					this.focus = (ContribIU) iu;           // may not want this...
					this.integrateList.clear();
					this.visited.clear();
					System.err.println("FOCUS IS: " + this.focus.toString());

					return true;
				}
			}
			return false;
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * @true if successful
	 */
	@Override
	public boolean clarifyNextInput() {
		ContribIU glue = new ContribIU(null, this.integrateList, new AVPair("clarify:true"), false, false);
		glue.groundIn(this.nextInput);
		this.contributions.add(glue);
		this.nextOutput = new DialogueActIU(this.nextOutput, glue, new ClarifyDialogueAct());
		this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.nextOutput));
		this.currentContrib = this.focus;
		this.nextInput = null;
		this.integrateList.clear();
		this.visited.clear();
		return true;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * @true if successful
	 */
	@Override
	public boolean requestMoreInfoAboutFocus() {
		this.nextOutput = new DialogueActIU(this.nextOutput, this.focus, new RequestDialogueAct());
		this.outputEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.nextOutput));
		this.currentContrib = this.focus;
		this.nextInput = null;
		this.integrateList.clear();
		this.visited.clear();
		return true;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's current contribution can integrate with next input.
	 * This is the case if the current contribution hasn't already integrated with
	 * current input during search and isn't already grounded in other input.
	 * In addition, if another contribution that doesn't require clarification also
	 * integrates, the current one is barred from integration.
	 * Barring these conditions, a payload check is performed to check if integration
	 * is possible.
	 * Also tracks the contributions tries during the current search by adding
	 * the current contribution to the visited list.
	 * @return true if so
	 */
	@Override
	public boolean currentContribIntegratesNextInput() {
		if (this.nextInput == null)
			return false;
		if (this.integrateList.contains(this.getCurrentContrib())) {
			// contrib doesn't integrate input if it already integrates this input
			return false;
		}
		// contrib doesn't integrate input if it's already grounded
		// in input/output and shouldn't be "overwritten"
		for (IU iu : this.currentContrib.groundedIn()) {
			if (iu.getClass().equals(this.nextInput.getClass())) {
				if (!this.currentContrib.overwrite()) {
					return false;
				}
			}
		}
		for (ContribIU iu : this.integrateList) {
			if (!iu.clarify()) {
				// contrib shouldn't integrate input if another one
				// that doesn't want to be clarified was
				// already marked for integration
				return false;
			}
		}
		// barring the above, make the payload check against input.
		return this.currentContrib.integratesWith(this.nextInput);
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's current contribution has a subsequent
	 * SLL that has not been visited during search (visited
	 * SLL return false because the IS implements a top-down search.)
	 * @return true if so
	 */
	@Override
	public boolean currentContribHasNextSLL() {
		if (this.nextInput == null)
			return false;
		for (IU iu : this.contributions) {
			if (iu.getSameLevelLink() != null) {
				if (iu.getSameLevelLink().equals(this.getCurrentContrib())) {
					if (!this.visited.contains(iu))
						return true;
				}				
			}
		}
		return false;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's current contribution grounds another contribution that hasn't been visited yet.
	 * @return true if so 
	 */
	@Override
	public boolean currentContribGroundsSomething() {
		if (this.nextInput == null)
			return false;
		for (IU iu : this.getCurrentContrib().grounds()) {
			if (iu instanceof ContribIU) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's current contribution has a SLL.
	 * @return true if so
	 */
	@Override
	public boolean currentContribHasSSL() {
		if (this.nextInput == null)
			return false;
		if (this.getCurrentContrib().getSameLevelLink() != null) {
			return true;
		}
		return false;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's current contribution is grounded
	 * in another contribution. The root contribution is not
	 * grounded in another contribution.
	 * @return true if so 
	 */
	@Override
	public boolean currentContribIsGroundedInSomething() {
		if (this.nextInput == null)
			return false;
		for (IU iu : this.getCurrentContrib().groundedIn()) {
			if (iu instanceof ContribIU) {
				if (!this.visited.contains(iu)) {
					return true;					
				}
			}
		}
		return false;
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's integrate list has exactly one member contribution
	 * or several of which at least one doesn't require clarification.
	 * that integrates with current input.
	 * @return true if so
	 */
	@Override
	public boolean integrateListHasOneMember() {
		if (this.nextInput == null)
			return false;
		return  (this.integrateList.size() == 1);
	}

	/**
 	 * A Precondition method that queries the information state.
	 * Checks if the IS's integrate list has more than one member contribution
	 * that integrates with current input.
	 * @return true if so
	 */
	@Override
	public boolean integrateListHasMoreThanOneMember() {
		if (this.nextInput == null)
			return false;
		return (this.integrateList.size() > 1);
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's integrate list is empty.
	 * @return true if so
	 */
	@Override
	public boolean integrateListIsEmpty() {
		if (this.nextInput == null)
			return false;
		return (this.integrateList.isEmpty());
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's next input is bool:true
	 * @return true if so
	 */
	@Override
	public boolean nextInputIsNo() {
		if (this.nextInput == null)
			return false;
		if (this.nextInput.getAVPairs() == null)
			return false;
		return this.nextInput.getAVPairs().get(0).equals("bool:false");
	}

	/**
	 * A Precondition method that queries the information state.
	 * Checks if the IS's next input is bool:false
	 * @return true if so
	 */
	@Override
	public boolean nextInputIsYes() {
		if (this.nextInput == null)
			return false;
		if (this.nextInput.getAVPairs() == null)
			return false;
		return this.nextInput.getAVPairs().get(0).equals("bool:true");
	}

	/**
	 * Builds and returns a String representation of the IS.
	 * @return string the String representation
	 */
	public String toString() {
		String ret;
		ret  = "-INFORMATION STATE------\n";
		ret += "  Next Input:\n\t";
		ret += this.nextInput != null && this.nextInput.isRevoked() ? "(revoked) " : "(added) ";
		ret += this.nextInput == null ? "none\n" : this.nextInput.toString() + "\n";
		ret += "  Contributions:\n";
		for (ContribIU iu : this.contributions) {
			ret += "\t" + iu.toString() + "\n";
		}
		ret += "    Focus Contribution:\n";
		ret += "\t" + this.focus.toString() + "\n";
		ret += "  Current Contribution:\n";
		ret += "\t" + this.currentContrib.toString() + "\n";
		ret += "  Integration Canditates:\n";
		ret += "\t" + this.integrateList.toString() + "\n";
		ret += "  Next Output:\n\t";
		ret += this.nextOutput == null ? "none\n" : this.nextOutput.toString() + "\n";
		return ret;
	}
	
}
