package inpro.incremental.unit;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter5internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import org.junit.Assert;
import org.junit.Test;

import demo.inpro.synthesis.TreeStructuredInstallmentIU;

/** ensure that updates come while audio is rendered, not all together before or after delivery */
public class GradualSynthesisProgressTest {

	@Test(timeout=60000) 
	public void testEnglish() {
		System.setProperty("inpro.tts.language", "en_US");
		System.setProperty("inpro.tts.voice", "cmu-slt-hsmm");
		test("This is a very long and at least somewhat complex utterance.", 3000);
	}
	
	@Test(timeout=60000)
	public void testGerman() {
		System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		test("Nimm bitte das Kreuz ganz oben links in der Ecke.", 2000);
	}
	
	public void test(String tts, long minDuration) {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		TreeStructuredInstallmentIU installment = new TreeStructuredInstallmentIU(tts);
		MyListener listener = new MyListener(); 
		for (WordIU w : installment.getWords()) {
			w.addUpdateListener(listener);
			w.updateOnGrinUpdates();
		}
		dispatcher.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(installment), MaryAdapter5internal.getDefaultHMMData(), true)), false);
		dispatcher.waitUntilDone();
		// saying the utterance should take at least 1000 milliseconds
		Assert.assertTrue("too little time elapsed while speaking the following: \"" + tts + "\" (expected at least " + minDuration + " milliseconds.", listener.getDuration() > minDuration); 
		dispatcher.shutdown();
	}

	/** records the time between first and last occurrence */
	private class MyListener implements IUUpdateListener {
		long firstTime = -1;
		long lastTime;
		@Override
		public void update(IU updatedIU) {
			System.err.println(updatedIU);
			if (firstTime == -1)
				firstTime = System.currentTimeMillis();
			lastTime = System.currentTimeMillis();
		}
		public long getDuration() {
			return lastTime - firstTime;
		}
	}

}
