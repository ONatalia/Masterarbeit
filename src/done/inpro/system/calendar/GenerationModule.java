package done.inpro.system.calendar;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.sink.TEDviewNotifier;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.SysInstallmentIU;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.hts.InteractiveHTSEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.soa.incremental.nlg.adaptionmanager.AdaptionManager;
import org.soa.incremental.nlg.knowledgeobject.CalendarEvent;
import org.soa.incremental.nlg.spud.CalendarKnowledgeInterface;
import org.soa.incremental.nlg.spudmanager.SpudManager;
import org.soa.incremental.nlg.uttereanceobject.EventConflict;
import org.soa.incremental.nlg.uttereanceobject.MovedEvent;

import done.inpro.system.calendar.NoiseThread.NoiseHandling;
import edu.rutgers.nlp.spud.SPUD;

public class GenerationModule extends IUModule {

	private SpudManager nlg;
	private List<CIU> chunks;
	
	public GenerationModule(SpudManager sm) {
		this.nlg = sm;
		this.nlg.setPrePlanningSteps(6);
		chunks = new ArrayList<ChunkIU>();
		SPUD.setDebugLevel(30);
	}
	
	/*private IUUpdateListener ChunkUpdateListener = new IUUpdateListener() {
		
		@Override
		public synchronized void update(IU updatedIU) {
			assert logger != null;
			logger.warn("update on IU " + (updatedIU != null ? updatedIU.toString() : "null"));
			String projectedChunk;
			if (updatedIU == null || updatedIU.isCompleted()) {
				if (AdaptionManager.getInstance().hasChanged()) {
					// --> this is usually the case when noise has been played
					System.out.println("*************** CHANGE ****************");
					chunks.remove(chunks.size() - 1);
					nlg.invalidatePreplanCache();
					nlg.preplanIncrements();
					String chunk = nlg.takePreplannedAndPreplanNextIncrements();
					if (chunk != null) {
						ChunkIU piu = new ChunkIU(chunk); // add type: continuation or repair?
						piu.addUpdateListener(chunkUpdateListener);
						chunks.add(piu);
					} else {
						return;
					}
					// change back to initial values - hack for SigDial Paper
					AdaptionManager.getInstance().setLevelOfUnderstanding(3);
					AdaptionManager.getInstance().setVerbosityFactor(1);
					AdaptionManager.getInstance().hasChanged(); // we know it has, but sets flag to false again.
					//nlg.preplanIncrements();
					projectedChunk = nlg.peekPreplannedIncrement(0);
				} else {
					nlg.takePreplannedAndPreplanNextIncrements(); //consume the previously peeked increment
					projectedChunk = nlg.peekPreplannedIncrement(0);
					// give prev. IU normal status
					ChunkIU iu = chunks.get(chunks.size() - 1);
					iu.addUpdateListener(chunkUpdateListener);
				} 	 
				
				if (projectedChunk != null) {
					ChunkIU ppiu = new ChunkIU(projectedChunk);
					ppiu.addUpdateListener(chunkUpdateListener);
					chunks.add(ppiu);
				} else {
					ChunkIU finalChunk = chunks.get(chunks.size() - 1);
					// set chunk to final, this also removes any vocoder locks
					finalChunk.setFinal();
				}
				rightBuffer.setBuffer(chunks);
				rightBuffer.notify(iulisteners);
			}
			System.out.println("Current update id: " + nlg.getCurrentUpdateId());
		}
	};
*/	

