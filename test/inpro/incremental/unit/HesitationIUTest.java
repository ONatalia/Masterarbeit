package inpro.incremental.unit;

import inpro.apps.SimpleMonitor;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter4internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

public class HesitationIUTest {

	@Test(timeout=60000)
	public void test() {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		HesitationIU hes = new HesitationIU();
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), MaryAdapter4internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
		dispatcher.shutdown();
	}

}
