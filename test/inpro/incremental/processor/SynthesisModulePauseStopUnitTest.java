package inpro.incremental.processor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SynthesisModulePauseStopUnitTest extends SynthesisModuleAdaptationUnitTest {

	@Test(timeout=60000)
	public void testPauseResumeAfterOngoingWord() throws InterruptedException {
		int initialDelay = 400;
		int pauseDuration = 1000;
		startPhrase("eins zwei drei vier fünf sechs sieben acht neun zehn");
		Thread.sleep(initialDelay);
		asm.pauseAfterOngoingWord();
		Thread.sleep(pauseDuration);
		asm.resumePausedSynthesis();
	}
	
	@Test(timeout=60000)
	public void testPauseStopAfterOngoingWord() throws InterruptedException {
		int initialDelay = 400;
		int pauseDuration = 1000;
		startPhrase("eins zwei drei vier fünf sechs sieben acht neun zehn");
		Thread.sleep(initialDelay);
		asm.pauseAfterOngoingWord();
		Thread.sleep(pauseDuration);
		asm.stopAfterOngoingWord();
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
			assertArrayEquals("in round " + i + " first Phrase updates: " + firstPhraseProgressUpdates.toString(), 
					expectedProgressOfFirstPhrase[i], 
					firstPhraseProgressUpdates.toArray());
			assertArrayEquals("in round " + i + " second Phrase updates: " + secondPhraseProgressUpdates.toString(), 
					expectedProgressOfSecondPhrase[i], 
					secondPhraseProgressUpdates.toArray());
		}
	}

	// ignore tests from super class
	@Override public void testScaleTempo() {}	
}
