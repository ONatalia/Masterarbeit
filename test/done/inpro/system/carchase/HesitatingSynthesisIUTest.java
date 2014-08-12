package done.inpro.system.carchase;

import inpro.apps.SimpleMonitor;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter5internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

import done.inpro.system.carchase.HesitatingSynthesisIU.HesitationIU;

public class HesitatingSynthesisIUTest {

	@Test
	public void test() {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		HesitationIU hes = new HesitationIU(null);
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), MaryAdapter5internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
		dispatcher.shutdown();
	}

}
