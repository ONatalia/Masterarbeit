package inpro.incremental.unit;

import inpro.apps.SimpleMonitor;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import java.util.List;


public class HesitationIU extends WordIU implements IU.IUUpdateListener {
	
	public static SysInstallmentIU protoHesitation;
	
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

	public static void main(String[] args) throws InterruptedException {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		HesitationIU hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
		hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
	//	dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
		IncrSysInstallmentIU installment = new IncrSysInstallmentIU("Am Ende der Straße <hes>");
		//installment.addFeatureStreamToSegmentIUs();
		installment.appendContinuation((new IncrSysInstallmentIU("links")).getWords());
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(installment), true)), false);
		Thread.sleep(5000);
		dispatcher.shutdown();
	}

}
