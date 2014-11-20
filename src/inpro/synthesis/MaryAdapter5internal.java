package inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.util.TTSUtil;
import inpro.synthesis.hts.InteractiveHTSEngine;
import inpro.synthesis.hts.PHTSParameterGeneration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.util.MaryUtils;

public class MaryAdapter5internal extends MaryAdapter {

	private static MaryAdapter maryAdapter;
	private MaryInterface maryInterface;

	private static Logger logger = Logger.getLogger(MaryAdapter5internal.class);

	public MaryAdapter5internal() {
		maryInterface = null;
		try {
			maryInterface = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected ByteArrayOutputStream process(String query, String inputType,
			String outputType, String audioType) throws UnknownHostException,
			IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MaryDataType input = MaryDataType.get(inputType);
		MaryDataType output = MaryDataType.get(outputType);
		assert !(inputType.equals("MBROLA") || outputType.equals("MBROLA")) : 
			"There's no MBROLA support in internalized Mary 5, please use an external Mary 4 server";

		Locale mLocale = MaryUtils.string2locale(System.getProperty("inpro.tts.language", "de"));
		String voiceName = System.getProperty("inpro.tts.voice", System.getProperty("inpro.tts.voice", "bits1-hsmm"));
		maryInterface.setVoice(voiceName);
		Voice voice = Voice.getVoice(voiceName);
		AudioFormat audioFormat = voice.dbAudioFormat();

		audioFormat = new AudioFormat(16000, audioFormat.getSampleSizeInBits(), audioFormat.getChannels(), true, audioFormat.isBigEndian());
		assert audioFormat.getSampleRate() == 16000f : "InproTK cannot handle voices with sample rates other than 16000Hz, your's is " + audioFormat.getSampleRate();
		logger.debug("audioFormat is " + audioFormat);
		logger.debug("query is " + query);
		assert voice != null : "Cannot find the Mary voice " + voiceName;

		AudioFileFormat.Type audioFileFormatType = AudioFileFormat.Type.WAVE;
		logger.trace("audioFileFormatType is " + audioFileFormatType);
		AudioFileFormat audioFileFormat = new AudioFileFormat(
				audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
		logger.trace("audioFileFormat is " + audioFileFormat);

		Request req = new Request(input, output, mLocale, voice, (String) null,
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

	@Override
	public synchronized List<IU> text2IUs(String tts) {
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class);
		ihtse.resetUttHMMstore();
		ihtse.synthesizeAudio = false;
		InputStream is = text2maryxml(tts);
      // useful code for looking at Mary's XML (for debugging): 
/*		java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(is));
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
/*		java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(is));
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
		String defaultVoiceName = System.getProperty("inpro.tts.voice", System.getProperty("inpro.tts.voice", "bits1-hsmm"));
		Voice voice = Voice.getVoice(defaultVoiceName);
		assert (voice instanceof HMMVoice);
        return ((HMMVoice) voice).getHMMData();
	}

	public static PHTSParameterGeneration getNewParamGen() {
		return new PHTSParameterGeneration(getDefaultHMMData());
	}

}
