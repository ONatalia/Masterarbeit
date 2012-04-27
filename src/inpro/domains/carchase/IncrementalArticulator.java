package inpro.domains.carchase;

import inpro.audio.DispatchStream;
import inpro.domains.carchase.CarChaseExperimenter.TTSAction;
import inpro.incremental.unit.IncrSysInstallmentIU;
import inpro.incremental.unit.SysInstallmentIU;

import org.apache.log4j.Logger;

public class IncrementalArticulator extends StandardArticulator {

	private static Logger logger = Logger.getLogger("IncrementalArticulator");
	
	IncrSysInstallmentIU installment;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
	}
	
	@Override
	public void precompute(TTSAction action) {
		super.precompute(action);
		if (action.cont != null) {
			action.cont = new IncrSysInstallmentIU((String) action.cont);
		}
	}

	@Override
	public void say(TTSAction action) {
		logger.info(action.text);
		if (installment == null || installment.isCompleted()) {
			installment = (IncrSysInstallmentIU) action.appData;
			dispatcher.playStream(installment.getAudio(), false);
		} else { // installment is still in progress
			// inspect the word that is being uttered
			logger.info("trying to append continuation : " + action.cont);
			installment.appendContinuation(((SysInstallmentIU) action.cont).getWords());
		}
	}

}
