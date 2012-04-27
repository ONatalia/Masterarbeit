package inpro.irmrsc.rmrs;

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

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return the longName
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * @return the formula
	 */
	public Formula getFormula() {
		return formula;
	}

	/**
	 * @param formula the formula to set
	 */
	public void setFormula(Formula formula) {
		this.formula = formula;
	}
	
	
	
}
