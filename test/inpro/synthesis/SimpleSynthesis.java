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
	public void testDE() {
		System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		DispatchStream d = SimpleMonitor.setupDispatcher();
		d.playTTS("Dies ist ein Satz und Du machst Platz.", true);
		// wait for synthesis:
		d.waitUntilDone();
		d.playStream(MaryAdapter.getInstance().text2audio("Dies ist noch ein Satz, mach Platz."), true);
		// wait for synthesis:
		d.waitUntilDone();
		List<? extends IU> wordIUs = MaryAdapter.getInstance().text2WordIUs("Dies ist noch ein Satz, mach Platz.");
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(wordIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
		// wait for synthesis:
		d.waitUntilDone();
		assertTrue(true);
	}

	@Test
	public void testEN() {
		System.setProperty("inpro.tts.language", "en_US");
		System.setProperty("inpro.tts.voice", "cmu-rms-hsmm");
		DispatchStream d = SimpleMonitor.setupDispatcher();
		d.playTTS("This is a long and complex sentence.", true);
		// wait for synthesis:
		d.waitUntilDone();
		d.playStream(MaryAdapter.getInstance().text2audio("This is another long and complex sentence."), true);
		// wait for synthesis:
		d.waitUntilDone();
		List<? extends IU> wordIUs = MaryAdapter.getInstance().text2WordIUs("This is another long and complex sentence.");
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(wordIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
		// wait for synthesis:
		d.waitUntilDone();
		assertTrue(true);
	}

}
