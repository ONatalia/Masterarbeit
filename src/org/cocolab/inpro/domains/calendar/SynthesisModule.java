package org.cocolab.inpro.domains.calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
//	DispatchStream noiseDispatcher;

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		speechDispatcher = SimpleMonitor.setupDispatcher();
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<?> em : edits) {
			switch (em.getType()) {
			case ADD:
				System.err.println("I should append " + em.getIU().toPayLoad() + " to what I'm saying.");
				break;
			case REVOKE:
				System.err.println("I should NOT say " + em.getIU().toPayLoad());
				break;
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SynthesisModule sm = new SynthesisModule();
		List<PhraseIU> phrases = new ArrayList<PhraseIU>();
		phrases.add(new PhraseIU("Hallo Timo", PhraseIU.PhraseType.INITIAL));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.add(new PhraseIU("Wie geht's?", PhraseIU.PhraseType.CONTINUATION));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.remove(1);
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);		
	}

}
