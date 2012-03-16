package org.cocolab.inpro.domains.calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;
import org.cocolab.inpro.incremental.unit.IU.Progress;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.tts.MaryAdapter;

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
//	DispatchStream noiseDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		speechDispatcher = SimpleMonitor.setupDispatcher();
		MaryAdapter.initializeMary(); // preload mary
	}
	
	/* dummy TTS
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
	}*/
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		for (EditMessage<?> em : edits) {
			final PhraseIU phraseIU = (PhraseIU) em.getIU();
			switch (em.getType()) {
			case ADD:
				IncrSysInstallmentIU instIU = new IncrSysInstallmentIU(phraseIU.toPayLoad());
				instIU.getFinalWord().getLastSegment().addUpdateListener(new IUUpdateListener() {
					@Override
					public void update(IU updatedIU) {
						if (updatedIU.isOngoing()) {
							phraseIU.setProgress(Progress.COMPLETED);
						}
					}
				});
				speechDispatcher.playStream(instIU.getAudio(), false);
				phraseIU.setProgress(Progress.ONGOING);
				System.err.println("ADD " + phraseIU.toPayLoad() + " (" + phraseIU.status + ")");
				break;
			case REVOKE:
				System.err.println("   REVOKE " + phraseIU.toPayLoad() + " (" + phraseIU.status + ")");
				break;
			}
		}
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
