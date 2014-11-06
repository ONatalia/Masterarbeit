package inpro.synthesis;

import static org.junit.Assert.*;

import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

public class SimpleSynthesis {

	@Test
	public void test() {
		DispatchStream d = SimpleMonitor.setupDispatcher();
		d.playTTS("Dies ist ein Satz und Du machst Platz.", true);
		// wait for synthesis:
		d.waitUntilDone();
		List<IU> wordIUs = MaryAdapter.getInstance().text2IUs("Dies ist noch ein Satz, mach Platz.");
		d.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(wordIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
		// wait for synthesis:
		d.waitUntilDone();
		assertTrue(true);
	}

}
