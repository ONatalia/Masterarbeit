package org.cocolab.inpro.dm.isu.rule;

import org.cocolab.inpro.dm.isu.AbstractInformationState;

/**
 * An abstract rule with previsions for trigger and effect interfaces
 * @author okko
 *
 */
public abstract class AbstractRule {

	public abstract boolean triggers(AbstractInformationState is);
	public abstract boolean apply(AbstractInformationState is);
	
	public abstract interface Triggers {}
	public abstract interface Effects {}
	
	public String toString() {
		return this.getClass().getSimpleName().toString();
	}
}
