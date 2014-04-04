package inpro.incremental.util;

import inpro.annotation.Label;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SyllableIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.synthesis.PitchMark;
import inpro.util.TimeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import marytts.htsengine.HTSModel;

/**
 * utility functions to build IU sub-networks from MaryXML
 */
public class TTSUtil {
	
	private static AllContent mary2content(InputStream is) {
		AllContent content;
		try {
			JAXBContext context = JAXBContext.newInstance(AllContent.class);
			JAXBResult result = new JAXBResult(context);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t;
			is.mark(Integer.MAX_VALUE);
			t = tf.newTransformer(new StreamSource(TTSUtil.class.getResourceAsStream("mary2simple.xsl")));
			t.transform(new StreamSource(is), result);
			content = (AllContent) result.getResult(); //unmarshaller.unmarshal(is);
		} catch (Exception te) {
			try {
				is.reset();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot reset stream");
			}
			System.err.print(is);
			te.printStackTrace();
			throw new RuntimeException(te);
		}
		return content;
	}
	
	public static List<WordIU> wordIUsFromMaryXML(InputStream is, List<HTSModel> synthesisPayload) {
		AllContent content = mary2content(is);
		List<WordIU> words =  content.getWordIUs(synthesisPayload != null ? synthesisPayload.iterator() : Collections.<HTSModel>emptyIterator());
		// remove utterance final silences
		ListIterator<WordIU> fromEnd = words.listIterator(words.size());
		while (fromEnd.hasPrevious()) {
			WordIU last = fromEnd.previous();
			if (last.isSilence()) {
				fromEnd.remove();
			} else {
				break;
			}
		}
		fromEnd.next().removeAllNextSameLevelLinks();
		return words;
	}
	
	public static List<PhraseIU> phraseIUsFromMaryXML(InputStream is, List<HTSModel> synthesisPayload, boolean connectPhrases) {
		AllContent content = mary2content(is);
		List<PhraseIU> phrases =  content.getPhraseIUs(synthesisPayload != null ? synthesisPayload.iterator() : Collections.<HTSModel>emptyIterator(), connectPhrases);
		return phrases;
	}
	
	@XmlRootElement(name = "all")
	static class AllContent {
		@XmlElement(name = "phr")
		private List<Phrase> phrases;

		@Override
		public String toString() {
			return phrases.toString();
		}
		
		public List<PhraseIU> getPhraseIUs(Iterator<HTSModel> hmmIterator, boolean connect) {
			List<PhraseIU> phraseIUs = new ArrayList<PhraseIU>(phrases.size());
			IU prev = null;
			for (Phrase phrase : phrases) {
				PhraseIU pIU = phrase.toIU(hmmIterator);
				if (connect) {
					pIU.connectSLL(prev);
				}
				phraseIUs.add(pIU);
				prev = pIU;
			}
			return phraseIUs;
		}
		
		public List<WordIU> getWordIUs(Iterator<HTSModel> hmmIterator) {
			List<PhraseIU> phraseIUs = getPhraseIUs(hmmIterator, true);
			List<WordIU> wordIUs = new ArrayList<WordIU>();
			for (PhraseIU phraseIU : phraseIUs) {
				wordIUs.addAll(phraseIU.getWords());
			}
			return wordIUs;
		}
	}
	
	@XmlRootElement(name = "phr")
	private static class Phrase {
		@XmlMixed
		private List<String> tokenList;
		@XmlAttribute private String tone;
		@XmlAttribute private int breakIndex;
		@XmlAttribute private String pitchOffset;
		private transient double pitchOffsetValue;
		@XmlAttribute private String pitchRange;
		private transient double pitchRangeValue;
		@XmlElement(name = "t")
		private List<Word> words;

		@SuppressWarnings("unused")
		public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			List<Word> newWords = new ArrayList<Word>(words.size());
			// filter out the occasional empty words (which are caused by punctuation) 
			for (Word word : words) {
				if (!word.isEmpty())
					newWords.add(word);
			}

			words = newWords;
		}
		
		String phraseText() {
			String retVal = "";
			for (Word w : words) {
				if (!w.isBreak() && !retVal.equals("")) {
					retVal += " ";
				}
				retVal += w.token;
			}
			return retVal;
		}

		public PhraseIU toIU(Iterator<HTSModel> hmmIterator) {
			List<WordIU> wordIUs = new ArrayList<WordIU>(words.size());
			WordIU prev = null;
			for (Word w : words) {
				WordIU wIU = w.toIU(hmmIterator);
				wIU.connectSLL(prev);
				wordIUs.add(wIU);
				prev = wIU;
			}
			return new PhraseIU(wordIUs, phraseText(), tone, breakIndex);
		}

