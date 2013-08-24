package done.inpro.system.carchase;

import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter4internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

public class HesitatingSynthesisIU extends SysInstallmentIU {

	public HesitatingSynthesisIU(String text) {
		super(text);
		// handle <hes> marker at the end separately
		boolean addFinalHesitation;
		if (tts.endsWith(" <hes>")) {
			addFinalHesitation = true;
			tts = tts.replaceAll(" <hes>$", "");
		} else 
			addFinalHesitation = false;
		groundedIn = MaryAdapter.getInstance().text2IUs(tts);
		// remove utterance final silences
		IU pred = groundedIn.get(groundedIn.size() - 1);
		while (((WordIU) pred).isSilence()) {
			groundedIn.remove(pred);
			pred.getSameLevelLink().removeAllNextSameLevelLinks(); // cut GRIN-NextSLLs
			pred = pred.getSameLevelLink();
		}
		if (addFinalHesitation) {
			HesitationIU hes = new HesitationIU(null);
			hes.shiftBy(pred.endTime());
			hes.connectSLL(pred);
			pred.setAsTopNextSameLevelLink("<hes>");
			groundedIn.add(hes);
		}
	}

	public void appendContinuation(List<WordIU> words) {
		WordIU oldLastWord = getFinalWord();
		WordIU newFirstWord = words.get(0);
		newFirstWord.connectSLL(oldLastWord);
		groundedIn.addAll(words);
/* TODO: pitch adaptation will have to be reworked
		// adapt pitch
		SysSegmentIU oldLastSegment = (SysSegmentIU) oldLastWord.getLastSegment();
		while (oldLastSegment != null && !oldLastSegment.isVoiced()) {
			oldLastSegment = (SysSegmentIU) oldLastSegment.getSameLevelLink();
		}
		SysSegmentIU newFirstSegment = (SysSegmentIU) newFirstWord.getFirstSegment();
		while (newFirstSegment != null && !newFirstSegment.isVoiced()) {
			newFirstSegment = (SysSegmentIU) newFirstSegment.getNextSameLevelLink();
		}
		// attain previous IU's pitch for a smoother transition to the next
		//if (oldLastSegment != null && newFirstSegment != null)
		//	oldLastSegment.attainPitch(newFirstSegment.getFirstVoicedlf0());
		 */
	}
	
	public static class HesitationIU extends WordIU implements IU.IUUpdateListener {
		@SuppressWarnings({ "unchecked", "rawtypes" }) // the cast for GRINs
		public HesitationIU(WordIU sll) {
			super("<hes>", sll, (List) inpro.incremental.unit.HesitationIU.protoHesitation.getSegments());
			if (sll != null) {
				shiftBy(sll.endTime());
				//Integer lastPitch = ((SysSegmentIU) sll.getLastSegment()).getLastPitchValue();
				//shiftPitchBy(lastPitch);
			}
			for (SegmentIU seg : getSegments()) {
				seg.addUpdateListener(this);
			}
			inpro.incremental.unit.HesitationIU.protoHesitation.scaleDeepCopyAndStartAtZero(1f); // create new IU substructure for the next protohesitation
		}
		
		@Override
		public void addNextSameLevelLink(IU iu) {
			super.addNextSameLevelLink(iu);
			setToZeroDuration();
		}

		private void setToZeroDuration() {
			for (IU iu : groundedIn) {
				((SysSegmentIU) iu).setNewDuration(0f);
			}
		}

		@Override
		public void update(IU updatedIU) {
			System.err.println("update in " + updatedIU);
			SysSegmentIU mUnit = (SysSegmentIU) groundedIn.get(1);
			// stretch the first segment of a completion if it comes in during the second segment (/m/) of the hesitation 
			// question: why in the world would this heuristic make sense?
			if (isOngoing() && updatedIU == mUnit && nextSameLevelLinks != null) {
				IU somethingbelow = nextSameLevelLinks.get(0).groundedIn().get(0);
				System.err.println(somethingbelow.toString());
				((SysSegmentIU) somethingbelow).stretch(1.2);
			}
		}

	}

	public static void main(String[] args) {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		HesitationIU hes = new HesitationIU(null);
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), MaryAdapter4internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
		dispatcher.shutdown();
	}

}
