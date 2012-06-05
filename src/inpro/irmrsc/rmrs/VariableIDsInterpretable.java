package inpro.irmrsc.rmrs;

import java.util.Set;

/**
 * An expression that contains IDs of variables and that can be
 * interpreted in a corresponding {@link VariableEnvironment}.
 * @author Andreas Peldszus
 */
interface VariableIDsInterpretable {

	/** @return the set of IDs of all variables in that expression */
	Set<Integer> getVariableIDs();
	
	/** replace all variables IDs matching a specifid old ID with a new ID */
	void replaceVariableID(int oldID, int newID);
	
}
