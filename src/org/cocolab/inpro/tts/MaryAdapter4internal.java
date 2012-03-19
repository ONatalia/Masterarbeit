package org.cocolab.inpro.tts;
/**
 * (C) Timo Baumann, 2010, 2011, portions copyright 2000-2006 DFKI GmbH.
 * released under the terms of the GNU LGPL version >= 3
 */

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
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;
import org.cocolab.inpro.incremental.util.TTSUtil;
import org.cocolab.inpro.tts.hts.FullPStream;
import org.cocolab.inpro.tts.hts.InteractiveHTSEngine;
import org.cocolab.inpro.tts.hts.PHTSParameterGeneration;

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
	
    private static Logger logger = Logger.getLogger(MaryAdapter4internal.class);
    
	/** private constructor, this class is a singleton 
	 * @throws Exception */
	MaryAdapter4internal() throws Exception {
		startupInternalMary();
	}
	
	// startup-code mostly copied from marytts.server.Mary
	private static void startupInternalMary() throws Exception {
        MaryProperties.readProperties();
        Mary.startup();
	}
	
	@Override
	public synchronized List<IU> text2IUs(String tts) throws JAXBException {
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class); 
	    ihtse.synthesizeAudio = false;
		InputStream is = text2maryxml(tts);
	    ihtse.synthesizeAudio = true;
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
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
        MaryDataType mInputType = MaryDataType.get(inputType);
        MaryDataType mOutputType = MaryDataType.get(outputType);
        Locale mLocale = MaryUtils.string2locale(System.getProperty("inpro.tts.language", "de"));
        Voice voice = Voice.getVoice(defaultVoiceName);
        AudioFormat audioFormat = voice.dbAudioFormat();
        logger.info("audioFormat is " + audioFormat);
        AudioFileFormat.Type audioFileFormatType = //MaryAudioUtils.getAudioFileFormatType(audioType);
        						AudioFileFormat.Type.WAVE;
        logger.info("audioFileFormatType is " + audioFileFormatType);
        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
        logger.info("audioFileFormat is " + audioFileFormat);
        Request request = new Request(mInputType, mOutputType, mLocale, voice, 
        							  (String) null, (String) null, 
        							  // the following true ↓ is experimental (switches on streaming) which doesn't do anything it appears
        							  1, audioFileFormat, true, (String) null);
        try {
	        request.setInputData(query);
	        request.process();
	        request.writeOutputData(baos);
        } catch (Exception e) {
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
        return baos;
	}
	
}
