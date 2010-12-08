package org.cocolab.inpro.audio;

import java.io.IOException;
import java.net.URL;

import gov.nist.sphere.jaudio.SphereFileReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

//import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;

public class AudioUtils {

	public static AudioInputStream getAudioStreamForURL(URL audioFileURL) throws UnsupportedAudioFileException, IOException {
		AudioInputStream ais;
		String lowerCaseURL = audioFileURL.getFile().toLowerCase();
		if (lowerCaseURL.endsWith(".sph") ||
			lowerCaseURL.endsWith(".nis")) {
			AudioFileReader sfr = new SphereFileReader(); 
			ais = sfr.getAudioInputStream(audioFileURL);
//		} else if (lowerCaseURL.endsWith(".flac")) {
//			ais = null;
//			FlacAudioFileReader fafr = new FlacAudioFileReader();
//			ais = fafr.getAudioInputStream(audioFileURL);
		} else {
//			System.err.println(AudioSystem.getAudioFileFormat(audioFileURL));
	        ais = AudioSystem.getAudioInputStream(audioFileURL);
		}
		return ais;
	}
	
}
