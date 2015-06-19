package done.inpro.system.calendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;

import org.soa.incremental.nlg.knowledgeobject.CalendarEvent;
import org.soa.incremental.nlg.spudmanager.UtteranceObject;
import org.soa.incremental.nlg.uttereanceobject.UpcomingEvents;

public class EventGeneration {

	private static String[][] activities = {
		{"Vorlesung", "Linguistik"},
		{"Vorlesung", "in Hörsaal sieben"},
		{"Seminar", "zu alter Geschichte"},
		{"Einkaufen", "auf dem Wochenmarkt"},
		{"Einkaufsbummel", "im Ka Dee Weh"},
		{"gemeinsam Essen", "im Westend"},
		{"gemeinsam Essen", "in der Mensa"},
		{"Fotoausstellung", "im Atelier"},
		{"Ausstellungseröffnung", "in der Kunsthalle"},
		{"Essen kochen", "im Stadtpark"},
		{"Schwimmen", "im Hallenbad"}, 
		{"Joggen", "im Stadtpark"},
		{"Nordic-Walking", "im Wald"},
		
		};
	private static String[] names = { // most common first names in Germany in 1973 according to beliebte-vornamen.de
		//"Nicole",
		"Michael", 
		"Sandra", "Markus", 
		"Stefanie", "Thomas",
		"Tanja", "Stefan",
		"Daniela", "Matthias",
		"Claudia", "Andreas",
		"Anja", "Christian",
		"Katrin", "Oliver",
		"Melanie", "Sven",
		"Andrea", "Alexander",
		"Bianca", "Marko", 
		"Katja", "Jan", 
		"Silke", "Martin",
		//"Kerstin",
		"Karsten",
		"Alexandra", "Frank",
		"Sonja", "Torsten" };
	private static Calendar[] dates = { // in addition, magic below will often select "tomorrow" and "day-after-tomorrow"
		new GregorianCalendar(2014, 2, 17),
		new GregorianCalendar(2014, 2, 18),
		new GregorianCalendar(2014, 2, 19),
		//new GregorianCalendar(2014, 3, 17),
		new GregorianCalendar(2014, 1, 15),
		new GregorianCalendar(2014, 4, 17),
		//new GregorianCalendar(2014, 3, 14),
	};
	// always talk about individual events: "Der nächste Termin _date_ um _time_ mit _name_ _locPP_" 
	// with _date_ being "heute", "morgen", or near future
	// 
	
	public static int numStimuli() {
		return activities.length * names.length * dates.length;
	}
	
	public static EventWithRecallQuestion createStimulus(int i) {
		Random r = new Random(i);
		// four types of stimuli: individual event, event changed to other date/time, two consecutive events, two events in conflict
		int activityIndex = r.nextInt(activities.length);
		String activityMain = activities[activityIndex][0];
		String activityDetail = activities[activityIndex][1];
		String name = names[r.nextInt(names.length)];
		int hour = 8 + r.nextInt(11); // how about appointments between 8 and 18 o'clock?
		// minutes: p=0.5 full hour, p=0.25 half hour, p=0.125 for 15 or 45
		int minute = r.nextBoolean() ? 0 : r.nextBoolean() ? 30 : r.nextBoolean() ? 45 : 15;
		
		Calendar date;
		String dateAnswer = "heute";
		// 1/4 tomorrow, 1/4 today, the remainder on one of the days in the list above
		if (r.nextBoolean()) {
			date = Calendar.getInstance();  
			if (r.nextBoolean()) { // day after tomorrow
				date.add(Calendar.DAY_OF_MONTH, 2);
				dateAnswer = "übermorgen";
			} else { // tomorrow
				date.add(Calendar.DAY_OF_MONTH, 1);
				dateAnswer = "morgen";
			}
		} else {
			date = dates[r.nextInt(dates.length)];
			dateAnswer = date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN); 
//			dateAnswer = new SimpleDateFormat("dd.MM.YYYY").format(date);
		}
		date.set(Calendar.HOUR_OF_DAY, hour);
		date.set(Calendar.MINUTE, minute);
		
