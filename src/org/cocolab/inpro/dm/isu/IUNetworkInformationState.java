package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.dm.acts.ClarifyDialogueAct;
import org.cocolab.inpro.dm.acts.GroundDialogueAct;
import org.cocolab.inpro.dm.acts.RequestDialogueAct;
import org.cocolab.inpro.dm.acts.SpeakDialogueAct;
import org.cocolab.inpro.dm.isu.rule.AbstractIUNetworkRule;
import org.cocolab.inpro.incremental.unit.ContribIU;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;

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
	/** A variable that holds the contribution currently looked at while searching for contributions to integrate new input with. */
	private ContribIU currentContrib;
	/** The list of contributions with which input can be integrated. Filled during search and cleared afterwards. */
	private IUList<ContribIU> integrateList = new IUList<ContribIU>();
	/** A list of contributions that were visited during search. Filled during seach and cleared afterwards.  */
	private IUList<ContribIU> visited = new IUList<ContribIU>();
	/** The DialogueActIU representing nextOutput. */
	private DialogueActIU nextOutput;

	/**
	 * Generic constructor for an IUNetworkInformationState
	 */
	public IUNetworkInformationState() {
		ContribIU root = new ContribIU(null, ContribIU.FIRST_CONTRIB_IU, new AVPair("root", "true"));
		ContribIU dig1 = new ContribIU(null, root, new AVPair("digit", "?"));
		ContribIU dig2 = new ContribIU(dig1, root, new AVPair("digit", "?"));
		ContribIU dig3 = new ContribIU(dig2, root, new AVPair("digit", "?"));
		this.contributions.add(root);
		this.contributions.add(dig1);
		this.contributions.add(dig2);
		this.contributions.add(dig3);
		this.currentContrib = root;
		this.root = root;
	}

	/**
	 * Getter method for this IS's next input to process.
	 * @return nextInput the next word input to process
	 */
	public WordIU getNextInput() {
		return nextInput;
	}
	
	/**
	 * Setter method for the IS's next input to process.
	 * @param word the next word input to process.
	 */
	public void setNextInput(WordIU word) {
		this.nextInput = word;
	}

	/**
	 * Getter for this IS's contributions network
	 * @return contributions the list of ContribIUs 
	 */
	public IUList<ContribIU> getContributions() {
		return this.contributions;
	}

	/**
	 * Gets the current focus contribution.
	 * This is defined as the contribution that was most
	 * recently integrated with input.
	 * Returns the first contribution of none are integrated.
	 * @return the most recently integrated contribution
	 */
	public ContribIU getFocusContrib() {
		ContribIU focus = this.root;
		for (ContribIU iu : this.contributions) {
			// End time is inherited from input. Greater is later.
			if (iu.endTime() > focus.endTime()) {
				focus = iu;
			}
		}
		return focus;
	}

	/**
	 * Getter method returning the IS's next focus ContribIU (the first one that is grounded in the current contrib.)
	 * @return nextFocus  the next contribution that is grounded in current focus.
	 */
	public ContribIU getNextFocusContrib() {
		ContribIU nextFocus = this.getFocusContrib();
		if (!this.getFocusContrib().groundedIn().isEmpty()) {
			for (IU iu : this.getFocusContrib().groundedIn()) {
				if (iu instanceof ContribIU) {
					 nextFocus = (ContribIU) iu;
				}
			}
		}
		return nextFocus;
	}

	/**
	 * Getter method returning the IS's current contribution
	 * @return currentContrib this IS's current ContribIU
	 */
	private ContribIU getCurrentContrib() {
		if (this.currentContrib == null)
			return this.root;
		return this.currentContrib;
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
		for (ContribIU iu : this.contributions) {
			if (iu.getSameLevelLink() != null) {
				if (iu.getSameLevelLink().equals(this.getCurrentContrib())) {
					this.currentContrib = iu;
					return true;
				}				
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Moves the current contribution down to its first grounded-in contribution. 
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribDown() {
		for (IU iu : this.getCurrentContrib().grounds()) {
			if (iu instanceof ContribIU) {
				this.currentContrib = (ContribIU) iu;
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
		if (this.getCurrentContrib().getSameLevelLink() != null) {
			this.currentContrib = (ContribIU) this.getCurrentContrib().getSameLevelLink();
			return true;
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Attempts moving the current contrib uup to the first grounding contribution.
	 * @true if successful
	 */
	@Override
	public boolean moveCurrentContribUp() {
		if (this.nextInput == null)
			return false;
		if (this.getCurrentContrib().groundedIn() != null) {
			for (IU iu : this.getCurrentContrib().groundedIn()) {
				if (iu instanceof ContribIU) {
					this.currentContrib = (ContribIU) iu;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * Integrates the current contribution with the next input if possible.
	 * Clears next input and integrated and visitied contributions list to restart the
	 * search with new input.
	 * @true if successful
	 */
	@Override
	public boolean integrateNextInput() {
		if (!this.integrateList.get(0).groundedIn().contains(this.getNextInput())) {
			this.getNextInput().ground(this.integrateList.get(0));
			this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, this.integrateList.get(0), new GroundDialogueAct());
			this.currentContrib = this.integrateList.get(0);
			this.nextInput = null;
			this.integrateList.clear();
			this.visited.clear();
			//TODO: revoke clarification and request output grounded in current contrib here.
			return true;
		}
		return false;
	}

	/**
	 * An Effect method that updates the information state and returns true if successful.
	 * @true if successful
	 */
	@Override
	public boolean clarifyNextInput() {
		List<IU> grin = new ArrayList<IU>(this.integrateList);
		this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, grin, new ClarifyDialogueAct());
		this.currentContrib = this.getFocusContrib();
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
		this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, this.getFocusContrib(), new RequestDialogueAct());
		this.currentContrib = this.getFocusContrib();
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
		this.visited.add(this.getCurrentContrib());
		if (this.integrateList.contains(this.getCurrentContrib())) {
			// contrib doesn't integrate input if it already integrates this input
			return false;
		}
		for (IU iu : this.currentContrib.groundedIn()) {
			if (iu.getClass().equals(this.nextInput.getClass())) {
				// contrib doesn't integrate input if it's already grounded in input/output
				return false;
			}
		}
		for (ContribIU iu : this.integrateList) {
			if (!iu.clarify()) {
				// contrib shouldn't integrate input if another one
				// that doesn't want to be clarified was already marked for integration
				return false;
			}
		}
		// barring the above, make the payload check against input.
		return this.getCurrentContrib().integratesWith(this.nextInput);
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
	 * Checks if the IS's current contribution grounds another contribution.
	 * @return true if so 
	 */
	@Override
	public boolean currentContribGroundsSomething() {
		if (this.nextInput == null)
			return false;
		for (IU iu : this.getCurrentContrib().grounds()) {
			if (iu instanceof ContribIU) {
				if (!this.visited.contains(iu))
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
		if (this.getCurrentContrib().equals(this.root))
			return false;
		for (IU iu : this.getCurrentContrib().groundedIn()) {
			if (iu instanceof ContribIU) {
				return true;
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
	 * An Effect method that updates the information state and returns true if successful.
	 * Removes next input from any contributions it grounds.
	 * @true if successful
	 */

	@Override
	public boolean unintegrateNextInput() {
		boolean stateChanged = false;
		for (ContribIU iu : this.contributions) {
			if (iu.groundedIn().contains(this.nextInput)) {
				iu.groundedIn().remove(this.nextInput);
				this.nextInput.grounds().remove(iu);
				this.nextInput = null;
				stateChanged = true;
				//TODO: revoke DAs grounded in current contribution.
			}
		}
		return stateChanged;
	}

	/**
	 * Getter for the next output to perform.
	 * @return
	 */
	public DialogueActIU getNextOutput() {
		return this.nextOutput;
	}

	/**
	 * Builds and returns a String representation of the IS.
	 * @return string the String representation
	 */
	public String toString() {
		String ret;
		ret  = "-------\n";
		if (this.nextInput == null)
			ret += "  Next Input:\n\tnone\n";
		else
			ret += "  Next Input:\n\t" + this.nextInput.toString() + "\n";
		ret += "  Contributions:\n";
		ret += "\t" + this.contributions.toString() + "\n";
		ret += "  Current Contribution:\n";
		ret += "\t" + this.getCurrentContrib().toString() + "\n";
		ret += "  Integration Canditates:\n";
		ret += "\t" + this.integrateList.toString() + "\n";
		if (this.nextOutput == null)
			ret += "  Next Output:\n\tnone\n";
		else
			ret += "  Next Output:\n\t" + this.nextOutput.toString() + "\n";
		return ret;
	}
	
}
