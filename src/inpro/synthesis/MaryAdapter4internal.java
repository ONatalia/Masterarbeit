package inpro.synthesis;
/**
 * (C) Timo Baumann, 2010, 2011, portions copyright 2000-2006 DFKI GmbH.
 * released under the terms of the GNU LGPL version >= 3
 */

import inpro.incremental.unit.IU;
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
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.server.MaryProperties;
import marytts.server.Request;
import marytts.util.MaryUtils;

public class MaryAdapter4internal extends MaryAdapter {

	public static final String DEFAULT_VOICE = System.getProperty("mary.voice", "bits1-hsmm");
	public static final boolean MARY_AUTOLOAD_JARS = System.getProperty("mary.autoload.jars", "true").equals("true");
	
    private static Logger logger = Logger.getLogger(MaryAdapter4internal.class);
    
	/** private constructor, this class is a singleton 
	 * @throws Exception */
	MaryAdapter4internal() throws Exception {
		startupInternalMary();
	}
	
	// startup-code mostly copied from marytts.server.Mary
	private static void startupInternalMary() throws Exception {
        MaryProperties.readProperties();
        Mary.startup(MARY_AUTOLOAD_JARS);
	}
	
	@Override
	public synchronized List<IU> text2IUs(String tts) throws JAXBException {
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class); 
	    ihtse.synthesizeAudio = false;
		InputStream is = text2maryxml(tts);
	    ihtse.synthesizeAudio = true;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<IU> groundedIn = (List) TTSUtil.wordIUsFromMaryXML(is, ihtse.uttHMMs);
		// remove utterance final silences
		return groundedIn;
	}

	public static PHTSParameterGeneration getNewParamGen() {
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		Voice voice = Voice.getVoice(defaultVoiceName);
		assert (voice instanceof HMMVoice);
        return new PHTSParameterGeneration(((HMMVoice) voice).getHMMData());
	}

	@Override
	protected ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String voiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
        MaryDataType mInputType = MaryDataType.get(inputType);
        MaryDataType mOutputType = MaryDataType.get(outputType);
        Locale mLocale = MaryUtils.string2locale(System.getProperty("inpro.tts.language", "de"));
        Voice voice = Voice.getVoice(voiceName);
        assert voice != null : "Cannot find the Mary voice " + voiceName;
        AudioFormat audioFormat = voice.dbAudioFormat();
        logger.debug("audioFormat is " + audioFormat);
        AudioFileFormat.Type audioFileFormatType = //MaryAudioUtils.getAudioFileFormatType(audioType);
        						AudioFileFormat.Type.WAVE;
        logger.debug("audioFileFormatType is " + audioFileFormatType);
        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
        logger.debug("audioFileFormat is " + audioFileFormat);
        Request request = new Request(mInputType, mOutputType, mLocale, voice, 
        							  (String) null, (String) null, 
        							  // the following true ↓ is experimental (switches on streaming) which doesn't do anything it appears
        							  1, audioFileFormat, true, (String) null);
        try {
	        request.setInputData(query);
	        request.process();
	        request.writeOutputData(baos);
        } catch (Exception e) {
        	//e.printStackTrace();
        	//throw new RuntimeException(e);
        }
        return baos;
	}
	
}
