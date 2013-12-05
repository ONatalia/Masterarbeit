package inpro.incremental.processor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.incremental.processor.AdaptableSynthesisModule;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.incremental.unit.PhraseIU;

import org.apache.log4j.Logger;
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
		asm.addListener(new LabelWriter());
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
	 * assert that stopping results in the underlying phraseIUs being, well, what?
	 */
	@Test
	public void testPhraseIUProgressOnStop() throws InterruptedException {
		int[] delay = { 100, 200, 1500, 2100, 3500 };
		// we expect the first phrase to be ongoing for 300 and 700 ms, and to have completed after 2100 ms
		Progress[][] expectedProgressOfFirstPhrase = { { Progress.ONGOING }, 
											   { Progress.ONGOING }, 
											   { Progress.ONGOING, Progress.COMPLETED }, 
											   { Progress.ONGOING, Progress.COMPLETED }, 
											   { Progress.ONGOING, Progress.COMPLETED }, 
											 };
		// we expect the second phrase to only start after more than 700 ms, and to be completed before 3500 ms
		Progress[][] expectedProgressOfSecondPhrase = { 
												{ },
										        { },
										        { Progress.UPCOMING, Progress.ONGOING },										       
										        { Progress.UPCOMING, Progress.ONGOING },										       
										        { Progress.UPCOMING, Progress.ONGOING, Progress.COMPLETED },										       
											  };
		for (int i = 0; i < delay.length; i++) {
			PhraseIU firstPhrase = new PhraseIU("Ein ganz besonders langer"); // takes ~ 870 ms
			final List<Progress> firstPhraseProgressUpdates = new ArrayList<Progress>();
			firstPhrase.addUpdateListener(new IUUpdateListener() {@Override
				public void update(IU updatedIU) {
					firstPhraseProgressUpdates.add(updatedIU.getProgress());
				}});
			PhraseIU secondPhrase = new PhraseIU("und sehr komplizierter Satz."); // full utterances takes ~2700 ms
			final List<Progress> secondPhraseProgressUpdates = new ArrayList<Progress>();
			secondPhrase.addUpdateListener(new IUUpdateListener() {@Override
				public void update(IU updatedIU) {
					secondPhraseProgressUpdates.add(updatedIU.getProgress());
				}});
			myIUModule.addIUAndUpdate(firstPhrase);
			myIUModule.addIUAndUpdate(secondPhrase);
			Thread.sleep(delay[i]);
			asm.stopAfterOngoingWord();
			dispatcher.waitUntilDone();
			//System.err.println(firstPhraseProgressUpdates.toString());
			assertArrayEquals(firstPhraseProgressUpdates.toString(), expectedProgressOfFirstPhrase[i], firstPhraseProgressUpdates.toArray());
			assertArrayEquals(secondPhraseProgressUpdates.toString(), expectedProgressOfSecondPhrase[i], secondPhraseProgressUpdates.toArray());
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
		phrase.addUpdateListener(new IUUpdateListener() {
			@Override
			public void update(IU updatedIU) {
				Logger.getLogger(AdaptableSynthesisModuleUnitTest.class).info(
						"update message on IU " + updatedIU.toString() + 
						" with progress " + updatedIU.getProgress());
			}
		});
		myIUModule.addIUAndUpdate(phrase);
	}

}
