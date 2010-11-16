package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.dm.acts.SimpleDialogueAct;
import org.cocolab.inpro.dm.isu.rule.AbstractIUNetworkRule;
import org.cocolab.inpro.incremental.unit.ContribIU;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;

public class IUNetworkInformationState extends AbstractInformationState implements AbstractIUNetworkRule.Triggers, AbstractIUNetworkRule.Effects {

	private WordIU nextInput;
	private IUList<ContribIU> contributions = new IUList<ContribIU>();
	private IUList<ContribIU> integrateList = new IUList<ContribIU>();
	private ContribIU root;
	private ContribIU currentContrib;
	private IUList<ContribIU> visited = new IUList<ContribIU>();
//	private boolean traversalPassedRoot = false;
	private DialogueActIU nextOutput;

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

	private WordIU getNextInput() {
		return nextInput;
	}
	
	public void setNextInput(WordIU word) {
		this.nextInput = word;
	}

	/**
	 * Gets the current focus contribution.
	 * This is defined as the contribution that was most
	 * recently integrated with input.
	 * Returns the first contribution of none are integrated.
	 * @return the most recently integrated contribution
	 */
	public ContribIU getFocusContrib() {
		ContribIU focus = ContribIU.FIRST_CONTRIB_IU;
		for (ContribIU iu : this.contributions) {
			// End time is inherited from input. Greater is later.
			if (iu.endTime() > focus.endTime()) {
				focus = iu;
			}
		}
		return focus;
	}

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

	private ContribIU getCurrentContrib() {
		if (this.currentContrib == null)
			return this.root;
		return this.currentContrib;
	}

	@Override
	public boolean nextInputIsRevoked() {
		if (this.nextInput == null)
			return false;
		return this.nextInput.isRevoked();
	}

	@Override
	public boolean addCurrentContribToIntegrateList() {
		if (this.integrateList.contains(this.getCurrentContrib()))
			return false;
		this.integrateList.add(this.getCurrentContrib());
		return true;
	}

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

	@Override
	public boolean integrateNextInput() {
		if (!this.integrateList.get(0).groundedIn().contains(this.getNextInput())) {
			this.getNextInput().ground(this.integrateList.get(0));
			this.currentContrib = this.integrateList.get(0);
			this.nextInput = null;
			this.integrateList.clear();
			this.visited.clear();
			//TODO: revoke clarification output here.
			return true;
		}
		return false;
	}

	@Override
	public boolean clarifyNextInput() {
		List<IU> grin = new ArrayList<IU>(this.integrateList);
		this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, grin, new SimpleDialogueAct(SimpleDialogueAct.Act.CLARIFY));
		this.currentContrib = this.getFocusContrib();
		this.nextInput = null;
		this.integrateList.clear();
		this.visited.clear();
		return true;
	}

	@Override
	public boolean requestMoreInfoAboutFocus() {
		this.nextOutput = new DialogueActIU((IU) DialogueActIU.FIRST_DA_IU, this.getFocusContrib(), new SimpleDialogueAct(SimpleDialogueAct.Act.REQUEST));
		this.currentContrib = this.getFocusContrib();
		this.nextInput = null;
		this.integrateList.clear();
		this.visited.clear();
		return true;
	}

	@Override
	public boolean currentContribIntegratesNextInput() {
		if (this.nextInput == null)
			return false;
		this.visited.add(this.getCurrentContrib());
		if (this.integrateList.contains(this.getCurrentContrib())) {
			// contrib doesn't integrate input if it already integrates other input
			return false;			
		}
		for (IU iu : this.currentContrib.groundedIn()) {
			if (iu.getClass().equals(this.nextInput.getClass())) {
				// contrib doesn't integrate input if it's already grounded in input/output
				return false;
			}
		}
		// barring the above, make the payload check against input.
		return this.getCurrentContrib().integratesWith(this.nextInput);
	}

	@Override
	public boolean currentContribHasNextSSL() {
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
	
	@Override
	public boolean currentContribHasSSL() {
		if (this.nextInput == null)
			return false;
		if (this.getCurrentContrib().getSameLevelLink() != null) {
			return true;
		}
		return false;
	}

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

	@Override
	public boolean integrateListHasOneMember() {
		if (this.nextInput == null)
			return false;
		return (this.integrateList.size() == 1);
	}

	@Override
	public boolean integrateListHasMoreThanOneMember() {
		if (this.nextInput == null)
			return false;
		return (this.integrateList.size() > 1);
	}

	@Override
	public boolean integrateListIsEmpty() {
		if (this.nextInput == null)
			return false;
		return (this.integrateList.isEmpty());
	}

	@Override
	public boolean unintegrateNextInput() {
		boolean stateChanged = false;
		for (ContribIU iu : this.contributions) {
			if (iu.groundedIn().contains(this.nextInput)) {
				iu.groundedIn().remove(this.nextInput);
				this.nextInput.grounds().remove(iu);
				this.nextInput = null;
				stateChanged = true;
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


	
}
