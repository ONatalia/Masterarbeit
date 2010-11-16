package org.cocolab.inpro.dm.isu.rule;

public abstract class AbstractIUNetworkRule extends AbstractRule {

	public abstract interface Triggers {
		boolean currentContribIntegratesNextInput();
		boolean currentContribHasNextSSL();
		boolean currentContribHasSSL();
		boolean currentContribGroundsSomething();
		boolean currentContribIsGroundedInSomething();
		boolean integrateListHasOneMember();
		boolean integrateListHasMoreThanOneMember();
		boolean integrateListIsEmpty();
		boolean nextInputIsRevoked();
	}

	public abstract interface Effects {
		boolean addCurrentContribToIntegrateList();
		boolean moveCurrentContribRight();
		boolean moveCurrentContribDown();
		boolean moveCurrentContribLeft();
		boolean moveCurrentContribUp();
		boolean integrateNextInput();
		boolean clarifyNextInput();
		boolean requestMoreInfoAboutFocus();
		boolean unintegrateNextInput();
	}

}
