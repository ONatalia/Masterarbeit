package inpro.irmrsc.rmrs;

import java.util.HashMap;
import java.util.Map;

/**
 * A mapping from integer IDs to {@link Variable} objects.
 * @author Andreas Peldszus
 */
public abstract class VariableEnvironment {

	// holds all variable objects in this expression
	protected Map<Integer,Variable> mVariables;
	
	public VariableEnvironment(VariableEnvironment ve) {
		// deep copy variables
		this.mVariables = new HashMap<Integer,Variable>();
		for (Map.Entry<Integer, Variable> e : ve.mVariables.entrySet())
			this.mVariables.put(new Integer(e.getKey()), new Variable(e.getValue()));
	}
	
	public VariableEnvironment() {
		mVariables = new HashMap<Integer,Variable>();
	}
	
	public String getVariableString(Integer id) {
		Variable v = mVariables.get(id);
		if (v == null) {
			return "#"+id.toString();
		} else {
			return v.toString();
		}
	}
	
	/**
	 * @return the maximal variable index in this expression
	 */
	public int getMaxID() {
		int i = 0; //TODO: implicitly assuming that id are always positive
		for (Integer k : mVariables.keySet()) {
			int c = k.intValue();
			if (c > i) {
				i = c;
			}
		}
		return i;
	}
	
	/**
	 * updates the list of variables in this expression 
	 */
	public abstract void update();
}
