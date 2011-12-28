package org.cocolab.inpro.domains.carchase;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.domains.carchase.CarChaseExperimenter.Articulator;
import org.cocolab.inpro.domains.carchase.CarChaseExperimenter.TTSAction;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;

public class IncrementalArticulator extends Articulator {

	IncrSysInstallmentIU installment;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
	}

	@Override
	public void say(TTSAction action) {
		if (installment == null || installment.isCommitted()) {
			installment = new IncrSysInstallmentIU(action.text);
			dispatcher.playStream(installment.getAudio(), false);
		} else { // installment is still in progress
			
		}
	}

}
