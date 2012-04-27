package inpro.irmrsc.rmrs;

import java.util.Set;

interface VariableIDsInterpretable {

	Set<Integer> getVariableIDs();
	void replaceVariableID(int oldID, int newID);
	
}
