package org.cocolab.inpro.tts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * our connection to mary; currently, this only supports Mary 3.6,
 * but this will be extended to Mary 4.1
 * 
 * The server host and port can be selected with
 * "server.host" and "server.port", which defaults to localhost:59125.
 * 
 * the mary voice to use can be selected with the system property
 * "org.cocolab.inpro.tts.voice". The default voice is "male"
 * (i.e. we let mary decide what male voice to use)
 * @author timo
 */
public class MaryAdapter {
	de.dfki.lt.mary.client.MaryClient mc36;
	marytts.client.MaryClient mc41;
	
	public static final String DEFAULT_VOICE = "de6";
	
	enum CompatibilityMode { mary36, mary41 };
	CompatibilityMode compatibilityMode = CompatibilityMode.mary41;
	
	private static MaryAdapter maryAdapter = new MaryAdapter();
	
	public static MaryAdapter getMary() {
		return maryAdapter;
	}
	
	private MaryAdapter() {
        String serverHost = System.getProperty("mary.host", "localhost");
        int serverPort = Integer.getInteger("mary.port", 59125).intValue();
        try {
        	switch (compatibilityMode) {
        	case mary36:
    			mc36 = new de.dfki.lt.mary.client.MaryClient(serverHost, serverPort);
        		break;
        	case mary41:
        		mc41 = marytts.client.MaryClient.getMaryClient(
			               new marytts.client.http.Address(serverHost, serverPort)
			           );
        		break;
        	default:
        		throw new RuntimeException();
        	}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		switch (compatibilityMode) {
		case mary36:
			if (inputType.equals("TEXT"))
				inputType = "TEXT_DE";
			mc36.process(query, inputType, outputType, audioType,
					defaultVoiceName, baos);
			return baos;
		case mary41:
			String locale = "de";
			mc41.process(query, inputType, outputType, locale, audioType,
					defaultVoiceName, baos);
			return baos;
		default:
			throw new RuntimeException();
		}
	}
	
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
	
	private InputStream getInputStreamFromMary(String query, String inputType, String outputType) {
        String audioType = "";
        ByteArrayInputStream bais = null;
		try {
	        ByteArrayOutputStream baos = process(query, inputType, outputType, audioType);
			System.err.println(baos.toString());
			bais = new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bais;
	}
	
	public AudioInputStream text2audio(String text) {
        return getAudioInputStreamFromMary(text, "TEXT");
	}
	
	public InputStream text2mbrola(String text) {
		return getInputStreamFromMary(text, "TEXT", "MBROLA");
	}
	
	public InputStream text2maryxml(String text) {
		return getInputStreamFromMary(text, "TEXT", "ACOUSTPARAMS");
	}
	
	public AudioInputStream mbrola2audio(String mbrola) {
		return getAudioInputStreamFromMary(mbrola, "MBROLA");
	}
	
	public void mbrola2file(String mbrola, File file) throws UnknownHostException, IOException {
        ByteArrayOutputStream baos = process(mbrola, "MBROLA", "AUDIO", "WAVE");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(baos.toByteArray());
		fos.close();
	}
}