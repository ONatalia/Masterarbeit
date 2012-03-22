package org.cocolab.inpro.domains.calendar;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;

import scalendar.adaptionmanager.AdaptionManager;
import scalendar.knowledgeobject.CalendarEvent;
import scalendar.spud.CalendarKnowledgeInterface;
import scalendar.spudmanager.SpudManager;
import scalendar.uttereanceobject.EventConflict;
import scalendar.uttereanceobject.MovedEvent;
import scalendar.uttereanceobject.UpcomingEvents;
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
					AdaptionManager.getInstance().setVerbosityFactor(0);
					AdaptionManager.getInstance().hasChanged(); // we now it has, but sets flag to false again.
					projectedPhrase = nlg.simulateNextButOnePhrase();	
				} else {
					projectedPhrase = nlg.takeSimulatedAndSimulateNextPhrase();
					// give prev. IU normal status
					PhraseIU iu = phrases.get(phrases.size() - 1);
					iu.status = PhraseIU.PhraseStatus.NORMAL;
					iu.addUpdateListener(phraseUpdateListener);
				} 	 
				
				if (projectedPhrase != null) {
					PhraseIU ppiu = new PhraseIU(projectedPhrase, PhraseIU.PhraseStatus.PROJECTED);
					ppiu.addUpdateListener(phraseUpdateListener);
					phrases.add(ppiu);
				} else {
					phrases.get(phrases.size() - 1).type = PhraseIU.PhraseType.FINAL;
				}				
				rightBuffer.setBuffer(phrases);
				rightBuffer.notify(iulisteners);
			}
			System.out.println("Current update id: " + new Integer(nlg.getCurrentUpdateId()).toString());
		}
	};
		
	/** only called on startup  */
	public void generate() {
		String phrase = nlg.generateNextPhrase();
		PhraseIU piu = new PhraseIU(phrase, PhraseIU.PhraseStatus.NORMAL, PhraseIU.PhraseType.INITIAL);
		phrases.add(piu);
		
		String projectedPhrase = nlg.simulateNextButOnePhrase();
		PhraseIU ppiu = new PhraseIU(projectedPhrase, PhraseIU.PhraseStatus.PROJECTED);
		phrases.add(ppiu);
		
		rightBuffer.setBuffer(phrases);
		rightBuffer.notify(iulisteners);
		piu.addUpdateListener(phraseUpdateListener);
		ppiu.addUpdateListener(phraseUpdateListener);
		System.out.println("generate is done");
	}
	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws FileNotFoundException, ParseException {
		SpudManager spudmanager = new SpudManager(
				new CalendarKnowledgeInterface(),
				"src/org/cocolab/inpro/domains/calendar/calendar.gs");
		GenerationModule gm = new GenerationModule(spudmanager);
		final SynthesisModule sm = new SynthesisModule();

		gm.iulisteners = new ArrayList<PushBuffer>();
		gm.iulisteners.add(sm);
		
		GenerateStimuli gs = new GenerateStimuli();

		final CalendarEvent event2 = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 10, 0), 2);
		final CalendarEvent event2changed = new CalendarEvent("Einkaufen auf dem Wochenmarkt", new GregorianCalendar(2012, 4, 14, 9, 30), 2);
		final CalendarEvent event3 = new CalendarEvent("Augenarzt", new GregorianCalendar(2012, 4, 14, 10, 30), 1);
// uncomment the following line for STIMULUS PAIR 1
		//gm.nlg.setUtteranceObject(new EventConflict(event2, event3));
// uncomment the following for STIMULUS PAIR 2
		//gm.nlg.setUtteranceObject(new MovedEvent(event2, event2changed));

		final CalendarEvent event4 = new CalendarEvent("Austellungseröffnung", new GregorianCalendar(2012, 5, 20, 11, 00), 2);
		final CalendarEvent event4followup = new CalendarEvent("Sekt und Kringel", new GregorianCalendar(2012, 5, 20, 13, 00), 1);
// uncomment the following line for STIMULUS PAIR 3
		//gm.nlg.setUtteranceObject(gs.createUpcomingEvents(event4, event4followup));

		final CalendarEvent event5 = new CalendarEvent("Geschenk besorgen", new GregorianCalendar(2012, 3, 21, 17, 0), 1);
		final CalendarEvent event5followup = new CalendarEvent("Spieleabend bei Hanne", new GregorianCalendar(2012, 3, 22, 15, 0), 3);
// uncomment the following line for STIMULUS PAIR 4
		//gm.nlg.setUtteranceObject(gs.createUpcomingEvents(event5, event5followup));

		final CalendarEvent event6 = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 4, 10, 0), 2);
		final CalendarEvent event6changed = new CalendarEvent("Vorlesung Linguistik", new GregorianCalendar(2012, 3, 6, 12, 0), 2);
// uncomment the following line for STIMULUS PAIR 5
		//gm.nlg.setUtteranceObject(new MovedEvent(event6, event6changed));

		final CalendarEvent event7 = new CalendarEvent("Schwimmen gehen", new GregorianCalendar(2012, 6, 6, 15, 0), 2);
		final CalendarEvent event7conflict = new CalendarEvent("Geburtstag Tante Ilse.", new GregorianCalendar(2012, 6, 6, 15, 0), 4);
// uncomment the following line for STIMULUS PAIR 6
		//gm.nlg.setUtteranceObject(new EventConflict(event7, event7conflict));

		final CalendarEvent event8 = new CalendarEvent("Semesterstart", new GregorianCalendar(2012, 3, 4, 8, 0), 12);
		final CalendarEvent event8followup = new CalendarEvent("Westend Party", new GregorianCalendar(2012, 3, 6, 21, 0), 3);
// uncomment the following line for STIMULUS PAIR 7
		//gm.nlg.setUtteranceObject(gs.createUpcomingEvents(event8, event8followup));

		final CalendarEvent event9 = new CalendarEvent("Zug nach München", new GregorianCalendar(2012, 10, 8, 13, 37), 5);
		final CalendarEvent event9followup = new CalendarEvent("Tagungsbeginn", new GregorianCalendar(2012, 10, 9, 9, 00), 1);
// uncomment the following line for STIMULUS PAIR 8
		//gm.nlg.setUtteranceObject(gs.createUpcomingEvents(event9, event9followup));
		
		final CalendarEvent event10 = new CalendarEvent("Besprechung mit Betreuer", new GregorianCalendar(2012, 6, 27, 14,0), 1);
		final CalendarEvent event10conflict = new CalendarEvent("Mensaführung", new GregorianCalendar(2012, 6, 27, 13, 00), 3);
// uncomment the following line for STIMULUS PAIR 9
		gm.nlg.setUtteranceObject(new EventConflict(event10, event10conflict));
		
		gm.generate();
		NoiseThread nt = new NoiseThread(AdaptionManager.getInstance(), sm, gm.phraseUpdateListener);
		nt.start();
	}
	
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) { }

}