		@Override
		public String toString() {
			return words.toString();
		}

	}
	
	@XmlRootElement(name = "t")
	private static class Word {
		@XmlMixed
		private List<String> tokenList;
		private transient String token;
		@XmlAttribute private boolean isBreak;
		@XmlAttribute private String pos; 
		@XmlElement(name = "syl")
		private List<Syllable> syllables;
		
		@SuppressWarnings("unused")
		public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			if (tokenList != null) {
				token = joinList(tokenList, " ").toString().replace('\n', ' ').trim();
			} else {
				token = null;
			}
		}
		
		@SuppressWarnings("unused")
		public void beforeMarshal(Marshaller marshaller) {
			if (token != null) {
				tokenList = Arrays.<String>asList(token.split("\n"));
			} else {
				tokenList = null;
			}
		}
		
		public boolean isEmpty() {
			return syllables == null || syllables.isEmpty();
		}
		
		public boolean isBreak() {
			return isBreak;
		}
		
		@Override
		public String toString() {
			return "; " + token + "\n" + ((syllables != null) ? syllables.toString() : "");
		}
		
		public WordIU toIU(Iterator<HTSModel> hmmIterator) {
			List<IU> syllableIUs = new ArrayList<IU>(syllables.size());
			IU prev = null;
			for (Syllable s : syllables) {
				IU sIU = s.toIU(hmmIterator);
				sIU.connectSLL(prev);
				syllableIUs.add(sIU);
				prev = sIU;
			}
			WordIU wiu = new WordIU(token, isBreak(), null, syllableIUs);
			wiu.setUserData("pos", pos);
			return wiu;
		}
	}
	
	@XmlRootElement(name = "syl") 
	private static class Syllable {
		@XmlAttribute private String stress;
		@XmlAttribute private String accent;
		@XmlElement(name = "seg")
		private List<Segment> segments;
		
		@Override
		public String toString() {
			return "stress:" + stress + ", accent:" + accent + "\n" + segments.toString();
		}

		public SyllableIU toIU(Iterator<HTSModel> hmmIterator) {
			List<IU> segmentIUs = new ArrayList<IU>(segments.size());
			IU prev = null;
			for (Segment s : segments) {
				IU sIU = s.toIU(hmmIterator);
				sIU.setSameLevelLink(prev);
				segmentIUs.add(sIU);
				prev = sIU;
			}
			SyllableIU siu = new SyllableIU(null, segmentIUs);
			siu.setUserData("stress", stress);
			siu.setUserData("accent", accent);
			return siu;
		}
	}
	
	@XmlRootElement(name = "seg")
	private static class Segment {
		@XmlAttribute(name = "d")
		private int duration;
		@XmlAttribute(name = "end")
		private double endTime;
		@XmlAttribute(name = "f0")
		private String f0List = null;
		private transient List<PitchMark> pitchMarks;
		@XmlAttribute(name = "p")
		private String sampaLabel;
		
		@SuppressWarnings("unused")
		public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			if (f0List != null && !f0List.equals("")) { 
				List<String> pitchStrings; 
				if (f0List.contains(" ")) { 
					pitchStrings = Arrays.<String>asList(f0List.split(" "));
				} else if (f0List.contains(")(")) {
					pitchStrings = Arrays.<String>asList(f0List.split("\\)\\("));
				} else {
					pitchStrings = Collections.<String>singletonList(f0List);
				}
				pitchMarks = new ArrayList<PitchMark>(pitchStrings.size());
				for (String pitchString : pitchStrings) {
					pitchMarks.add(new PitchMark(pitchString));
				}
			} else {
				pitchMarks = Collections.<PitchMark>emptyList();
			}
		}
		
		@SuppressWarnings("unused")
		public void beforeMarshal(Marshaller marshaller) {
			if (pitchMarks.isEmpty()) {
				f0List = null; 
			} else {
				f0List = joinList(pitchMarks, " ").toString();
			}
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(sampaLabel);
			sb.append(" ");
			sb.append(duration);
			sb.append(" ");
			sb.append(joinList(pitchMarks, " "));
			sb.append("\n");
			return sb.toString();
		}
		
		public SysSegmentIU toIU(Iterator<HTSModel> hmmIterator) {
			Label l = new Label(endTime - (duration / TimeUtil.SECOND_TO_MILLISECOND_FACTOR), endTime, sampaLabel);
			SysSegmentIU segIU;
			assert hmmIterator != null;
			if (hmmIterator.hasNext()) { // the HMM case
				HTSModel hmm = hmmIterator.next();
				assert (sampaLabel.equals(hmm.getPhoneName())) : " oups, wrong segment alignment: " + sampaLabel + " != " + hmm.getPhoneName();
				segIU = new SysSegmentIU(l, pitchMarks, hmm, null);
			} else { // the standard case: no HMM synthesis with this segment
				segIU = new SysSegmentIU(l, pitchMarks);
			}
			return segIU;
		}
	}
	
	public static CharSequence joinList(List<? extends Object> list, CharSequence connector) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<? extends Object> iter = list.iterator(); iter.hasNext();) {
			sb.append(iter.next().toString());
			if (iter.hasNext())
				sb.append(connector);
		}
		return sb;
	}
	
}
