package done.inpro.system.carchase;

import done.inpro.system.carchase.CarChaseExperimenter.TTSAction;
import inpro.audio.DispatchStream;

public class StandardArticulator extends CarChaseExperimenter.Articulator {

	public StandardArticulator(DispatchStream dispatcher) {
		super(dispatcher);
	}
	
	@Override
	public void precompute(TTSAction action) {
		action.appData = new HesitatingSynthesisIU(action.text);
	}

	@Override
	public void say(TTSAction action) {
//		SysInstallmentIU inst = new IncrSysInstallmentIU(action.text);
		HesitatingSynthesisIU inst = (HesitatingSynthesisIU) action.appData;
		boolean skipQueue = true;
		if (!dispatcher.isSpeaking() || !action.isOptional()) 
			dispatcher.playStream(inst.getAudio(), skipQueue);
	}
	
}
