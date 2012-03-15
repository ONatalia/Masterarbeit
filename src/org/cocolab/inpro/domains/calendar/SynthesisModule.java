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
	
	ArrayList<PhraseIU> upcomingPhrases;

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		speechDispatcher = SimpleMonitor.setupDispatcher();
	}
	
	public void run() {
		while(true) {
			boolean empty;
			synchronized (upcomingPhrases) {
				empty = upcomingPhrases.isEmpty();
			}
			if (!empty) {
				PhraseIU iu;
				synchronized (upcomingPhrases) {
					iu = upcomingPhrases.remove(0);
				}
				System.out.println("iTTS: " + iu.toPayLoad() + " (" + iu.status + ")");
				try { Thread.sleep(1000);} catch (InterruptedException e) {}
				iu.notifyListeners();
			}
			try { Thread.sleep(1000);} catch (InterruptedException e) {}
		}
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		for (EditMessage<?> em : edits) {
			PhraseIU iu = (PhraseIU) em.getIU();
			switch (em.getType()) {
			case ADD:
				System.err.println("ADD " + iu.toPayLoad() + " (" + iu.status + ")");
				synchronized (upcomingPhrases) {
					upcomingPhrases.add(iu);
				}
				break;
			case REVOKE:
				System.err.println("   REVOKE " + iu.toPayLoad() + " (" + iu.status + ")");
				synchronized (upcomingPhrases) {
					upcomingPhrases.remove(iu);
				}
				break;
			}
		}
	}
	
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
	}

	/**
	 * @param args
	 *
	public static void main(String[] args) {
		SynthesisModule sm = new SynthesisModule();
		List<PhraseIU> phrases = new ArrayList<PhraseIU>();
		phrases.add(new PhraseIU("Hallo Timo", PhraseIU.PhraseStatus.NORMAL));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.add(new PhraseIU("Wie geht's?", PhraseIU.PhraseStatus.PROJECTED));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.remove(1);
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);		
	}*/

}
