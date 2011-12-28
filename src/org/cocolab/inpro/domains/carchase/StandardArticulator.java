package org.cocolab.inpro.domains.carchase;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.domains.carchase.CarChaseExperimenter.TTSAction;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;

public class StandardArticulator extends CarChaseExperimenter.Articulator {

	public StandardArticulator(DispatchStream dispatcher) {
		super(dispatcher);
	}

	@Override
	public void say(TTSAction action) {
		SysInstallmentIU inst = new IncrSysInstallmentIU(action.text);
		dispatcher.playStream(inst.getAudio(), false);
	}
	
}
