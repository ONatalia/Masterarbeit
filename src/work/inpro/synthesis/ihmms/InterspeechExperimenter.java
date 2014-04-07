package work.inpro.synthesis.ihmms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.audio.DDS16kAudioInputStream;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.synthesis.MaryAdapter;
import inpro.synthesis.MaryAdapter4internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

public class InterspeechExperimenter {

	DispatchStream ds = SimpleMonitor.setupDispatcher(new MonitorCommandLineParser("-D"));
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		InterspeechExperimenter ie = new InterspeechExperimenter();
		if (args.length > 0) {
			int utts = 0;
			int phrases = 0;
			for (String filename : args) {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String line;
				while ((line = br.readLine()) != null) {
					utts++;
					System.out.println(utts + ": " + line);
					phrases += ie.synthesizeOnce(line);
				}
				br.close();
			}
			System.err.println("total utterances: " + utts);
			System.err.println("total phrases: " + phrases);
		} else
			ie.synthesizeOnce(
				"Der nächste Termin, " +
				"am Montag den 14. Mai, " +
				"10 bis 12 Uhr, " + 
				"Betreff Einkaufen auf dem Wochenmarkt, " + 
				"überschneidet sich mit dem Termin: " +
				"10 Uhr 30 bis 11 Uhr 30: " +
				"Zahnarzt."
			);
	}
	
	static List<PhraseIU> preprocess(String text) {
		return preprocess(text, true);
	}
	
	static List<PhraseIU> preprocess(String text, boolean connectPhrases) {
		MaryAdapter4internal m = (MaryAdapter4internal) MaryAdapter.getInstance();
		return m.text2phraseIUs(text, connectPhrases);
	}
	
	private int synthesizeOnce(String text) {
		List<PhraseIU> phrases = preprocess(text);
		//phrases = constructIncrementalProsody(text, false);
		IUBasedFullPStream pstream = new IUBasedFullPStream(phrases.get(0));
		VocodingAudioStream vas = new VocodingAudioStream(pstream, MaryAdapter4internal.getDefaultHMMData(), true);
		ds.playStream(new DDS16kAudioInputStream(vas));
		ds.waitUntilDone();
		return phrases.size();
	}

	/** 
	 * the algorithm is to do the following:
	 *  - generate list of the full utterances phrases (fullPhrases)
	 *  - incrementally generate list of phrases without right context (returnPhrases)
	 *  - copy over "original "features for each fullPhrases' final word to the corresponding word in returnPhrases
	 */
	static List<PhraseIU> constructIncrementalProsody(String text, boolean copyOverFeatures) {
		List<PhraseIU> fullPhrases = preprocess(text);
		List<PhraseIU> returnPhrases = new ArrayList<PhraseIU>(fullPhrases.size());
		assert fullPhrases.size() > 0;
		IU prev = null;
		for (int end = 1; end <= fullPhrases.size(); end++) {
			String partialText = getStringWithinPhrases(fullPhrases, 0, end);
			System.out.println(partialText);
			List<PhraseIU> partList = preprocess(partialText, false);
			assert partList.size() == end : end;
			PhraseIU partialPhraseIU = partList.get(end - 1);
			PhraseIU fullPhraseIU = fullPhrases.get(end - 1);
			copyOverFeaturesOfTheLastWord(fullPhraseIU, partialPhraseIU, copyOverFeatures);
			partialPhraseIU.connectSLL(prev);
			returnPhrases.add(partialPhraseIU);
			prev = partialPhraseIU;
		}
		return returnPhrases;
	}
	
	private static void copyOverFeaturesOfTheLastWord(PhraseIU fullPhraseIU,
			PhraseIU partialPhraseIU, boolean copyOverFeatures) {
		assert fullPhraseIU.groundedIn().size() == partialPhraseIU.groundedIn().size();
		assert fullPhraseIU.getWords().size() == partialPhraseIU.getWords().size();
		for (int i = 0; i < fullPhraseIU.getWords().size(); i++) {
			assert fullPhraseIU.getWords().get(i).getWord().equals(partialPhraseIU.getWords().get(i).getWord());
		}
		int exchangeIndex = fullPhraseIU.getWords().size() - 1;
		assert fullPhraseIU.getWords().get(exchangeIndex).isSilence() == partialPhraseIU.getWords().get(exchangeIndex).isSilence();
		WordIU fullWordIU = fullPhraseIU.getWords().get(exchangeIndex);
		WordIU partialWordIU = partialPhraseIU.getWords().get(exchangeIndex);
		copyOverFeaturesInWord(fullWordIU, partialWordIU, copyOverFeatures);
		if (fullWordIU.isSilence()) {
			exchangeIndex--;
			fullWordIU = fullPhraseIU.getWords().get(exchangeIndex);
			partialWordIU = partialPhraseIU.getWords().get(exchangeIndex);
			copyOverFeaturesInWord(fullWordIU, partialWordIU, copyOverFeatures);
		}
	}

	private static void copyOverFeaturesInWord(WordIU fullWordIU, WordIU partialWordIU, boolean copyOverFeatures) {
		assert fullWordIU.getSegments().size() == partialWordIU.getSegments().size();
		for (int i = 0; i < fullWordIU.getSegments().size(); i++) {
			SysSegmentIU fullSegIU = (SysSegmentIU) fullWordIU.getSegments().get(i);
			SysSegmentIU partialSegIU = (SysSegmentIU) partialWordIU.getSegments().get(i);
			assert fullSegIU.legacyHTSmodel.getPhoneName().
				equals(partialSegIU.legacyHTSmodel.getPhoneName());
			partialSegIU.legacyHTSmodel = fullSegIU.legacyHTSmodel;
			if (copyOverFeatures) {
				partialSegIU.fv = fullSegIU.fv;
			}
		}
	}
	
	static String getStringWithinPhrases(List<PhraseIU> fullPhrases, int start, int end) {
		String returnString = "";
		for (int i = start; i < end; i++) {
			returnString += fullPhrases.get(i).toPayLoad() + " ";
		}
		return returnString;
	}

}