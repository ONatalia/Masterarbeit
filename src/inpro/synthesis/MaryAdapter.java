package inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.util.TTSUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.htsengine.HTSModel;

import org.apache.log4j.Logger;

/**
 * our connection to mary; with support for versions 3.6 and 4.1
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

	enum CompatibilityMode { MARY36EXTERNAL, MARY4EXTERNAL, MARY4INTERNAL;	
		public static CompatibilityMode fromString(String mode) {
			if ("3".equals(mode)) return MARY36EXTERNAL;
			if ("4".equals(mode)) return MARY4EXTERNAL; 
			if ("internal".equals(mode)) return MARY4INTERNAL;
			return MARY4INTERNAL;
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
			case MARY36EXTERNAL: 
				maryAdapter = new MaryAdapter36();
				break;
			case MARY4EXTERNAL:
				maryAdapter = new MaryAdapter4();
				break;
			case MARY4INTERNAL:
				try {
					maryAdapter = new MaryAdapter4internal();
				} catch (Exception e) {
					logger.info("could not start MaryAdapter41internal");
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			logger.info("could not start external Mary Adapter");
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
	
	public List<IU> text2IUs(String tts) {
		InputStream is = text2maryxml(tts);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<IU> groundedIn = (List) TTSUtil.wordIUsFromMaryXML(is, Collections.<HTSModel>emptyList());
		return groundedIn;
	}
	
	public AudioInputStream text2audio(String text) {
        return getAudioInputStreamFromMary(text, "TEXT");
	}
	
	public InputStream text2mbrola(String text) {
		return getInputStreamFromMary(text, "TEXT", "MBROLA");
	}
	
	public InputStream text2maryxml(String text) {
		return getInputStreamFromMary(text, "TEXT", "REALISED_ACOUSTPARAMS");
	}
	
	public AudioInputStream mbrola2audio(String mbrola) {
		return getAudioInputStreamFromMary(mbrola, "MBROLA");
	}
	
	public AudioInputStream maryxml2audio(String maryxml) {
		return getAudioInputStreamFromMary(maryxml, "ACOUSTPARAMS");
	}
	
	public void mbrola2file(String mbrola, File file) throws UnknownHostException, IOException {
        ByteArrayOutputStream baos = process(mbrola, "MBROLA", "AUDIO", "WAVE");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(baos.toByteArray());
		fos.close();
	}
}