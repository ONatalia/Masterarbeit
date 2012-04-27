package org.cocolab.inpro.incremental.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import marytts.htsengine.HTSModel;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SysInstallmentIU;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.synthesis.MaryAdapter;
import org.cocolab.inpro.synthesis.PitchMark;

public class TTSUtil {
	
	public static List<WordIU> wordIUsFromMaryXML(InputStream is, List<HTSModel> hmms) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Paragraph.class);
		JAXBResult result = new JAXBResult(context);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t;
		is.mark(Integer.MAX_VALUE);
		try {
			t = tf.newTransformer(new StreamSource(TTSUtil.class.getResourceAsStream("mary2simple.xsl")));
			t.transform(new StreamSource(is), result);
		} catch (TransformerException te) {
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
		Paragraph paragraph = (Paragraph) result.getResult(); //unmarshaller.unmarshal(is);
		return paragraph.getWordIUs(hmms.iterator());
	}
	
	@XmlRootElement(name = "s")
	private static class Paragraph {
		@XmlElement(name = "t")
		private List<Word> words;

		@Override
		public String toString() {
			return words.toString();
		}
		
		@SuppressWarnings("unused")
		public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			List<Word> newWords = new ArrayList<Word>(words.size());
			for (Word word : words) {
				if (!word.isEmpty())
					newWords.add(word);
			}
			words = newWords;
		}
		
		public List<WordIU> getWordIUs(Iterator<HTSModel> hmmIterator) {
			List<WordIU> wordIUs = new ArrayList<WordIU>(words.size());
			WordIU prev = null;
			for (Word word : words) {
				WordIU wordIU = word.toIU(hmmIterator);
				wordIU.connectSLL(prev);
				wordIUs.add(wordIU);
				prev = wordIU;
			}
			return wordIUs;
		}
	}
	
	@XmlRootElement(name = "t")
	private static class Word {
		@XmlMixed
		private List<String> tokenList;
		private transient String token;
		@XmlElementWrapper(name = "syllable")
		@XmlElement(name = "ph")
		private List<Segment> segments;
		
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
			return segments == null || segments.isEmpty();
		}
		
		@Override
		public String toString() {
			return "; " + token + "\n" + ((segments != null) ? segments.toString() : "");
		}
		
		public WordIU toIU(Iterator<HTSModel> hmmIterator) {
			List<IU> segmentIUs = new ArrayList<IU>(segments.size());
			IU prev = null;
			for (Segment s : segments) {
				IU sIU = s.toIU(hmmIterator);
				sIU.setSameLevelLink(prev);
				segmentIUs.add(sIU);
				prev = sIU;
			}
			return new WordIU(token, null, segmentIUs);
		}
	}
	
	@XmlRootElement(name = "ph")
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
			Label l = new Label(endTime - (duration / 1000.0), endTime, sampaLabel);
			SysSegmentIU segIU = new SysSegmentIU(l, pitchMarks);
			if (hmmIterator != null && hmmIterator.hasNext()) {
				HTSModel hmm = hmmIterator.next();
				assert (sampaLabel.equals(hmm.getPhoneName())) : " oups, wrong segment alignment: " + sampaLabel + " != " + hmm.getPhoneName();
				segIU.setHTSModel(hmm);
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
	
	public static void main(String[] args) throws JAXBException, TransformerException {
		MaryAdapter ma = MaryAdapter.getInstance();
		//InputStream is = TTSUtil.class.getResourceAsStream("example.maryxml");
		//InputStream is = ma.text2maryxml("nordwind und sonne");
		String testUtterance = "Nimm bitte das rote Kreuz.";
		InputStream is = ma.text2maryxml(testUtterance);

		JAXBContext context = JAXBContext.newInstance(Paragraph.class);
		JAXBResult result = new JAXBResult(context);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer(new StreamSource(TTSUtil.class.getResourceAsStream("mary2simple.xsl")));
		t.transform(new StreamSource(is), result);
		
		Paragraph paragraph = (Paragraph) result.getResult(); //unmarshaller.unmarshal(is);
		System.err.println(paragraph.toString());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		marshaller.marshal(paragraph, System.out);
		System.out.println((new SysInstallmentIU(testUtterance)).deepToString());
	}

}
