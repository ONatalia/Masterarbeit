package inpro.incremental.processor;

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

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * concurrency: playNoise() and update() are synchronized, so do not try to call them from the same thread 
 */

public class SynthesisModule extends IUModule {

	@S4Component(type = DispatchStream.class)
	public final static String PROP_DISPATCHER = "dispatcher";
	
	
	protected DispatchStream speechDispatcher;
	
	protected PhraseBasedInstallmentIU currentInstallment;
	
	public SynthesisModule() {
		this(null);
	}
	
	public SynthesisModule(DispatchStream speechDispatcher) {
		this.speechDispatcher = speechDispatcher;
		MaryAdapter.initializeMary(); // preload mary
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		speechDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
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
				throw new NotImplementedException("phrases cannot yet be revoked from synthesis; check for our next release");
			default:
				break;
			}
		}
		if (startPlayInstallment)
			speechDispatcher.playStream(currentInstallment.getAudio());
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
	
	/** notifies the given PhraseIU when the IU this is listening to is completed */
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
