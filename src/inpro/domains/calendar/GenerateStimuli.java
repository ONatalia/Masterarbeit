package inpro.domains.calendar;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import scalendar.knowledgeobject.CalendarEvent;
import scalendar.spud.CalendarKnowledgeInterface;
import scalendar.spudmanager.SpudManager;
import scalendar.spudmanager.UtteranceObject;
import scalendar.uttereanceobject.EventConflict;
import scalendar.uttereanceobject.MovedEvent;
import scalendar.uttereanceobject.UpcomingEvents;
import edu.rutgers.nlp.asciispec.grammar.jj.ParseException;

public class GenerateStimuli {
	
	private static Random random = new Random();
	private static int year = 2012;
	
	public static int getRandomHourOfDay(int duration) {
		int hourOfDay = random.nextInt(24 - duration);
		if (hourOfDay < 7) { 
			return 7;
		} else {
			return hourOfDay;
		}
	}
	
	private static int randomIntBetween(int min, int max) {
		assert(min < max);
		min *= -1;
		return random.nextInt(min + max) - min;
	}
	
	public static CalendarEvent createRandomEvent(String name, int duration) {
		int hourOfDay = getRandomHourOfDay(duration);
		return createRandomEvent(name, hourOfDay, duration);
	}
	
	public static CalendarEvent createRandomEvent(String name, int hourOfDay, int duration) {
		int month = random.nextInt(12);
		int day = random.nextInt(30);
		return new CalendarEvent(name, new GregorianCalendar(year, month, day, hourOfDay, 0), duration);
	}

	public static CalendarEvent createEvent(String name, int month, int day, int hourOfDay, int duration) {
		return new CalendarEvent(name, new GregorianCalendar(year, month, day, hourOfDay, 0), duration);
	}
	
	private enum TimeRelation {BEFORE, IN, AFTER}	
	private static boolean overlaps(int eventStart, int eventDuration, int problemStart, int problemDuration) {
		int eventEnd = eventStart + eventDuration;
		int problemEnd = problemStart + problemDuration;
		
		if (problemStart >= problemEnd)
			return false;
		
		TimeRelation start, end;
		if (problemStart < eventStart) { 
			start = TimeRelation.BEFORE;
		} else if (problemStart >= eventStart && problemStart < problemEnd) {
			start = TimeRelation.IN;
		} else {
			return false;
		}
		
		if (problemEnd < eventStart) {
			return false;
		} else if (problemEnd > eventStart && problemEnd <= eventEnd) {
			end = TimeRelation.IN;
		} else {
			end = TimeRelation.AFTER;
		}
		
		if (start == TimeRelation.IN 
				|| end == TimeRelation.IN
				|| (start == TimeRelation.BEFORE && end == TimeRelation.AFTER)) {
			return true;
		}
		return false;
	}
	
	public static EventConflict createEventConflict(CalendarEvent event, String problemName) {
		int eventStart = event.getStartDate().get(Calendar.HOUR_OF_DAY);
		int eventDuration = event.getStartDate().get(Calendar.HOUR_OF_DAY) - event.getEndDate().get(Calendar.HOUR_OF_DAY);
		
		int problemStart = 0;
		int problemDuration = 0;
		while (!overlaps(eventStart, eventDuration, problemStart, problemDuration)) {
			problemStart = randomIntBetween(eventStart - 2, eventStart+eventDuration + 2);
			problemDuration = randomIntBetween(1, 5);
		}
			
		CalendarEvent problem = new CalendarEvent(
				problemName,
				new GregorianCalendar(
					year,
					event.getStartDate().get(Calendar.MONTH),
					event.getStartDate().get(Calendar.DAY_OF_MONTH),
					problemStart,
					0),
				problemDuration);
		
		EventConflict ec = new EventConflict(event, problem);
		return ec;
	}
	
	public static MovedEvent createMovedEvent(CalendarEvent event) {
		int duration = event.getStartDate().get(Calendar.HOUR_OF_DAY) - event.getEndDate().get(Calendar.HOUR_OF_DAY);
		int newday = event.getStartDate().get(Calendar.DAY_OF_MONTH) + randomIntBetween(-3, 3);
		if (newday > 29) { 
			newday = 29;
		} else if (newday < 1) {
			newday = 1;
		}

		CalendarEvent moveTo = new CalendarEvent(
				event.getName(),
				new GregorianCalendar(year, 
						event.getStartDate().get(Calendar.MONTH), // same month
						newday,
						getRandomHourOfDay(duration),
						0),
				2);
		MovedEvent me = new MovedEvent(event, moveTo);
		return me;
	}
	
	public static UpcomingEvents createUpcomingEvents(CalendarEvent event1, CalendarEvent event2) {
		ArrayList<CalendarEvent> l = new ArrayList<CalendarEvent>();
		l.add(event1);
		l.add(event2);
		return new UpcomingEvents(l);
	}

	public static void main(String[] args)  throws FileNotFoundException, ParseException {
		SpudManager spudmanager = new SpudManager(
				new CalendarKnowledgeInterface(),
				"src/inpro/domains/calendar/calendar.gs");
		//UtteranceObject uo = GenerateStimuli.createMovedEvent(gs.createRandomEvent("Austellung: China: Stadt, Land, Fluss", 2));
		//UtteranceObject uo = GenerateStimuli.createEventConflict(gs.createRandomEvent("Austellung: China: Stadt, Land, Fluss", 2), "Spazierengehen");
		UtteranceObject uo = GenerateStimuli.createUpcomingEvents(
				GenerateStimuli.createRandomEvent("Austellung: China: Stadt, Land, Fluss", 2),
				GenerateStimuli.createRandomEvent("Spaziergang nach Hause", 2));
		spudmanager.setUtteranceObject(uo);
		System.out.println(spudmanager.generateCompleteUtteranceNonIncrementally());
	}
	
}
