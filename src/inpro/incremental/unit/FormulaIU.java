package inpro.incremental.unit;

import inpro.irmrsc.rmrs.Formula;

import java.util.Collections;
import java.util.List;


public class FormulaIU extends IU {
	
	private Formula formula;

	public static final FormulaIU FIRST_FORMULA_IU = new FormulaIU();
	
	@SuppressWarnings("unchecked")
	public FormulaIU() {
		this(FIRST_FORMULA_IU, Collections.EMPTY_LIST, null);
	}

	public FormulaIU(IU sll, List<IU> groundedIn, Formula formula) {
		super(sll, groundedIn);
		this.formula = formula;
	}
	
	public FormulaIU(IU sll, IU groundedIn, Formula formula) {
		super(sll, Collections.singletonList(groundedIn));
		this.formula = formula;
	}

	@Override
	public String toPayLoad() {
		if (formula == null) {
			return "";
		}
		return this.formula.toString();
	}

	public Formula getFormula () {
		return formula;
	}
}
