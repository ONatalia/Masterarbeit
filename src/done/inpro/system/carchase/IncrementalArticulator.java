package done.inpro.system.carchase;

import inpro.audio.DispatchStream;

import org.apache.log4j.Logger;

import done.inpro.system.carchase.CarChaseExperimenter.TTSAction;

public class IncrementalArticulator extends StandardArticulator {

	private static Logger logger = Logger.getLogger("IncrementalArticulator");
	
	HesitatingSynthesisIU installment;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
	}
	
	@Override
	public void precompute(TTSAction action) {
		super.precompute(action);
		if (action.cont != null) {
			action.cont = new HesitatingSynthesisIU((String) action.cont);
		}
	}

	@Override
	public void say(TTSAction action) {
		logger.info(action.text);
		if (installment == null || installment.isCompleted()) {
			installment = (HesitatingSynthesisIU) action.installmentIU;
			dispatcher.playStream(installment.getAudio(), false);
		} else { // installment is still in progress
			// inspect the word that is being uttered
			logger.info("trying to append continuation : " + action.cont);
			installment.appendContinuation(((HesitatingSynthesisIU) action.cont).getWords());
		}
	}

}
