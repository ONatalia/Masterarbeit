package inpro.synthesis;

import static org.junit.Assert.*;

import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

public class SimpleSynthesis {

	@Test
	public void test() {
		System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		DispatchStream d = SimpleMonitor.setupDispatcher();
		d.playTTS("Dies ist ein Satz und Du machst Platz.", true);
		// wait for synthesis:
		d.waitUntilDone();
		List<IU> wordIUs = MaryAdapter.getInstance().text2IUs("Dies ist noch ein Satz, mach Platz.");
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(wordIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
		// wait for synthesis:
		d.waitUntilDone();
		assertTrue(true);
	}

}
