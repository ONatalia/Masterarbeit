package org.cocolab.inpro.incremental.unit;

import java.util.List;

import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.audio.DDS16kAudioInputStream;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.tts.MaryAdapter;
import org.cocolab.inpro.tts.hts.IUBasedFullPStream;
import org.cocolab.inpro.tts.hts.VocodingAudioStream;

public class HesitationIU extends WordIU {
	
	public static SysInstallmentIU protoHesitation;
	
	static { // setup and lengthen protoHesitation and add HMM features
		protoHesitation = new SysInstallmentIU("ähm");
		for (IU seg : protoHesitation.getSegments()) {
			((SysSegmentIU) seg).stretch(2);
		}		
		protoHesitation.addFeatureStreamToSegmentIUs();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) // the cast for GRINs
	public HesitationIU(WordIU sll) {
		super("<hes>", sll, (List) protoHesitation.getSegments());
		if (sll != null) {
			shiftBy(sll.endTime());
		}
		protoHesitation.scaleDeepCopyAndStartAtZero(1f); // create new IU substructure for the next protohesitation
	}

	public static void main(String[] args) throws InterruptedException {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		HesitationIU hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
		hes = new HesitationIU(null);// HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "b"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
	//	dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), false);
		SysInstallmentIU installment = new SysInstallmentIU("Am Ende der Straße <hes>");
		installment.addFeatureStreamToSegmentIUs();
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(installment), true)), false);
		Thread.sleep(5000);
		dispatcher.shutdown();
	}

}
