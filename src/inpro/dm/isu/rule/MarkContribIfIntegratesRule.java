package inpro.dm.isu.rule;

import inpro.dm.isu.AbstractInformationState;
import inpro.dm.isu.IUNetworkInformationState;

public class MarkContribIfIntegratesRule extends AbstractIUNetworkRule {

	public MarkContribIfIntegratesRule() {}

	@Override
	public boolean triggers(AbstractInformationState is) {
//		System.err.println("Testing trigger for " + this.toString());
		return (((IUNetworkInformationState) is).currentContribIntegratesNextInput());
	}

	@Override
	public boolean apply(AbstractInformationState is) {
//		System.err.println(this.toString() + " triggered.");
		return (((IUNetworkInformationState) is).addCurrentContribToIntegrateList());
	}

}
