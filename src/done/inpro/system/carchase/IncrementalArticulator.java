package done.inpro.system.carchase;

import java.util.Collection;
import java.util.List;

import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.HesitationIU;
import inpro.incremental.unit.IU;

import org.apache.log4j.Logger;

import done.inpro.system.carchase.CarChaseExperimenter.TTSAction;

public class IncrementalArticulator extends StandardArticulator {

	private static Logger logger = Logger.getLogger("IncrementalArticulator");
	
	private final SynthesisModule synthesisModule;
	private final DispatchStream dispatcher;
	private final MyIUSource myIUSource;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
		this.dispatcher = dispatcher;
		synthesisModule = new SynthesisModule(dispatcher);
		myIUSource = new MyIUSource();
		myIUSource.addListener(synthesisModule);
		//myIUSource.addListener(new CurrentHypothesisViewer().show());
		//synthesisModule.addListener(new CurrentHypothesisViewer().show());
	}
	
	@Override
	public void say(TTSAction action) {
		logger.info(action.text);
		if (!dispatcher.isSpeaking()) {
			myIUSource.say(action.text);
		} else { // installment is still in progress
			logger.info("trying to append continuation : " + action.cont);
			myIUSource.say((String) action.cont);
		}
	}
	
	private class MyIUSource extends IUModule {

		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw new org.apache.commons.lang.NotImplementedException("StandardArticulator.MyIUSource is an IU source, it hence ignores its left buffer.");
		}
		
		// example of how to revoke the most recently added chunk/hesitation
		public void revokeLast() {
			rightBuffer.editBuffer(new EditMessage(EditType.REVOKE, rightBuffer.getBuffer().get(rightBuffer.getBuffer().size() - 1)));
			rightBuffer.notify(iulisteners);
		}
		
		public void say(String text) {
			boolean addHesitation = false;
			if (text.matches(".*<hes>$")) {
				text = text.replaceAll(" <hes>$", "");
				addHesitation = true;
			}
			rightBuffer.addToBuffer(new ChunkIU(text));
			if (addHesitation) {
				logger.info("adding hesitation");
				rightBuffer.addToBuffer(new HesitationIU());
			}
			rightBuffer.notify(iulisteners);
		}
		
	}

}
