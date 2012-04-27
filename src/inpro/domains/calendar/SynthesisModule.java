package inpro.domains.calendar;

import inpro.apps.SimpleMonitor;
import inpro.apps.util.CommonCommandLineParser;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.synthesis.MaryAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * concurrency: playNoise() and update() are synchronized, so do not try to call them from the same thread 
 */

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
	DispatchStream noiseDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	PhraseBasedInstallmentIU currentInstallment;
	
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		noiseDispatcher = setupDispatcher2();
		speechDispatcher = SimpleMonitor.setupDispatcher();
		MaryAdapter.initializeMary(); // preload mary
		// preheat mary symbolic processing, HMM optimization and vocoding
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
	
	private void appendNotification(SysInstallmentIU installment, PhraseIU phrase) {
		String updateposition = System.getProperty("proso.cond.updateposition", "end");
		if (updateposition.equals("end")) 
			installment.getFinalWord()
					.getLastSegment().getSameLevelLink()
					.addUpdateListener(new NotifyCompletedOnOngoing(phrase));
		else if (updateposition.equals("-1word"))
			((WordIU) installment.getFinalWord().getSameLevelLink())
			.getLastSegment().getSameLevelLink()
			.addUpdateListener(new NotifyCompletedOnOngoing(phrase));
		else {
			int req;
			if (updateposition.equals("+1word"))
				req = 0;
			else if (updateposition.equals("+2word"))
				req = 1;
			else if (updateposition.equals("+3word"))
				req = 2;
			else
				throw new RuntimeException("proso.cond.updateposition was set to the invalid value " + updateposition);
				
			if (phrase.groundedIn().size() <= req) {
				logger.warn("cannot update on " + req + ", will update on " + (phrase.groundedIn().size() - 1) + " instead");
				req = phrase.groundedIn().size() - 1;
			}
			((WordIU) phrase.groundedIn().get(req))
			.getLastSegment().getSameLevelLink()
			.addUpdateListener(new NotifyCompletedOnOngoing(phrase));
		}
	}
	
	class NotifyCompletedOnOngoing implements IUUpdateListener {
		PhraseIU completed;
		NotifyCompletedOnOngoing(PhraseIU notify) {
			completed = notify;
		}
		@Override
		public void update(IU updatedIU) {
			if (updatedIU.isOngoing()) {
				// block vocoding from finishing synthesis before our completion is available
				if (!completed.type.equals(PhraseIU.PhraseType.FINAL))
					((SysSegmentIU) currentInstallment.getFinalWord().getLastSegment()).setAwaitContinuation(true);
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