	private IUUpdateListener chunkUpdateListener_new = new IUUpdateListener() {
		
		@Override
		public synchronized void update(IU updatedIU) {
			ChunkIU piu, ppiu;
			
			System.out.println("update on IU " + (updatedIU != null ? updatedIU.toString() : "null"));
			
			if (updatedIU == null || updatedIU.isCompleted()) {
				
				if (updatedIU != null && updatedIU.isCompleted()) {
					ChunkIU ipiu = (ChunkIU)updatedIU;
					if (!ipiu.completionNotified) {
						ipiu.completionNotified = true;
					} else {
						System.out.println("Redundant notification of completion of IU.");
						return;
					}
				}
				
				if (AdaptionManager.getInstance().hasChanged()) {
					System.out.println("*************CHANGE************");
					chunks.remove(chunks.size() - 1);
					chunks.remove(chunks.size() - 1);
					nlg.invalidatePreplanCache();
					nlg.preplanIncrements();

					String chunk = nlg.peekPreplannedIncrement(0); //takePreplannedAndPreplanNextIncrements();
					String chunk2 = nlg.peekPreplannedIncrement(1);

					if (chunk != null) {
						if (chunk2 != null) {
							piu = new ChunkIU(chunk, ChunkIU.ChunkType.NONFINAL);
							ppiu = new ChunkIU(chunk2, ChunkIU.ChunkType.NONFINAL);
							chunks.add(piu);
							chunks.add(ppiu);
							ppiu.addUpdateListener(chunkUpdateListener_new);
						} else {
							piu = new ChunkIU(chunk, ChunkIU.ChunkType.FINAL);
							chunks.add(piu);
						}
						piu.addUpdateListener(ChunkUpdateListener_new);
					} else {
						return;
					}
					AdaptionManager.getInstance().setLevelOfUnderstanding(3);
					AdaptionManager.getInstance().setVerbosityFactor(1);
					AdaptionManager.getInstance().hasChanged(); // we know it has, but sets flag to false again.	
				} else {
					System.out.println("***********NO CHANGE***********");
					nlg.takePreplannedAndPreplanNextIncrements(); //consume the previously peeked increment
					String lookaheadChunk = nlg.peekPreplannedIncrement(1);
					System.out.println("Lookahead: " +lookaheadChunk);
					if (lookaheadChunk != null) {
						ppiu = new ChunkIU(lookaheadChunk, ChunkIU.ChunkType.NONFINAL);
						// give prev. IU normal status
						chunks.get(chunks.size() - 1).addUpdateListener(chunkUpdateListener_new);
						chunks.add(ppiu);
					} else {
						// set chunk to final, this also removes any vocoder locks
						chunks.get(chunks.size() - 1).setFinal();
					}
				}	
			}
			System.out.println("-------");
			for (ChunkIU c : chunks) {
				System.out.println(c);
			}
			System.out.println("-------");
			
			rightBuffer.setBuffer(chunks);
			rightBuffer.notify(iulisteners);
		}
	};
		
	/** only called on startup  */
	/*public void generate() {
		// for timing-measurements:
		Logger speedLogger = Logger.getLogger("speedlogger");
		long start = System.currentTimeMillis();
		
		String chunk = nlg.generateNextIncrement();
		ChunkIU piu = new ChunkIU(chunk, ChunkIU.ChunkType.NONFINAL);
		chunks.add(piu);
		
		nlg.preplanIncrements();
		String projectedChunk = nlg.peekPreplannedIncrement(0);
		ChunkIU ppiu = new ChunkIU(projectedChunk);
		chunks.add(ppiu);
		
		// for timing-measurements:
		long duration = System.currentTimeMillis() - start;
		speedLogger.info("NLG for onset took: " + duration);
		
		rightBuffer.setBuffer(chunks);
		rightBuffer.notify(iulisteners);
		//if (System.getProperty("proso.cond.onePhraseahead", "true").equals("true")) {
		piu.addUpdateListener(chunkUpdateListener);
		//}
		ppiu.addUpdateListener(chunkUpdateListener);
		System.out.println("generate is done");
	}*/
	
	public void generate_new() {
		nlg.preplanIncrements();
		String chunk = nlg.peekPreplannedIncrement(0);// takePreplannedAndPreplanNextIncrements();
		ChunkIU piu = new ChunkIU(chunk, ChunkIU.ChunkType.NONFINAL);
		piu.addUpdateListener(chunkUpdateListener_new);
		chunks.add(piu);
		
		String chunk2 = nlg.peekPreplannedIncrement(1);
		ChunkIU ppiu = new ChunkIU(chunk2, ChunkIU.ChunkType.NONFINAL);
		chunks.add(ppiu);
		
		rightBuffer.setBuffer(chunks);
		rightBuffer.notify(iulisteners);
	}
	