		String text = activityMain + " " + activityDetail + " mit " + name;
		String question=null, answer=" ?", falseAnswer = null;
		
		//this is a hack... when this is found, it should try something else
		int maxIts = 0;
		while(" ?".equals(answer)) { 
			switch (r.nextInt(7)) {
			case 0: question = "%s?"; answer = activityMain; falseAnswer = findRandomActivity(answer); break;
			case 1: question = "%s?"; answer = activityDetail; falseAnswer = findRandomPlace(answer); break;
			case 2: question = "%s?"; answer = name; falseAnswer = findRandomName(answer); break;
			case 3: question = "%s?"; if(minute > 0) answer = hour + " uhr " + minute; else answer = hour + " uhr"; falseAnswer = findRandomTime(answer); break;
			case 4: question = "%s?"; answer = dateAnswer; falseAnswer = findRandomDay(answer); break;
			case 5: question = "%s?"; answer = dateAnswer; falseAnswer = findRandomDay(answer); break;
			case 6: question = "%s?"; if(minute > 0) answer = hour + " uhr " + minute; else answer = hour + " uhr"; falseAnswer = findRandomTime(answer); break;
			default: question = "%s?"; answer = dateAnswer; falseAnswer = findRandomDay(answer); break;
			}
			maxIts++;
			if (maxIts > 10) break; //another hack to break out if for some crazy reason it is always empty
			
		}
		
		if (r.nextBoolean()) {
			question = String.format(question, answer);
			answer = "true";
		} 
		else {
			question = String.format(question, falseAnswer);
			answer = "false";
		}
		

		UtteranceObject uo = new UpcomingEvents(new CalendarEvent(text, date, 2));
		return new EventWithRecallQuestion(uo, question, answer, dateAnswer + " " + hour + ":" + minute + " " + text);
	}

	private static String findRandomTime(String answer) {
		Random r = new Random();
		String newTime = answer;
		while (newTime.equals(answer)) {
			int hour = 8 + r.nextInt(11); // how about appointments between 8 and 18 o'clock?
			// minutes: p=0.5 full hour, p=0.25 half hour, p=0.125 for 15 or 45
			int minute = r.nextBoolean() ? 0 : r.nextBoolean() ? 30 : r.nextBoolean() ? 45 : 15;
			newTime  = hour + " uhr " + minute;
			if (minute == 0) 
				newTime = hour + " uhr";
		}
		return newTime;
	}

	private static String findRandomActivity(String answer) {
		Random r = new Random();
		String newActivity = answer;
		while (newActivity.equals(answer) || newActivity.equals("") ) {
			int activityIndex = r.nextInt(activities.length);
			newActivity = activities[activityIndex][0]; //index is 0
		}
		return newActivity;
	}

	private static String findRandomPlace(String answer) {
		Random r = new Random();
		String newPlace = answer;
		while (newPlace.equals(answer) || newPlace.equals("") ) {
			int activityIndex = r.nextInt(activities.length);
			newPlace = activities[activityIndex][1]; //index is 1
		}
		return newPlace;
	}

	private static String findRandomDay(String answer) {
		Random r = new Random();
		String newDay = answer;
		Calendar date = null;
		while (newDay.equals(answer)) {
			date = dates[r.nextInt(dates.length)];
			newDay = date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN); 
		}
		return newDay;
	}

	private static String findRandomName(String answer) {
		Random r = new Random();
		String newName = answer;
		while (newName.equals(answer)) 
			newName = names[r.nextInt(names.length)];
		return newName;
	}

	public static class EventWithRecallQuestion {
		public EventWithRecallQuestion(UtteranceObject uo, String question, String answer, String text) {
			this.uo = uo;
			this.question = question;
			this.answer = answer;
			this.text = text;
		}
		public UtteranceObject uo;
		public String question;
		public String answer;
		public String text;
	}

}