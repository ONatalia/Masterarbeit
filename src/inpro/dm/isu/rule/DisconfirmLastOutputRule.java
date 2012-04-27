package inpro.dm.isu.rule;

import inpro.dm.isu.AbstractInformationState;
import inpro.dm.isu.IUNetworkInformationState;

public class DisconfirmLastOutputRule extends AbstractIUNetworkRule {

	@Override
	public boolean triggers(AbstractInformationState is) {
		return (((IUNetworkInformationState) is).nextInputIsNo());	}

	@Override
	public boolean apply(AbstractInformationState is) {
		System.err.println(this.toString() + " triggered!");
		return (((IUNetworkInformationState) is).integrateNoEllipsis());
	}

}
