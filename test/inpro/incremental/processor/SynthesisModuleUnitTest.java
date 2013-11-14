package inpro.incremental.processor;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.HesitationIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.IU.IUUpdateListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SynthesisModuleUnitTest {

	private static String[][] testList = {
		{ "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun" }, 
		{ "Nimm bitte das Kreuz und lege es in den Kopf des Elefanten." },
		{ "Nimm bitte das Kreuz", "und lege es in den Kopf des Elefanten." },
		{ "Nimm das Kreuz,", "das rote Kreuz,", "und lege es in den Kopf des Elefanten."}, 
	};
	
	public DispatchStream dispatcher;
	public TestIUModule myIUModule;
	
	@Before
	public void setupMinimalSynthesisEnvironment() {
        System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		dispatcher = SimpleMonitor.setupDispatcher();
		myIUModule = new TestIUModule();
		myIUModule.addListener(new SynthesisModule(dispatcher));
	}
	
	@After
	public void waitForSynthesis() {
		dispatcher.waitUntilDone();
		myIUModule.reset();
	}
	
	/**  
	 * test the addition of a few phrases as specified in testList
	 */
	@Test
	public void testLeftBufferUpdateWithSomeUtterances() {
		for (String[] list : testList) {
			for (String s : list) {
				myIUModule.addIUAndUpdate(new PhraseIU(s));
			}
			dispatcher.waitUntilDone();
			myIUModule.reset();
		}
	}
	
	/**
	 * test pre-synthesis (mostly that it works, it will be hard to judge whether this is successful)
	 */
	@Test
	public void testLeftBufferUpdateWithPreSynthesis() {
		for (String[] list : testList) {
			for (String s : list) {
				PhraseIU phrase = new PhraseIU(s);
				phrase.preSynthesize();
				myIUModule.addIUAndUpdate(phrase);
			}
			dispatcher.waitUntilDone();
			myIUModule.reset();
		}
	}
	
	/**
	 * test timing consecutivity(?) of PhraseIUs when pre-synthesis is used
	 */
	@Test
	public void testPreSynthesisTiming() {
		PhraseIU initialPhrase = new PhraseIU("Dies ist ein");
		initialPhrase.preSynthesize();
		PhraseIU continuationPhrase = new PhraseIU("komplizierter Satz.");
		continuationPhrase.preSynthesize();
		assertTrue("continuation should start at 0 before it is being synthesized", continuationPhrase.startTime() == 0);
		myIUModule.addIUAndUpdate(initialPhrase);
		myIUModule.addIUAndUpdate(continuationPhrase);
		assertTrue("continuation should start immediately after initial phrase", initialPhrase.endTime() == continuationPhrase.startTime());
		dispatcher.waitUntilDone();
		assertTrue("continuation should start immediately after initial phrase (even after uttering)", initialPhrase.startTime() != continuationPhrase.startTime());
	}
	
	/**
	 * test whether it's possible to add the next chunk only on update of the previous chunk
	 */
	@Test
	public void testAddWordOnUpdate() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(1);
		for (String s : testList[0]) {
			PhraseIU phrase = new PhraseIU(s);
			phrase.addUpdateListener(new IUUpdateListener() {
            	int counter = 0;
				@Override
				public void update(IU updatedIU) {
					counter++;
					if (counter == 1)
						semaphore.release();
					System.err.println("notified of " + updatedIU.getProgress() + " in " + updatedIU.toPayLoad() + " counter: " + counter);
				}
            });
			myIUModule.addIUAndUpdate(phrase);
            semaphore.acquire();
		}
	}

	/**
	 * test hesitations by adding the content after a hesitations after increasing delays
	 */
	@Test 
	public void testHesitations() throws InterruptedException {
		String s1 = "Und dann";
		String s2 = "weiter";
		for (int delay = 200; delay < 600; delay+= 40) {
			myIUModule.addIUAndUpdate(new PhraseIU(s1));
			myIUModule.addIUAndUpdate(new HesitationIU());
			Thread.sleep(delay);
			myIUModule.addIUAndUpdate(new PhraseIU(s2));
			dispatcher.waitUntilDone();
			myIUModule.reset();
		}
	}
	
	/**
	 * test en_US and en_GB
	 */
	@Test
	public void testInternationalisation() {
		String voice = System.getProperty("inpro.tts.voice");
		String language = System.getProperty("inpro.tts.language");
		System.setProperty("inpro.tts.voice", "dfki-prudence-hsmm");
        System.setProperty("inpro.tts.language", "en_GB");
        myIUModule.addIUAndUpdate(new PhraseIU("I can also speak in British English."));
        dispatcher.waitUntilDone();
		myIUModule.reset();
		System.setProperty("inpro.tts.voice", "cmu-slt-hsmm");
        System.setProperty("inpro.tts.language", "en_US");
        myIUModule.addIUAndUpdate(new PhraseIU("I can also speak with an American accent."));
        System.setProperty("inpro.tts.voice", voice);
        System.setProperty("inpro.tts.language", language);
	}

	protected static class TestIUModule extends IUModule {
		protected void leftBufferUpdate(Collection<? extends IU> ius, 
				List<? extends EditMessage<? extends IU>> edits) { } // do nothing, this is only a source of IUs
		void addIUAndUpdate(IU iu) {
			rightBuffer.addToBuffer(iu);
			notifyListeners();
		}
		
		@Override
		public void reset() {
			rightBuffer.setBuffer(null, null);
		}
	}

}