package inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.util.TTSUtil;
import inpro.synthesis.hts.InteractiveHTSEngine;
import inpro.synthesis.hts.PHTSParameterGeneration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Result;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.util.MaryUtils;

public class MaryAdapter5internal extends MaryAdapter {

	public static final String DEFAULT_VOICE = System.getProperty("mary.voice",
			"bits1-hsmm");
	private static MaryAdapter maryAdapter;
	private MaryInterface maryInterface;
	private String voice;

	private static Logger logger = Logger.getLogger(MaryAdapter5internal.class);

	public MaryAdapter5internal() {
		maryInterface = null;
		try {
			maryInterface = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		Set<String> voices = maryInterface.getAvailableVoices();
		voice = voices.iterator().next();
		maryInterface.setVoice(voice);

	}

	@Override
	protected ByteArrayOutputStream process(String query, String inputType,
			String outputType, String audioType) throws UnknownHostException,
			IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MaryDataType input = MaryDataType.get(inputType);
		MaryDataType output = MaryDataType.get(outputType);

		Locale mLocale = MaryUtils.string2locale(System.getProperty(
				"inpro.tts.language", "de"));
		String voiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		Voice acVoice = Voice.getVoice(voice);
		AudioFormat audioFormat = acVoice.dbAudioFormat();

		logger.debug("audioFormat is " + audioFormat);
		logger.debug("query is " + query);
		assert voice != null : "Cannot find the Mary voice " + voiceName;

		AudioFileFormat.Type audioFileFormatType = AudioFileFormat.Type.WAVE;
		logger.trace("audioFileFormatType is " + audioFileFormatType);
		AudioFileFormat audioFileFormat = new AudioFileFormat(
				audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
		logger.trace("audioFileFormat is " + audioFileFormat);

		Request req = new Request(input, output, mLocale, acVoice, (String) null,
				(String) null, 1, audioFileFormat);

		 try {
		        req.setInputData(query);
		        req.process();
		        req.writeOutputData(baos);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
	        return baos;
	}

	public static MaryAdapter getInstance() {
		if (maryAdapter == null) {
			initializeMary();
		}
		return maryAdapter;
	}

	public static void initializeMary() {
		maryAdapter = new MaryAdapter5internal();

	}

	public InputStream text2maryxml() {
		Document doc = null;
		try {
			doc = maryInterface.generateXML("Test");
		} catch (SynthesisException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Source xmlSource = new DOMSource(doc);
		Result outputTarget = new StreamResult(outputStream);
		try {
			TransformerFactory.newInstance().newTransformer()
					.transform(xmlSource, outputTarget);
		} catch (TransformerException | TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
		return is;
	}

	public Document text2maryxmlDoc() {
		try {
			return maryInterface.generateXML("Test");
		} catch (SynthesisException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public synchronized List<IU> text2IUs(String tts) {
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class);
		ihtse.resetUttHMMstore();
		ihtse.synthesizeAudio = false;
		InputStream is = text2maryxml(tts);
      // useful code for looking at Mary's XML (for debugging): 
/*		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = null;
		try {
			while((line = in.readLine()) != null) {
			  System.err.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ihtse.resetUttHMMstore();
		is = text2maryxml(tts); /**/
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<IU> groundedIn = (List) TTSUtil.wordIUsFromMaryXML(is, ihtse.getUttData());
		return groundedIn;
	}
	@Override
	public synchronized List<PhraseIU> text2phraseIUs(String tts) {
		return text2phraseIUs(tts, true);
	}
	
	public synchronized List<PhraseIU> text2phraseIUs(String tts, boolean connectedPhrases) {
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class);
		ihtse.resetUttHMMstore();
		InputStream is = text2maryxml(tts);
/*		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = null;
		try {
			while((line = in.readLine()) != null) {
			  System.err.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ihtse.resetUttHMMstore();
		is = text2maryxml(tts); /**/
		List<PhraseIU> groundedIn = TTSUtil.phraseIUsFromMaryXML(is, ihtse.getUttData(), connectedPhrases);
		return groundedIn;
	}
	
	public static HMMData getDefaultHMMData() {
		String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		//Voice voice = Voice.getVoice(defaultVoiceName);
		Voice voice = Voice.getVoice("cmu-slt-hsmm");
		assert (voice instanceof HMMVoice);
        return ((HMMVoice) voice).getHMMData();
	}

	public static PHTSParameterGeneration getNewParamGen() {
		return new PHTSParameterGeneration(getDefaultHMMData());
	}

}
