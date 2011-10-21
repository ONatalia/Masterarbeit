package org.cocolab.inpro.irmrsc.rmrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A simple representation of logical predicates.
 * The arguments are given as integers, representing untyped
 * variables, the predicate name is a String.
 * @author andreas
 *
 */
public class SimpleAssertion implements VariableIDsInterpretable {

	public static enum Type { INTRANSITIVE, TRANSITIVE, DITRANSITIVE; }
	private String predicateName;
	private List<Integer> arguments;
	
	/**
	 * @param predicateName
	 * @param arguments
	 */
	public SimpleAssertion(String predicateName, List<Integer> arguments) {
		this.predicateName = predicateName;
		this.arguments = arguments;
	}
	
	/**
	 * @param predicateName
	 * @param arguments
	 */
	public SimpleAssertion(String predicateName, int[] arguments) {
		this.predicateName = predicateName;
		this.arguments = new ArrayList<Integer>(arguments.length);
		for (int i : arguments) {
			this.arguments.add(i);
		}
	}
	
	/**
	 * @return the predicateName
	 */
	public String getPredicateName() {
		return predicateName;
	}
	
	/**
	 * @return the arguments
	 */
	public List<Integer> getArguments() {
		return arguments;
	}
	
	/**
	 * @return the number of arguments
	 */
	public int getNumberOfArguments() {
		return arguments.size();
	}
	
	/**
	 * Gets the type of assertions (transtivive with one, ditransitive with two or intransitive with 0 arguments)
	 * @return the Type
	 */
	public Type getType() {
		if (this.getNumberOfArguments() == 2) {
			return Type.DITRANSITIVE;
		} else if (this.getNumberOfArguments() == 1) {
			return Type.TRANSITIVE;
		}
		return Type.INTRANSITIVE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(predicateName+"(");
		if (arguments.size() > 0) {
			for (Integer i : arguments) {
				if (i != null) {
					sb.append(i.toString()+",");
				} else {
					sb.append(",");
				}
			}
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Set<Integer> getVariableIDs() {
		return new TreeSet<Integer>(arguments);
	}

	@Override
	public void replaceVariableID(int oldID, int newID) {
		List<Integer> newArguments = new ArrayList<Integer>(arguments.size());
		for (Integer i : arguments) {
			if (i.intValue() == oldID) {
				newArguments.add(new Integer(newID));
			} else {
				newArguments.add(i);
			}
		}
	}
}
