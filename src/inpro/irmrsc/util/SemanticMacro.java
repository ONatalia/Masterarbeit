package inpro.irmrsc.util;

import inpro.irmrsc.rmrs.Formula;

/**
 * A semantic macro is a RMRS {@link Formula} identified by a short name or a long name. 
 * @author Andreas Peldszus
 */
public class SemanticMacro {

	private String shortName;
	private String longName;
	private Formula formula;
	
	public SemanticMacro (String s, String l, Formula f) {
		if (s == null) {
			shortName = "";
		} else {
			shortName = s;
		}
		if (l == null) {
			longName = "";
		} else {
			longName = l;
		}
		formula = f;
	}

	public String getShortName() {
		return shortName;
	}

	public String getLongName() {
		return longName;
	}

	public Formula getFormula() {
		return formula;
	}

	public void setFormula(Formula formula) {
		this.formula = formula;
	}
}
