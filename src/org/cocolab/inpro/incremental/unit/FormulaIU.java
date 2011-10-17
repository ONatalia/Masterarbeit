package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.irmrsc.rmrs.Formula;

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

	@Override
	public String toPayLoad() {
		return this.formula.toString();
	}

}
