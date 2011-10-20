package org.cocolab.inpro.irmrsc.util;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple representation of logical predicates.
 * The arguments are given as integers, representing untyped
 * variables, the predicate name is a String.
 * @author andreas
 *
 */
public class SimpleAssertion {

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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(predicateName+"(");
		if (arguments.size() > 0) {
			for (Integer i : arguments) sb.append(i.toString()+",");
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(")");
		return sb.toString();
	}
}