	/** set the stimulus (between 1 and 9) */
	public void setStimulus(int stimulus) {
		assert stimulus > 0 && stimulus < 10;
		switch (stimulus) {
		case 1: // --> 6 Chunks
			final CalendarEvent event1 = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 10, 0), 2);
			final CalendarEvent event1conflict = new CalendarEvent("Zahn Arzt", new GregorianCalendar(2012, 4, 14, 10, 30), 1);
			nlg.setUtteranceObject(new EventConflict(event1, event1conflict));
			break;
		case 2: // --> 6 Chunks
			final CalendarEvent event2 = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 10, 0), 2);
			// synthesis doesn't like the one-word Chunk "äh" which is inserted for moved events on the same day
			final CalendarEvent event2changed = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 16, 9, 30), 2);
			nlg.setUtteranceObject(new MovedEvent(event2, event2changed));
			break;
		case 3: // --> 7 Chunks
			final CalendarEvent event3 = new CalendarEvent("Austellungseröffnung", new GregorianCalendar(2012, 5, 20, 11, 00), 2);
			final CalendarEvent event3followup = new CalendarEvent("Sekt und Kringel", new GregorianCalendar(2012, 5, 20, 12, 00), 1);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event3, event3followup));
			break;
		case 4: // --> 7 Chunks
			final CalendarEvent event4 = new CalendarEvent("Geschenk besorgen", new GregorianCalendar(2012, 3, 21, 17, 0), 1);
			final CalendarEvent event4followup = new CalendarEvent("Spieleabend bei Hanne", new GregorianCalendar(2012, 3, 22, 15, 0), 3);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event4, event4followup));
			break;
		case 5: // --> 6 Chunks
			final CalendarEvent event5 = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 4, 10, 0), 2);
			final CalendarEvent event5changed = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 6, 12, 0), 2);
			nlg.setUtteranceObject(new MovedEvent(event5, event5changed));
			break;
		case 6: // --> 6 Chunks
			final CalendarEvent event6 = new CalendarEvent("Schwimmen gehen", new GregorianCalendar(2012, 6, 6, 14, 0), 2);
			final CalendarEvent event6conflict = new CalendarEvent("Geburtstag Tante Ilse", new GregorianCalendar(2012, 6, 6, 15, 0), 4);
			nlg.setUtteranceObject(new EventConflict(event6, event6conflict));
			break;
		case 7: // --> 7 Chunks
			final CalendarEvent event7 = new CalendarEvent("Semesterstart", new GregorianCalendar(2012, 3, 4, 8, 0), 12);
			final CalendarEvent event7followup = new CalendarEvent("Westend Party", new GregorianCalendar(2012, 3, 6, 21, 0), 3);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event7, event7followup));
			break;
		case 8: // --> 7 Chunks
			final CalendarEvent event8 = new CalendarEvent("Zug nach München", new GregorianCalendar(2012, 10, 8, 13, 37), 5);
			final CalendarEvent event8followup = new CalendarEvent("Tagungsbeginn", new GregorianCalendar(2012, 10, 9, 9, 00), 1);
			nlg.setUtteranceObject(GenerateStimuli.createUpcomingEvents(event8, event8followup));
			break;
		case 9: // --> 6 Chunks
			final CalendarEvent event9 = new CalendarEvent("Besprechung mit Betreuer", new GregorianCalendar(2012, 6, 27, 14,0), 1);
			final CalendarEvent event9conflict = new CalendarEvent("Mensaführung", new GregorianCalendar(2012, 6, 27, 13, 00), 3);
			nlg.setUtteranceObject(new EventConflict(event9, event9conflict));
			break;
		default:
			throw new RuntimeException("illegal stimulus ID " + stimulus);
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		
		int stimulusID = 1;
		// for detailed timing-measurements:
		boolean measureTiming = false;
		boolean playNoise = true;
		NoiseHandling noiseHandling = NoiseHandling.regenerate;
		String knowledgeFile = "src/done/inpro/system/calendar/calendar.gs";

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
		
		// normal program flow
		final DispatchStream speechDispatcher = SimpleMonitor.setupDispatcher();
		final NoisySynthesisModule synthesisModule = new NoisySynthesisModule(speechDispatcher);
		//TEDviewNotifier ted = new inpro.incremental.sink.TEDviewNotifier();
		//ted.setTEDadapter("localhost", 2000);
		//synthesisModule.addListener(ted);
	
		gm.addListener(synthesisModule);
		
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
		gm.generate_new();
		long duration = System.currentTimeMillis() - start;
		speedLogger.info("full onset took: " + duration);
		if (playNoise) {
			NoiseThread nt = new NoiseThread(AdaptionManager.getInstance(), synthesisModule, gm.ChunkUpdateListener_new, noiseHandling);
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

	public SpudManager getNLG() {
		return this.nlg;
	}

}
