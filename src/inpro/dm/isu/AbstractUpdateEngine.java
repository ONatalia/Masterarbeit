package inpro.dm.isu;

import inpro.dm.isu.rule.AbstractRule;

import java.util.ArrayList;
import java.util.List;


/**
 * A rule engine with previsions for loading rules and applying them.
 * @author okko
 *
 */
public abstract class AbstractUpdateEngine {

	protected static List<AbstractRule> rules = new ArrayList<AbstractRule>();

	/**
	 * Applies rules
	 */
	abstract public void applyRules();

}
