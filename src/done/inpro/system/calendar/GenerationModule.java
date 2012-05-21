package done.inpro.system.calendar;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.hts.InteractiveHTSEngine;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

import done.inpro.system.calendar.NoiseThread.NoiseHandling;

import scalendar.adaptionmanager.AdaptionManager;
import scalendar.knowledgeobject.CalendarEvent;
import scalendar.spud.CalendarKnowledgeInterface;
import scalendar.spudmanager.SpudManager;
import scalendar.uttereanceobject.EventConflict;
import scalendar.uttereanceobject.MovedEvent;
import edu.rutgers.nlp.asciispec.grammar.jj.ParseException;
import edu.rutgers.nlp.spud.SPUD;

public class GenerationModule extends IUModule {

	private SpudManager nlg;
	private List<PhraseIU> phrases;
	
	public GenerationModule(SpudManager sm) {
		this.nlg = sm;
		phrases = new ArrayList<PhraseIU>();
		SPUD.setDebugLevel(30);
	}
	
	private IUUpdateListener phraseUpdateListener = new IUUpdateListener() {
		
		@Override
		public synchronized void update(IU updatedIU) {
			String projectedPhrase;
			if (updatedIU == null || updatedIU.isCompleted()) {
				if (AdaptionManager.getInstance().hasChanged()) {
					System.out.println("*************** CHANGE ****************");
					phrases.remove(phrases.size() - 1);
					String phrase = nlg.generateNextPhrase();
					if (phrase != null) {
						PhraseIU piu = new PhraseIU(phrase, PhraseIU.PhraseStatus.NORMAL); // add type: continuation or repair?
						piu.addUpdateListener(phraseUpdateListener);
						phrases.add(piu);
					} else {
						return;
					}
					// change back to initial values - hack for SigDial Paper
					AdaptionManager.getInstance().setLevelOfUnderstanding(3);
					AdaptionManager.getInstance().setVerbosityFactor(1);
					AdaptionManager.getInstance().hasChanged(); // we know it has, but sets flag to false again.
					projectedPhrase = nlg.simulateNextButOnePhrase();	
				} else {
					projectedPhrase = nlg.takeSimulatedAndSimulateNextPhrase();
					// give prev. IU normal status
					PhraseIU iu = phrases.get(phrases.size() - 1);
					iu.setStatus(PhraseIU.PhraseStatus.NORMAL);
					iu.addUpdateListener(phraseUpdateListener);
				} 	 
				
				if (projectedPhrase != null) {
					PhraseIU ppiu = new PhraseIU(projectedPhrase, PhraseIU.PhraseStatus.PROJECTED);
					ppiu.addUpdateListener(phraseUpdateListener);
					phrases.add(ppiu);
				} else {
					PhraseIU finalPhrase = phrases.get(phrases.size() - 1);
					// set phrase to final, this also removes any vocoder locks
					finalPhrase.setFinal();
				}				
				rightBuffer.setBuffer(phrases);
				rightBuffer.notify(iulisteners);
			}
			System.out.println("Current update id: " + new Integer(nlg.getCurrentUpdateId()).toString());
		}
	};
		
	/** only called on startup  */
	public void generate() {
		// for timing-measurements:
		Logger speedLogger = Logger.getLogger("speedlogger");
		long start = System.currentTimeMillis();
		
		String phrase = nlg.generateNextPhrase();
		PhraseIU piu = new PhraseIU(phrase, PhraseIU.PhraseStatus.NORMAL, PhraseIU.PhraseType.INITIAL);
		phrases.add(piu);
		
		String projectedPhrase = nlg.simulateNextButOnePhrase();
		PhraseIU ppiu = new PhraseIU(projectedPhrase, PhraseIU.PhraseStatus.PROJECTED);
		phrases.add(ppiu);
		
		// for timing-measurements:
		long duration = System.currentTimeMillis() - start;
		speedLogger.info("NLG for onset took: " + duration);
		
		rightBuffer.setBuffer(phrases);
		rightBuffer.notify(iulisteners);
		if (System.getProperty("proso.cond.onephraseahead", "true").equals("true")) {
			piu.addUpdateListener(phraseUpdateListener);
		}
		ppiu.addUpdateListener(phraseUpdateListener);
		System.out.println("generate is done");
	}
	
