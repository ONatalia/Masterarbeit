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
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.tts.MaryAdapter;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * concurrency: playNoise() and update() are synchronized, so do not try to call them from the same thread 
 */

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
	DispatchStream noiseDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	PhraseBasedInstallmentIU currentInstallment;
	
	@SuppressWarnings("unused")
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		noiseDispatcher = setupDispatcher2();
		speechDispatcher = SimpleMonitor.setupDispatcher();
		MaryAdapter.initializeMary(); // preload mary
		new SysInstallmentIU("Ein Satz zum Aufwärmen der Optimierungsmethoden."); // preheat mary
		// preheat HMM optimization and vocoding, hmpf.
		speechDispatcher.playInstallment(new SysInstallmentIU("Neuer Stimulus:"));
		speechDispatcher.waitUntilDone();
	}
	
	/**
	 * please only send PhraseIUs, everything else will result in assertions failing
	 */
	@Override
	protected synchronized void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		boolean startPlayInstallment = false;
		for (EditMessage<?> em : edits) {
			assert em.getIU() instanceof PhraseIU;
			final PhraseIU phraseIU = (PhraseIU) em.getIU();
			System.out.println("   " + em.getType() + " " + phraseIU.toPayLoad() + " (" + phraseIU.status + "; " + phraseIU.type + ")");
			switch (em.getType()) {
			case ADD:
				if (currentInstallment != null && !currentInstallment.isCompleted()) {
					WordIU choiceWord = currentInstallment.getFinalWord();
					// mark the ongoing utterance as non-final, check whether there's still enough time
					boolean canContinue = ((SysSegmentIU) choiceWord.getLastSegment()).setAwaitContinuation(true);
					if (canContinue) {
						currentInstallment.appendPhrase(phraseIU);
					} else { // 
						currentInstallment = null;
					}
				} 
				if (currentInstallment == null) { // start a new installment
					currentInstallment = new PhraseBasedInstallmentIU(phraseIU);
					startPlayInstallment = true;
				}
				appendNotification(currentInstallment, phraseIU);
				break;
			case REVOKE:
				// TODO
				break;
			default:
				break;
			}
		}
		if (startPlayInstallment)
			speechDispatcher.playInstallment(currentInstallment);
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
	protected synchronized void playNoiseSmart(String file) {
		noiseDispatcher.playFile(file, true);
		sleepy(300);
		// TODO: interrupt ongoing utterance 
		// stop after ongoing word, 
		currentInstallment.stopAfterOngoingWord();
		// (no need to keep reference to the ongoing utterance as we'll start a new one anyway)
		currentInstallment = null;
	}

	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoiseDumb(String file) {
		noiseDispatcher.playFile(file, true);
		sleepy(300);
		speechDispatcher.interruptPlayback();
		// wait until noiseDispatcher is done
		noiseDispatcher.waitUntilDone();
		sleepy(100);
		speechDispatcher.continuePlayback();
	}
	
	protected synchronized void playNoiseDeaf(String file) {
		noiseDispatcher.playFile(file, true);
	}

	void sleepy(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
