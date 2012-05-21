package done.inpro.system.carchase;

import java.util.List;

import javax.xml.bind.JAXBException;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.synthesis.MaryAdapter;

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
		try {
			groundedIn = MaryAdapter.getInstance().text2IUs(tts);
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
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
	
	private static final SysInstallmentIU protoHesitation;
	
	static { // setup and lengthen protoHesitation and add HMM features
		protoHesitation = new SysInstallmentIU("ähm");
		List<SysSegmentIU> segs = protoHesitation.getSegments();
		// lengthen segments very much
		segs.get(0).stretch(4);
		segs.get(1).stretch(8);
		// that way we get many feature frames
		//protoHesitation.addFeatureStreamToSegmentIUs();
		// shorten segments somewhat (we still have double the amount of frames to be able to lengthen the segment later on
		for (IU seg : protoHesitation.getSegments()) {
			((SysSegmentIU) seg).stretch(.5);
		}
	}
	
	public class HesitationIU extends WordIU implements IU.IUUpdateListener {
		@SuppressWarnings({ "unchecked", "rawtypes" }) // the cast for GRINs
		public HesitationIU(WordIU sll) {
			super("<hes>", sll, (List) protoHesitation.getSegments());
			if (sll != null) {
				shiftBy(sll.endTime());
			}
			for (SegmentIU seg : getSegments()) {
				seg.addUpdateListener(this);
			}
			protoHesitation.scaleDeepCopyAndStartAtZero(1f); // create new IU substructure for the next protohesitation
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
			if (isOngoing() && updatedIU == mUnit && nextSameLevelLinks != null) {
				((SysSegmentIU) nextSameLevelLinks.get(0).groundedIn().get(0)).stretch(1.2);
			}
			
		}

//		public static void main(String[] args) throws InterruptedException {
//			MaryAdapter.getInstance();
//			DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
//			HesitationIU hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
//			dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
//			hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
//		//	dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
//			IncrSysInstallmentIU installment = new IncrSysInstallmentIU("Am Ende der Straße <hes>");
//			//installment.addFeatureStreamToSegmentIUs();
//			installment.appendContinuation((new IncrSysInstallmentIU("links")).getWords());
//			dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(installment), true)), false);
//			Thread.sleep(5000);
//			dispatcher.shutdown();
//		}

	}

}
