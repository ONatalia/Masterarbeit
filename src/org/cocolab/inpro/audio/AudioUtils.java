package org.cocolab.inpro.audio;

import java.io.IOException;
import java.net.URL;

import gov.nist.sphere.jaudio.SphereFileReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

public class AudioUtils {

	public static AudioInputStream getAudioStreamForURL(URL audioFileURL) throws UnsupportedAudioFileException, IOException {
		AudioInputStream ais;
		if (audioFileURL.getFile().endsWith(".sph") ||
				audioFileURL.getFile().endsWith(".SPH") ||
				audioFileURL.getFile().endsWith(".nis") ||
				audioFileURL.getFile().endsWith(".NIS")) {
				AudioFileReader sfr = new SphereFileReader(); 
				ais = sfr.getAudioInputStream(audioFileURL);
		} else {
			System.err.println(AudioSystem.getAudioFileFormat(audioFileURL));
	        ais = AudioSystem.getAudioInputStream(audioFileURL);
		}
		return ais;
	}
	
}
