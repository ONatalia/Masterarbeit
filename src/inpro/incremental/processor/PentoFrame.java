package inpro.incremental.processor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.FormulaIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SlotIU;
import inpro.incremental.unit.utils.IUUtils;
import inpro.irmrsc.rmrs.Formula;

public class PentoFrame extends IUModule {
	
	private LinkedList<SlotIU> slots;
	
	private String head;
	private String arg1;
	private String arg2;
	
	private Formula latestFormula;

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		
		init();
	}
	
	
	private void init() {
		slots = new LinkedList<SlotIU>();
		head = new String();
		arg1 = new String();
		arg2 = new String();
		latestFormula = null;
	}


	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {

		for (EditMessage<? extends IU> edit : edits) {
			
			IU iu = edit.getIU();
			
			switch (edit.getType()) {
			case ADD:
				
				if (iu instanceof SlotIU) {
					if (IUUtils.isFirst(iu)) {
						init();
					}
					
					SlotIU siu = (SlotIU) edit.getIU();
					
					slots.add(siu);
					
					FormulaIU formulaIU = getFomrulaIU(siu);
					if (formulaIU == null) continue; // risky, this is
					
					latestFormula = formulaIU.getFormula();
				}

				break;
			case COMMIT:
				break;
			case REVOKE:
				break;
			default:
				break;
			
			}
		}
					
		
		findSlots();
		
	}
	
	public Formula getFormula() {
		return latestFormula;
	}


	private void findSlots() {

		for (SlotIU slot : slots) {
			if (slot.isHead()) {
				if (!slot.getDistribution().isEmpty())
					System.out.println(slot.getDistribution().getArgMax());
			}
		}
	}


	private FormulaIU getFomrulaIU(IU iu) {
		while (iu != null) {
			if (iu instanceof FormulaIU) {
				return (FormulaIU) iu;
			}
			if (iu.groundedIn().isEmpty()) return null;
			iu = iu.groundedIn().get(0); // TODO: this should probably be recursive
		}
		return null;
	}
}
