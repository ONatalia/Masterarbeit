package inpro.incremental.processor;

import static org.junit.Assert.*;
import inpro.apps.SimpleMonitor;
import inpro.incremental.processor.AdaptableSynthesisModule;
import inpro.incremental.unit.PhraseIU;

import org.junit.Before;
import org.junit.Test;

public class AdaptableSynthesisModuleUnitTest extends SynthesisModuleUnitTest {
	
	AdaptableSynthesisModule asm;
	
	@Override
	@Before
	public void setupMinimalSynthesisEnvironment() {
        System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		dispatcher = SimpleMonitor.setupDispatcher();
		myIUModule = new TestIUModule();
		asm = new AdaptableSynthesisModule(dispatcher);
		myIUModule.addListener(asm);
	}

	/**
	 * assert that aborting after the ongoing phoneme does not take longer than 600 ms 
	 * (this test uses digits, which should not last much longer than 600 ms to say) 
	 * in addition, assert that aborting something that has already ended does not fail
	 */
	@Test
	public void testStopAfterOngoingWord() throws InterruptedException {
		for (int initialDelay = 300; initialDelay < 4000; initialDelay += 300) {
			startPhrase("eins zwei drei vier fünf sechs sieben acht neun zehn");
			Thread.sleep(initialDelay);
			long timeBeforeAbort = System.currentTimeMillis();
			asm.stopAfterOngoingWord();
			dispatcher.waitUntilDone();
			long timeUntilAbort = System.currentTimeMillis() - timeBeforeAbort;
			assertTrue(Long.toString(timeUntilAbort), timeUntilAbort < 600);
		}
	}

	/**
	 * assert that aborting after the ongoing phoneme does not take longer than 250 ms 
	 * in addition, assert that aborting something that has already ended does not fail
	 */
	@Test
	public void testStopAfterOngoingPhoneme() throws InterruptedException {
		for (int initialDelay = 300; initialDelay < 4000; initialDelay += 300) {
			startPhrase("eins zwei drei vier fünf sechs sieben acht neun zehn");
			Thread.sleep(initialDelay);
			long timeBeforeAbort = System.currentTimeMillis();
			asm.stopAfterOngoingPhoneme();
			dispatcher.waitUntilDone();
			long timeUntilAbort = System.currentTimeMillis() - timeBeforeAbort;
			assertTrue(Long.toString(timeUntilAbort), timeUntilAbort < 250);
		}
	}

	/**
	 * test that scaling works as expected (scaling error is within 10%)
	 */
	@Test
	public void testScaleTempo() {
		String textKurz = "eins zwei drei vier fünf";
		// get the standard duration
		startPhrase(textKurz);
		long timeBeforeSynthesis = System.currentTimeMillis();
		dispatcher.waitUntilDone();
		long timeForNormalSynthesis = System.currentTimeMillis() - timeBeforeSynthesis;
		// now mesure scaled synthesis
		double[] scalingFactors = {0.41, 0.51, 0.64, 0.8, 1.0, 1.25, 1.56, 1.95, 2.44};
		for (double scalingFactor : scalingFactors) {
			startPhrase(textKurz);
			timeBeforeSynthesis = System.currentTimeMillis();
			asm.scaleTempo(scalingFactor);
			dispatcher.waitUntilDone();
			long timeForSlowSynthesis = System.currentTimeMillis() - timeBeforeSynthesis;
			double actualFactor = (double) timeForSlowSynthesis / (double) timeForNormalSynthesis;
			double deviationOfFactor = actualFactor / scalingFactor;
			assertTrue(Double.toString(actualFactor) + " but should have been " + Double.toString(scalingFactor), 
					deviationOfFactor < 1.1 && deviationOfFactor > 0.91);
		}
	}
	
	private void startPhrase(String s) {
		PhraseIU phrase = new PhraseIU(s);
		phrase.preSynthesize();
		myIUModule.addIUAndUpdate(phrase);
	}

}
