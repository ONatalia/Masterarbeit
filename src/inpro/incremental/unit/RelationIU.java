package inpro.incremental.unit;

import inpro.irmrsc.rmrs.Formula;
import inpro.irmrsc.rmrs.Relation;

public class RelationIU extends IU {
	
	public static final RelationIU FIRST_RELATION_IU = new RelationIU();

	private Relation relation;
	private Formula formula; //this is the formula that this relation came from; it is needed to hook things together
	
	public RelationIU(Relation r, Formula f) {
		this.setRelation(r);
		this.setFormula(f);
	}
	
	public RelationIU() {
		
	}

	@Override
	public String toPayLoad() {
		return this.getRelation().toString();
	}

	public Relation getRelation() {
		return relation;
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
	}

	public Formula getFormula() {
		return formula;
	}

	public void setFormula(Formula formula) {
		this.formula = formula;
	}


}
