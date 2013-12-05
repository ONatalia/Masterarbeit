package inpro.incremental.processor;

import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.HesitationIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.Progress;
import inpro.incremental.unit.PhraseBasedInstallmentIU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.synthesis.MaryAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private Any2PhraseIUWrapper words2phrase = new Any2PhraseIUWrapper(); 
	
	/** 
	 * This constructor is necessary for use with Configuration Management. 
	 * You should rather use SynthesisModule(SimpleMonitor.setupDispatcher()) 
	 * if you need to create your own synthesis module. 
	 */
	public SynthesisModule() {
		this(null);
	}
	
	/**
	 * @param speechDispatcher if in doubt, create one by calling SimpleMonitor.setupDispatcher()
	 */
	public SynthesisModule(DispatchStream speechDispatcher) {
		this.speechDispatcher = speechDispatcher;
		MaryAdapter.getInstance(); // preload mary
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		speechDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
	}
	
	/**
	 * please only send PhraseIUs or WordIUs; everything else will result in assertions failing
	 */
	@Override
	protected synchronized void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		boolean startPlayInstallment = false;
		for (EditMessage<?> em : edits) {
			logger.debug(em.getType() + " " + em.getIU().toPayLoad());
			PhraseIU phraseIU = words2phrase.getPhraseIU(em.getIU());
			switch (em.getType()) {
			case ADD:
				if (currentInstallment != null && !currentInstallment.isCompleted()) {
					WordIU choiceWord = currentInstallment.getFinalWord();
					// mark the ongoing utterance as non-final, (includes check whether there's still enough time)
					boolean canContinue = ((SysSegmentIU) choiceWord.getLastSegment()).setAwaitContinuation(true);
					if (canContinue) {
						currentInstallment.appendPhrase(phraseIU);
					} else { 
						currentInstallment = null;
					}
				} 
				if (currentInstallment == null || currentInstallment.isCompleted()) { // start a new installment
					if (phraseIU instanceof HesitationIU)
						currentInstallment = new PhraseBasedInstallmentIU((HesitationIU) phraseIU);
					else
						currentInstallment = new PhraseBasedInstallmentIU(phraseIU);
					startPlayInstallment = true;
				}
				appendNotification(currentInstallment, phraseIU);
				break;
			case REVOKE:
				if (currentInstallment == null || currentInstallment.isCompleted()) {
					logger.warn("phrase " + phraseIU + " was revoked but installment has been completed already, can't revoke anymore.");
				} else {
					if (!phraseIU.isUpcoming()) {
						logger.warn("phrase " + phraseIU + " is not upcoming anymore, can't revoke anymore. (send us an e-mail if you really need to revoke ongoing phrases)");
					} else {
						currentInstallment.revokePhrase(phraseIU);
					}
				}
				break;
			case COMMIT:
				// ensure that this phrase can be finished
				phraseIU.setFinal();
				if (currentInstallment == null) 
					break; // do nothing if the installment has been discarded already
				for (SysSegmentIU seg : currentInstallment.getSegments()) {
					// clear any locks that might block the vocoder from finishing the utterance phrase
					seg.setAwaitContinuation(false);			
				}
				currentInstallment = null; 
				startPlayInstallment = false; // to avoid a null pointer
				break;
			default:
				break;
			}
		}
		if (startPlayInstallment) {
			assert speechDispatcher != null;
			speechDispatcher.playStream(currentInstallment.getAudio(), false);
		}
		if (currentInstallment != null)
			rightBuffer.setBuffer(currentInstallment.getWords());
	}
	
	private void appendNotification(SysInstallmentIU installment, PhraseIU phrase) {
		IUUpdateListener listener = new NotifyCompletedOnOngoing(phrase);
		// add listener to first segment for UPCOMING/ONGOING updates
		phrase.getFirstSegment().addUpdateListener(listener);
		String updateposition = System.getProperty("proso.cond.updateposition", "end");
		if (updateposition.equals("end")) 
			installment.getFinalWord()
					.getLastSegment().getSameLevelLink() // attach to second-to-last segment of the last word
					.addUpdateListener(listener);
		else if (updateposition.equals("-1word"))
			((WordIU) installment.getFinalWord().getSameLevelLink())
			.getLastSegment().getSameLevelLink()
			.addUpdateListener(listener);
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
			.addUpdateListener(listener);
		}
	}
	
	/** notifies the given PhraseIU when the IU this is listening to is completed */
	class NotifyCompletedOnOngoing implements IUUpdateListener {
		PhraseIU completed;
		Progress previousProgress = null;
		NotifyCompletedOnOngoing(PhraseIU notify) {
			completed = notify;
		}
		/** @param updatingIU the SegmentIU that this listener is attached to in {@link SynthesisModule#appendNotification(SysInstallmentIU, PhraseIU)} */
		@Override
		public void update(IU updatingIU) {
			if (updatingIU.isCompleted() && updatingIU != completed.getFirstSegment()) { // make sure that the phrase is marked as completed even though not necessarily the last segment has been completed
                completed.setProgress(Progress.COMPLETED);
                previousProgress = completed.getProgress();
            }
			if (!completed.getProgress().equals(previousProgress))
				completed.notifyListeners();
			previousProgress = completed.getProgress();
		}
	}
	
	class Any2PhraseIUWrapper {
		Map<WordIU,PhraseIU> map = new HashMap<WordIU,PhraseIU>();
		PhraseIU getPhraseIU(WordIU w) {
			if (!map.containsKey(w)) {
				if ("<hes>".equals(w.toPayLoad()))
					map.put(w,  new HesitationIU());
				else 
					map.put(w, new PhraseIU(w));
			}
			return map.get(w);
		}
		PhraseIU getPhraseIU(IU iu) {
			return (iu instanceof PhraseIU) ? (PhraseIU) iu : getPhraseIU((WordIU) iu);
		}
	}
	
}
