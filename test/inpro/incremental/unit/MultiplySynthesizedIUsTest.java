package inpro.incremental.unit;

import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter5internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

public class MultiplySynthesizedIUsTest {

	@Test public void testAbilityToSynthesizeAnIUMultipleTimes() {
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		List<IU> words = MaryAdapter.getInstance().text2IUs("Mississippi");
		WordIU word = (WordIU) words.get(0);
		word.connectSLL(word);
		dispatcher.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(word.getFirstSegment()), MaryAdapter5internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
	}

}
