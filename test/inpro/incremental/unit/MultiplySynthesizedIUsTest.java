package inpro.incremental.unit;

import java.util.List;

import junit.framework.Assert;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter5internal;
import inpro.synthesis.hts.FullPFeatureFrame;
import inpro.synthesis.hts.FullPStream;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Test;

public class MultiplySynthesizedIUsTest {
	
	@Test public void testInvarianceOfFeatureFrameToVocoding() {
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		List<IU> words = MaryAdapter.getInstance().text2IUs("Mississippi");
		WordIU word = (WordIU) words.get(0);
		FullPStream stream = new IUBasedFullPStream(word.getFirstSegment());
		FullPFeatureFrame frame = stream.getNextFrame();
		String copy = frame.toString();
		stream.reset();
		dispatcher.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(stream, MaryAdapter5internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
		String newCopy = frame.toString();
		Assert.assertEquals(copy, newCopy);
		
	}

	@Test public void testAbilityToSynthesizeAnIUMultipleTimes() {
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		List<IU> words = MaryAdapter.getInstance().text2IUs("Dies sollte gleichbleibend gut klingen.");
		WordIU firstWord = (WordIU) words.get(0);
		WordIU lastWord = (WordIU) words.get(words.size() - 1);
		firstWord.connectSLL(lastWord);
		lastWord.getLastSegment().addUpdateListener(new BreakAfterXthSynthesisListener(15));
		dispatcher.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(firstWord.getFirstSegment()), MaryAdapter5internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
	}
	
	class BreakAfterXthSynthesisListener implements IUUpdateListener {
		int i;
		BreakAfterXthSynthesisListener(int times) {
			i = times;
		}
		public void update(IU updatedIU) {
			if (updatedIU.isCompleted()) {
				i--;
				System.err.println("reducing counter to " + i);
			}
			if (i <= 0) {
				IU iu = updatedIU;
				while (iu != null) {
					iu.nextSameLevelLinks = null;
					iu = iu.getFromNetwork("down[-1]");
				}
			}
		}
	}

}
