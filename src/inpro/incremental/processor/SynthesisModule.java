package inpro.incremental.processor;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseBasedInstallmentIU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.synthesis.MaryAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * concurrency: playNoise() and update() are synchronized, so do not try to call them from the same thread 
 */

public class SynthesisModule extends IUModule {

	protected DispatchStream speechDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	protected PhraseBasedInstallmentIU currentInstallment;
	
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
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
			final PhraseIU phraseIU;
			if (em.getIU() instanceof PhraseIU) {
				phraseIU = (PhraseIU) em.getIU();
			} else {
				phraseIU = new PhraseIU((WordIU) em.getIU());
			}
			System.out.println("   " + em.getType() + " " + phraseIU.toPayLoad() + " (" + phraseIU.getStatus() + "; " + phraseIU.getType() + ")");
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
				if (!completed.getType().equals(PhraseIU.PhraseType.FINAL))
					((SysSegmentIU) currentInstallment.getFinalWord().getLastSegment()).setAwaitContinuation(true);
				completed.setProgress(Progress.COMPLETED);
			}
		}
	}
	
}
