package inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.util.TTSUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

/**
 * our connection to mary; with support for Marytts 5.1+ and external
 * Mary servers (use this if you need MBROLA output)
 * 
 * The server host and port can be selected with
 * "mary.host" and "mary.port", which defaults to localhost:59125.
 * 
 * the mary voice to use can be selected with the system property
 * "mary.voice". The default voice is "male"
 * (i.e. we let mary decide what male voice to use)
 * @author timo
 */
public abstract class MaryAdapter {

	enum CompatibilityMode { MARYEXTERNAL, MARY5INTERNAL;	
		public static CompatibilityMode fromString(String mode) {
			if ("external".equals(mode)) return MARYEXTERNAL;
			if ("internal".equals(mode)) return MARY5INTERNAL;
			return MARY5INTERNAL;
		}
	}
	
	public static CompatibilityMode compatibilityMode = CompatibilityMode.fromString(
								System.getProperty("mary.version", "internal"));

    private static Logger logger = Logger.getLogger(MaryAdapter.class);

    private static MaryAdapter maryAdapter;
	
	public static void initializeMary() {
		initializeMary(compatibilityMode);
	}
	
	public static void initializeMary(CompatibilityMode compatibilityMode) {
		logger.info("initializing Mary in compatibility mode " + compatibilityMode);
		maryAdapter = null;
		try {
			switch (compatibilityMode) {
			case MARYEXTERNAL:
				maryAdapter = new MaryAdapterExternal();
				break;
			case MARY5INTERNAL:
				maryAdapter = new MaryAdapter5internal();
				break;
			default:
				throw new RuntimeException("InproTK does not support old versions of MaryTTS anymore.");
			}
		} catch (Exception e) {
			logger.info("could not start MaryAdapter");
			e.printStackTrace();
		}	
	}

	public static MaryAdapter getInstance() {
		if (maryAdapter == null) {
			initializeMary();
		}
		return maryAdapter;
	}
	
	protected abstract ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException;

	private AudioInputStream getAudioInputStreamFromMary(String query, String inputType) {
        String outputType = "AUDIO";
        String audioType = "WAVE";
        AudioInputStream ais = null;
        try {
            ByteArrayOutputStream baos = process(query, inputType, outputType, audioType);
	        ais = AudioSystem.getAudioInputStream(
		            new ByteArrayInputStream(baos.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ais;
	}
	
	protected InputStream getInputStreamFromMary(String query, String inputType, String outputType) {
        String audioType = "";
        ByteArrayInputStream bais = null;
		try {
	        ByteArrayOutputStream baos = process(query, inputType, outputType, audioType);
			bais = new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bais;
	}
	
	public InputStream text2maryxml(String text) {
		return getInputStreamFromMary(text, "TEXT", "REALISED_ACOUSTPARAMS");
	}
	
	public List<IU> text2IUs(String tts) {
		InputStream is = text2maryxml(tts);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<IU> groundedIn = (List) TTSUtil.wordIUsFromMaryXML(is, Collections.<TTSUtil.SynthesisPayload>emptyList());
		return groundedIn;
	}
	
	public List<PhraseIU> text2phraseIUs(String tts) {
		InputStream is = text2maryxml(tts);
		List<PhraseIU> groundedIn = TTSUtil.phraseIUsFromMaryXML(is, Collections.<TTSUtil.SynthesisPayload>emptyList(), true);
		return groundedIn;
	}
	
	public AudioInputStream text2audio(String text) {
        return getAudioInputStreamFromMary(text, "TEXT");
	}
	
	public InputStream text2mbrola(String text) {
		return getInputStreamFromMary(text, "TEXT", "MBROLA");
	}
	
	public AudioInputStream mbrola2audio(String mbrola) {
		return getAudioInputStreamFromMary(mbrola, "MBROLA");
	}
	
}