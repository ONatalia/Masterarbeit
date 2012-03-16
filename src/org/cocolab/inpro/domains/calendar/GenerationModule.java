package org.cocolab.inpro.domains.calendar;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedList;
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
			System.out.println("update called");
			if (updatedIU.isCompleted()) { // should be isCompleted()
				
				if (AdaptionManager.getInstance().hasChanged()) {
					phrases.remove(phrases.size() - 1);
					String phrase = nlg.generateNextPhrase();
					if (phrase != null) {
						PhraseIU piu = new PhraseIU(phrase, PhraseIU.PhraseStatus.NORMAL); // add type: continuation or repair?
						piu.addUpdateListener(phraseUpdateListener);
						phrases.add(piu);
					} else {
						return;
					}					
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
		}
	};
		
	/** only called on startup  */
	public void generate() {
		String phrase = nlg.generateNextPhrase();
		System.out.println("   INITIAL: " + phrase);
		PhraseIU piu = new PhraseIU(phrase, PhraseIU.PhraseStatus.NORMAL, PhraseIU.PhraseType.INITIAL);
		phrases.add(piu);
		
		String projectedPhrase = nlg.simulateNextButOnePhrase();
		System.out.println("   INITIAL PROJECTION: " + projectedPhrase);
		PhraseIU ppiu = new PhraseIU(projectedPhrase, PhraseIU.PhraseStatus.PROJECTED);
		phrases.add(ppiu);
		
		rightBuffer.setBuffer(phrases);
		rightBuffer.notify(iulisteners);
		piu.addUpdateListener(phraseUpdateListener);
		ppiu.addUpdateListener(phraseUpdateListener);
		System.out.println("generate is done");
	}
	
	
	
	
	
	public static void main(String[] args) throws FileNotFoundException, ParseException {
		SpudManager spudmanager = new SpudManager(
				new CalendarKnowledgeInterface(),
				"src/org/cocolab/inpro/domains/calendar/calendar.gs");
		GenerationModule gm = new GenerationModule(spudmanager);
		final SynthesisModule sm = new SynthesisModule();

		gm.iulisteners = new ArrayList<PushBuffer>();
		gm.iulisteners.add(sm);
		
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int year = Calendar.getInstance().get(Calendar.YEAR);
		if (day > 25) {
			day = 1; month += 1;
			if (month == 12) {
				month = 0; year += 1;
			}
		}
		final CalendarEvent event0 = new CalendarEvent("besprechung mit stefan",
				new GregorianCalendar(year, month, day, 16, 0),1);
		final CalendarEvent event1 = new CalendarEvent("flug nach iisland",
				new GregorianCalendar(year, month,day, 16, 0), 2);
		final List<CalendarEvent> events=new LinkedList<CalendarEvent>();
		events.add(event0);
		events.add(event1);
		gm.nlg.setUtteranceObject(new EventConflict(event0,event1));
		gm.generate();
	}
	
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) { }

}
