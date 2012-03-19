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

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;
import org.cocolab.inpro.tts.hts.FullPStream;
import org.cocolab.inpro.tts.hts.HTSFullPStream;
import org.cocolab.inpro.tts.hts.InteractiveHTSEngine;
import org.cocolab.inpro.tts.hts.PHTSParameterGeneration;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HTSParameterGeneration;
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
	
	public FullPStream maryxml2hmmFeatures(List<SysSegmentIU> segments, String maryxml) {
		KeepingOneHTSParameterDataListener pdl = new KeepingOneHTSParameterDataListener();
		InteractiveHTSEngine ihtse = (InteractiveHTSEngine) ModuleRegistry.getModule(InteractiveHTSEngine.class); 
	    ihtse.setParameterDataListener(pdl);	
	    ihtse.setSegmentIUs(segments);
	    ihtse.synthesizeAudio = false;
	    maryxml2audio(maryxml); // after this call, htsData should be correctly set
	    ihtse.synthesizeAudio = true;
	    ihtse.setSegmentIUs(null);
	    return pdl.pstream;
	}
	
	class KeepingOneHTSParameterDataListener implements InteractiveHTSEngine.FullPStreamListener {
		FullPStream pstream = null;
		@Override
		public void newParameterData(MaryData d, FullPStream pstream) {
			this.pstream = pstream;
		}		
	}
	
	@Override
	public InputStream text2maryxml(String text) {
		return getInputStreamFromMary(text, "TEXT", "ACOUSTPARAMS");
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
