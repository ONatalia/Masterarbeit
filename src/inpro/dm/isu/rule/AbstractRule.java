package inpro.dm.isu.rule;

import inpro.dm.isu.AbstractInformationState;

/**
 * An abstract rule with previsions for trigger and effect interfaces
 * @author okko
 *
 */
public abstract class AbstractRule {

	/**
	 * A method that a rule engine calls to verify if this rule
	 * triggers.
	 * @param is the InformationState to check this rule's trigger
	 * conditions against.
	 * @return true if triggers
	 */
	public abstract boolean triggers(AbstractInformationState is);
	/**
	 * A method that a rule engine calls to apply this rule's
	 * effect to.
	 * @param is the InformationState to apply this rule's effects on.
	 * @return true if applying changes the information state. 
	 */	
	public abstract boolean apply(AbstractInformationState is);
	
	/**
	 * A list of query methods that an implementing
	 * information state must provide to allow
	 * update rules to query it.
	 * @author okko
	 *
	 */
	public abstract interface Triggers {}
	/**
	 * A list of update methods that an implementing
	 * information state must provide to allow
	 * update rules to apply changes to it.
	 * @author okko
	 *
	 */
	public abstract interface Effects {}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName().toString();
	}
}
