package inpro.irmrsc.rmrs;

import java.util.Set;

/**
 * An expression that contains indexed variables and that can be interpreted in a corresponding {@link VariableEnvironment}.
 * @author Andreas Peldszus
 */
interface VariableIDsInterpretable {

	Set<Integer> getVariableIDs();
	void replaceVariableID(int oldID, int newID);
	
}
