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
	de.dfki.lt.mary.client.MaryClient mc36;
	marytts.client.MaryClient mc41;
	
	private static MaryAdapter maryAdapter = new MaryAdapterImpl();
	
	public static MaryAdapter getInstance() {
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