	/** set the stimulus (between 1 and 9) */
	public void setStimulus(int stimulus) {
		assert stimulus > 0 && stimulus < 10;
		switch (stimulus) {
		case 1: // --> 6 phrases
			final CalendarEvent event1 = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 10, 0), 2);
			final CalendarEvent event1conflict = new CalendarEvent("Zahn Arzt", new GregorianCalendar(2012, 4, 14, 10, 30), 1);
			nlg.setUtteranceObject(new EventConflict(event1, event1conflict));
			break;
		case 2: // --> 6 phrases
			final CalendarEvent event2 = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 10, 0), 2);
			// synthesis doesn't like the one-word phrase "äh" which is inserted for moved events on the same day
			final CalendarEvent event2changed = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 16, 9, 30), 2);
			nlg.setUtteranceObject(new MovedEvent(event2, event2changed));
			break;
		case 3: // --> 7 phrases
			final CalendarEvent event3 = new CalendarEvent("Austellungseröffnung", new GregorianCalendar(2012, 5, 20, 11, 00), 2);
			final CalendarEvent event3followup = new CalendarEvent("Sekt und Kringel", new GregorianCalendar(2012, 5, 20, 12, 00), 1);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event3, event3followup));
			break;
		case 4: // --> 7 phrases
			final CalendarEvent event4 = new CalendarEvent("Geschenk besorgen", new GregorianCalendar(2012, 3, 21, 17, 0), 1);
			final CalendarEvent event4followup = new CalendarEvent("Spieleabend bei Hanne", new GregorianCalendar(2012, 3, 22, 15, 0), 3);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event4, event4followup));
			break;
		case 5: // --> 6 phrases
			final CalendarEvent event5 = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 4, 10, 0), 2);
			final CalendarEvent event5changed = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 6, 12, 0), 2);
			nlg.setUtteranceObject(new MovedEvent(event5, event5changed));
			break;
		case 6: // --> 6 phrases
			final CalendarEvent event6 = new CalendarEvent("Schwimmen gehen", new GregorianCalendar(2012, 6, 6, 14, 0), 2);
			final CalendarEvent event6conflict = new CalendarEvent("Geburtstag Tante Ilse", new GregorianCalendar(2012, 6, 6, 15, 0), 4);
			nlg.setUtteranceObject(new EventConflict(event6, event6conflict));
			break;
		case 7: // --> 7 phrases
			final CalendarEvent event7 = new CalendarEvent("Semesterstart", new GregorianCalendar(2012, 3, 4, 8, 0), 12);
			final CalendarEvent event7followup = new CalendarEvent("Westend Party", new GregorianCalendar(2012, 3, 6, 21, 0), 3);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event7, event7followup));
			break;
		case 8: // --> 7 phrases
			final CalendarEvent event8 = new CalendarEvent("Zug nach München", new GregorianCalendar(2012, 10, 8, 13, 37), 5);
			final CalendarEvent event8followup = new CalendarEvent("Tagungsbeginn", new GregorianCalendar(2012, 10, 9, 9, 00), 1);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event8, event8followup));
			break;
		case 9: // --> 6 phrases
			final CalendarEvent event9 = new CalendarEvent("Besprechung mit Betreuer", new GregorianCalendar(2012, 6, 27, 14,0), 1);
			final CalendarEvent event9conflict = new CalendarEvent("Mensaführung", new GregorianCalendar(2012, 6, 27, 13, 00), 3);
			nlg.setUtteranceObject(new EventConflict(event9, event9conflict));
			break;
		default:
			throw new RuntimeException("illegal stimulus ID " + stimulus);
		}
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, ParseException {
		int stimulusID = 1;
		// for detailed timing-measurements:
		boolean measureTiming = false;
		boolean playNoise = true;
		NoiseHandling noiseHandling = NoiseHandling.regenerate;
		String knowledgeFile = "src/org/cocolab/inpro/domains/calendar/calendar.gs";

		// handle command line arguments if there are any
		if (args.length > 0) {
			try {
				stimulusID = Integer.parseInt(args[0]);
				knowledgeFile = args[1];
				measureTiming = Boolean.parseBoolean(args[2]);
				playNoise = Boolean.parseBoolean(args[3]);
				if (playNoise) {
					noiseHandling = NoiseHandling.parseParam(args[4]);
				}
			} catch (Exception e) {
				System.err.println("Command-line arguments (if any are given) must be:");
				System.err.println("1. stimulus ID (a number between 1 and 9)");
				System.err.println("2. location of the knowledge file (full path to calendar.gs)");
				System.err.println("3. whether to give detailed timing measurements (true/false)");
				System.err.println("4. whether to play noise (true/false)");
				System.err.println("5. the noise handling strategy (conditionA/conditionB/conditionC)");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		SpudManager spudmanager = new SpudManager(
				new CalendarKnowledgeInterface(), knowledgeFile);
		GenerationModule gm = new GenerationModule(spudmanager);

		gm.setStimulus(stimulusID);
		
		// output the non-incremental control condition
		if (System.getProperty("proso.cond.control", "false").equals("true")) {
			String fullUtterance = gm.nlg.generateCompleteUtteranceNonIncrementally();
			fullUtterance = fullUtterance.replaceAll(":", ""); // get rid of colons, which mess up pitch optimization
			fullUtterance = fullUtterance.replaceAll(" \\| ", " ");
			System.out.println(";; " + fullUtterance);
			SysInstallmentIU nonincremental = new SysInstallmentIU(fullUtterance);
			System.out.print(nonincremental.toMbrola());
			System.out.println("#");
			DispatchStream speechDispatcher = SimpleMonitor.setupDispatcher();
			speechDispatcher.playStream(nonincremental.getAudio(), true);
			speechDispatcher.waitUntilDone();
			System.exit(0);
		}
		
		final DispatchStream speechDispatcher = SimpleMonitor.setupDispatcher();
		final NoisySynthesisModule sm = new NoisySynthesisModule(speechDispatcher);

		gm.iulisteners = new ArrayList<PushBuffer>();
		gm.iulisteners.add(sm);
		
		Logger speedLogger = Logger.getLogger("speedlogger");
		if (measureTiming) {
			//pre-heat
			String fullUtterance = gm.nlg.generateCompleteUtteranceNonIncrementally();
			speedLogger.info(fullUtterance);
			gm.nlg.clear();
			// NEED TO RESET Stimulus here
			gm.setStimulus(stimulusID);
			long start = System.currentTimeMillis();
			fullUtterance = gm.nlg.generateCompleteUtteranceNonIncrementally();
			long duration = System.currentTimeMillis() - start;
			speedLogger.info("non-incremental NLG: " + duration);
			gm.nlg.clear();
			// and NEED TO RESET Stimulus here
			gm.setStimulus(stimulusID);
			
			fullUtterance = fullUtterance.replaceAll(" \\| ", " ");
			speedLogger.info(fullUtterance);
			
			start = System.currentTimeMillis();
			MaryAdapter.getInstance().text2audio(fullUtterance);
			duration = System.currentTimeMillis() - start;
			speedLogger.info("non-incremental synthesis (full synthesis): " + duration);
			
			InteractiveHTSEngine.returnIncrementalAudioStream = true;
			start = System.currentTimeMillis();
			MaryAdapter.getInstance().text2audio(fullUtterance);
			duration = System.currentTimeMillis() - start;
			speedLogger.info("non-incremental synthesis (ling processing only): " + duration);
			InteractiveHTSEngine.returnIncrementalAudioStream = false;
		}
		
		long start = System.currentTimeMillis();
		gm.generate();
		long duration = System.currentTimeMillis() - start;
		speedLogger.info("full onset took: " + duration);
		if (playNoise) {
			NoiseThread nt = new NoiseThread(AdaptionManager.getInstance(), sm, gm.phraseUpdateListener, noiseHandling);
			nt.start();
		}
		
		// dirty hack to force shutdown (should rather have a way to tear down AudioDispatchers in a sane way
		// something like dispatchStream.setTimeout(60000); // should shutdown the stream after a minute of no output 
		try {
			Thread.sleep(30000); // sleep for half a minute
			System.exit(0);
		} catch (InterruptedException e) {
		} 
	}

	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) { }

}
