package inpro.synthesis;

import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.util.TTSUtil;
import inpro.synthesis.hts.SynthesisPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

/**
 * our connection to mary; with support for Marytts 5.1+ and external
 * Mary servers (use external if you need MBROLA output)
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

	/** get the MaryAdapter singleton */
	public static MaryAdapter getInstance() {
		if (maryAdapter == null) {
			initializeMary();
		}
		return maryAdapter;
	}

	/* * * the actual connection with MaryTTS (needs to be implemented by subclasses) * * */
	
	/** this method needs to be implemented with mary-specific processing in implementing classes */
	protected abstract ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws IOException;

	/* * * two methods to request audio OR textual data from MaryTTS * * */
	
	/** generic method to query Mary for speech audio */
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
			throw new RuntimeException(e);
		}
		return ais;
	}
	
	/** generic method to query Mary for all sorts of output */
	private InputStream getInputStreamFromMary(String query, String inputType, String outputType) {
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

	/* * * methods to turn text (or ACOUSTPARAMS markup) into fully specified MaryXML * * */
	
	/** input: markup that is acceptable to MaryTTS as RAWMARYXML */
	public InputStream text2maryxml(String markup) {
		return getInputStreamFromMary(wrapWithToplevelTag(markup), "RAWMARYXML", "REALISED_ACOUSTPARAMS");
	}
	
	/** input: markup that is acceptable to MaryTTS as ACOUSTPARAMS (i.e., including all durations and f0's) */
	protected InputStream fullySpecifiedMarkup2maryxml(String markup) {
		return getInputStreamFromMary(wrapWithToplevelTag(markup), "ACOUSTPARAMS", "REALISED_ACOUSTPARAMS");
	}
	
	/** surround markup with toplevel maryxml-tag, if this is missing */
	private String wrapWithToplevelTag(String markup) {
		String localeString = System.getProperty("inpro.tts.language", "de");
		Pattern wrapTest = Pattern.compile("<maryxml.*</maryxml>", Pattern.DOTALL);
		Matcher wrapped = wrapTest.matcher(markup);
		if (!wrapped.find()) {
			return  "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
					"<maryxml version=\"0.5\" " +
					"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
					"xmlns=\"http://mary.dfki.de/2002/MaryXML\" " +
					"xml:lang=\"" + localeString + "\">\n" + 
					markup + "\n</maryxml>";
		} else
			return markup;
	}
	
	/* * * methods to turn text into IU structures * * */

	/* * to PhraseIUs * */
	
	/** turn text (including prosodic markup) into phraseIU-structures, phraseIUs are connected via SLLs */
	public List<PhraseIU> text2PhraseIUs(String markup) {
		return text2PhraseIUs(markup, true);
	}
	
	/** turn text (including prosodic markup) into phraseIU-structures */
	@SuppressWarnings("unchecked")
	public List<PhraseIU> text2PhraseIUs(String markup, boolean connectPhrases) {
		return (List<PhraseIU>) text2IUs(markup, true, connectPhrases);
	}
	
	/* * to WordIUs * */

	/** turn text (including prosodic markup) into lists of WordIUs */ 
	@SuppressWarnings("unchecked")
	public List<WordIU> text2WordIUs(String tts) {
		return (List<WordIU>) text2IUs(tts, false, false);
	}
	
	/* * helper for both PhraseIUs and WordIUs * */
	
	/** turn text (including prosodic markup) into lists of WordIUs */ 
	protected List<? extends WordIU> text2IUs(String tts, boolean keepPhrases, boolean connectPhrases) {
		InputStream is = text2maryxml(tts);
		return createIUsFromInputStream(is, Collections.<SynthesisPayload>emptyList(), keepPhrases, connectPhrases);
	}
	
	/* * * method to turn ACOUSTPARAMS data into PhraseIU structures * * */	
	
	@SuppressWarnings("unchecked")
	public List<PhraseIU> fullySpecifiedMarkup2PhraseIUs(String markup) {
		InputStream is = fullySpecifiedMarkup2maryxml(markup);
		return (List<PhraseIU>) createIUsFromInputStream(is, Collections.<SynthesisPayload>emptyList(), true, true);
	}
	
	/* * * helper for creating word/phrase IU structures using TTSUtil * * */
	
	/** actually create the IU structure using TTSUtil */
	protected static List<? extends WordIU> createIUsFromInputStream(InputStream is, List<SynthesisPayload> synload, 
			boolean keepPhrases, boolean connectPhrases) {
		if (keepPhrases) {
			return TTSUtil.phraseIUsFromMaryXML(is, synload, connectPhrases);
		} else {
			return TTSUtil.wordIUsFromMaryXML(is, synload);
		}		
	}

	/* * * legacy functions for direct audio production and for MBROLA * * */
	
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