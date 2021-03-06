package inpro.dm.isu.rule;

import inpro.dm.isu.AbstractInformationState;
import inpro.dm.isu.IUNetworkInformationState;

public class ClarifyNextInputRule extends AbstractIUNetworkRule {

	@Override
	public boolean triggers(AbstractInformationState is) {
//		System.err.println("Testing trigger for " + this.toString());
		return (((IUNetworkInformationState) is).integrateListHasMoreThanOneMember());
	}

	@Override
	public boolean apply(AbstractInformationState is) {
		System.err.println(this.toString() + " triggered.");
		return (((IUNetworkInformationState) is).clarifyNextInput());
	}

}
