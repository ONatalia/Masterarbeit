package work.inpro.synthesis.ihmms;

import static org.junit.Assert.*;

import java.util.List;

import inpro.incremental.unit.PhraseIU;

import org.junit.Test;

public class InterspeechExperimenterTest {

	String testText = 
			"Der nächste Termin, " +
			"am Montag den 14. Mai, " +
			"zehn bis zwölf Uhr, " + 
			"Betreff Einkaufen auf dem Wochenmarkt, " + 
			"überschneidet sich mit dem Termin: " +
			"zehn Uhr dreißig bis elf Uhr dreißig: " +
			"Zahnarzt. ";

			
	@Test
	public void testStringWithinPhrases() {
		List<PhraseIU> phrases = InterspeechExperimenter.preprocess(testText);
		assertEquals(phrases.size(), 7);
		assertEquals(InterspeechExperimenter.getStringWithinPhrases(phrases, 0, 1), "Der nächste Termin, ");
		assertEquals(InterspeechExperimenter.getStringWithinPhrases(phrases, 0, 2), "Der nächste Termin, am Montag den 14. Mai, ");
		assertEquals(InterspeechExperimenter.getStringWithinPhrases(phrases, 0, 3), "Der nächste Termin, am Montag den 14. Mai, zehn bis zwölf Uhr, ");
		assertEquals(InterspeechExperimenter.getStringWithinPhrases(phrases, 5, 7), "zehn Uhr dreißig bis elf Uhr dreißig: Zahnarzt. ");
		assertEquals(phrases.size(), InterspeechExperimenter.constructIncrementalProsody(testText, true).size());
		assertEquals(phrases.size(), InterspeechExperimenter.constructIncrementalProsody(testText, false).size());
	}

}
