package org.cocolab.inpro.audio;

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
 * our connection to mary; currently, this only supports Mary 3.6
 * 
 * The server host and port can be selected with
 * "server.host" and "server.port", which defaults to localhost:59125.
 * 
 * the mary voice to use can be selected with the system property
 * "de.timobaumann.tts.voice". The default voice is "male"
 * (i.e. we let mary decide what male voice to use)
 */
class MaryAdapter {
	de.dfki.lt.mary.client.MaryClient mc36;
	marytts.client.MaryClient mc40;
	
	enum CompatibilityMode { mary36, mary40 };
	CompatibilityMode compatibilityMode = CompatibilityMode.mary36;
	
	MaryAdapter() {
        String serverHost = System.getProperty("server.host", "localhost");
        int serverPort = Integer.getInteger("server.port", 59125).intValue();
        try {
        	switch (compatibilityMode) {
        	case mary36:
    			mc36 = new de.dfki.lt.mary.client.MaryClient(serverHost, serverPort);
        		break;
        	case mary40:
    			mc40 = new marytts.client.http.MaryHttpClient(
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
	
	ByteArrayOutputStream process(String text, String inputType, String outputType, String audioType, String defaultVoiceName) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		switch (compatibilityMode) {
		case mary36:
			if (inputType.equals("TEXT"))
				inputType = "TEXT_DE";
			mc36.process(text, inputType, outputType, audioType,
					defaultVoiceName, baos);
			return baos;
		case mary40:
			String locale = "de";
			mc40.process(text, inputType, outputType, locale, audioType,
					defaultVoiceName, baos);
			return baos;
		default:
			throw new RuntimeException();
		}
	}
	
	InputStream text2audio(String text) {
	    String inputType = "TEXT";
        String outputType = "AUDIO";
        String audioType = "WAVE";
        String defaultVoiceName = System.getProperty("de.timobaumann.tts.voice", "male");
        ByteArrayInputStream bais = null;
		try {
			ByteArrayOutputStream baos = process(text, inputType, outputType, 
					audioType, defaultVoiceName);
			bais = new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return bais;
	}
	
	InputStream text2mbrola(String text) {
        String inputType = "TEXT";
        String outputType = "MBROLA";
        String audioType = "";
        String defaultVoiceName = System.getProperty("de.timobaumann.tts.voice", "male");
        ByteArrayInputStream bais = null;
		try {
	        ByteArrayOutputStream baos = process(text, inputType, outputType, audioType,
					defaultVoiceName);
			System.err.println(baos.toString());
			bais = new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bais;
	}
	
	ByteArrayOutputStream marySynthese(String mbrola) throws UnknownHostException, IOException {
        String inputType = "MBROLA";
        String outputType = "AUDIO";
        String audioType = "WAVE";
        String defaultVoiceName = System.getProperty("de.timobaumann.tts.voice", "male");
        ByteArrayOutputStream baos = process(mbrola, inputType, outputType, audioType,
			    defaultVoiceName);
		return baos;
	}
	
	AudioInputStream mbrola2audio(String mbrola) {
        AudioInputStream ais = null;
        try {
	        ByteArrayOutputStream baos = marySynthese(mbrola);
	        ais = AudioSystem.getAudioInputStream(
		            new ByteArrayInputStream(baos.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ais;
	}
	
	void mbrola2file(String mbrola, File file) throws UnknownHostException, IOException {
        ByteArrayOutputStream baos = marySynthese(mbrola);
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(baos.toByteArray());
		fos.close();
	}
}