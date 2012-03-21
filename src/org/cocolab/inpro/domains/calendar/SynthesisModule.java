package org.cocolab.inpro.domains.calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.apps.util.CommonCommandLineParser;
import org.cocolab.inpro.apps.util.MonitorCommandLineParser;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;
import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;
import org.cocolab.inpro.incremental.unit.IU.Progress;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.tts.MaryAdapter;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * 
 * 
 * concurrency: 
 */

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
	DispatchStream noiseDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	IncrSysInstallmentIU currentInstallment;
	
	@SuppressWarnings("unused")
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		noiseDispatcher = setupDispatcher2();
		speechDispatcher = SimpleMonitor.setupDispatcher();
		MaryAdapter.initializeMary(); // preload mary
		new SysInstallmentIU("Ein Satz zum Aufwärmen der Optimierungsmethoden."); // preheat mary
	}
	
	/**
	 * please only send PhraseIUs, everything else will result in assertions failing
	 */
	@Override
	protected synchronized void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<?> em : edits) {
			assert em.getIU() instanceof PhraseIU;
			final PhraseIU phraseIU = (PhraseIU) em.getIU();
			System.out.println("   " + em.getType() + " " + phraseIU.toPayLoad() + " (" + phraseIU.status + "; " + phraseIU.type + ")");
			switch (em.getType()) {
			case ADD:
				if (currentInstallment != null && currentInstallment.isOngoing()) {
					WordIU choiceWord = currentInstallment.getFinalWord();
					// mark the ongoing utterance as non-final, check whether there's still enough time
					boolean canContinue = ((SysSegmentIU) choiceWord.getLastSegment()).setAwaitContinuation(true);
					if (canContinue) {
						String fullPhrase = currentInstallment.toPayLoad() + phraseIU.toPayLoad();
						currentInstallment.addAlternativeVariant(fullPhrase);
					} else {
						currentInstallment = new IncrSysInstallmentIU(phraseIU.toPayLoad());
						speechDispatcher.playStream(currentInstallment.getAudio(), true);
					}
				} else { // start a new installment
					currentInstallment = new IncrSysInstallmentIU(phraseIU.toPayLoad());
					speechDispatcher.playStream(currentInstallment.getAudio(), false);
				}
				appendNotification(currentInstallment, phraseIU);
			case REVOKE:
				// TODO
				break;
			}
		}
	}
	
	private static void appendNotification(SysInstallmentIU installment, PhraseIU phrase) {
		installment.getFinalWord()
					.getLastSegment().getSameLevelLink().getSameLevelLink()
					.addUpdateListener(new NotifyCompletedOnOngoing(phrase));
	}
	
	static class NotifyCompletedOnOngoing implements IUUpdateListener {
		PhraseIU completed;
		NotifyCompletedOnOngoing(PhraseIU notify) {
			completed = notify;
		}
		@Override
		public void update(IU updatedIU) {
			if (updatedIU.isOngoing()) {
				completed.setProgress(Progress.COMPLETED);
			}
		}
	}
	
	protected synchronized boolean noisy() {
		return noiseDispatcher.isSpeaking();
	}
	
	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoise(String file) {
		noiseDispatcher.playFile(file, true);
		// TODO: interrupt ongoing utterance 
		// stop after ongoing word, (no need to keep reference to the ongoing utterance as we'll start a new one anyway
		// should I call GenerationModule.update() or is the caller taking care of that?
	}
	
	/* wow, this is ugly. but oh well ... as long as it works */
	@SuppressWarnings("unused")
	public static DispatchStream setupDispatcher2() {
		ConfigurationManager cm = new ConfigurationManager(SimpleMonitor.class.getResource("config.xml"));
		MonitorCommandLineParser clp = new MonitorCommandLineParser(new String[] {
				"-S", "-M" // -M is just a placeholder here, it's immediately overridden in the next line:
			});
		clp.setInputMode(CommonCommandLineParser.DISPATCHER_OBJECT_2_INPUT);
		try {
			new SimpleMonitor(clp, cm);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return (DispatchStream) cm.lookup("dispatchStream2");
	}

}
