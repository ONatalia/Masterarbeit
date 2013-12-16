package inpro.incremental.processor;

import static org.junit.Assert.*;

import inpro.apps.SimpleMonitor;
import inpro.incremental.processor.AdaptableSynthesisModule;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.PhraseIU;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class SynthesisModuleAdaptationUnitTest extends SynthesisModuleUnitTest {
	
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
	
	protected void startPhrase(String s) {
		PhraseIU phrase = new PhraseIU(s);
		phrase.preSynthesize();
		phrase.addUpdateListener(new IUUpdateListener() {
			@Override
			public void update(IU updatedIU) {
				Logger.getLogger(SynthesisModuleAdaptationUnitTest.class).info(
						"update message on IU " + updatedIU.toString() + 
						" with progress " + updatedIU.getProgress());
			}
		});
		myIUModule.addIUAndUpdate(phrase);
	}

	// ignore tests from super class
	@Override public void testLeftBufferUpdateWithSomeUtterances() {}
	@Override public void testLeftBufferUpdateWithPreSynthesis() {}	
	@Override public void testPreSynthesisTiming() {}	
	@Override public void testAddWordOnUpdate() {}
	@Override public void testHesitations() {}
	@Override public void testInternationalisation() {}
	
}
