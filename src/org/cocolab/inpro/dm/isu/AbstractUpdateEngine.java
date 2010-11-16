package org.cocolab.inpro.dm.isu;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.dm.isu.rule.AbstractRule;

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
