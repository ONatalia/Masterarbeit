package inpro.audio;

import inpro.synthesis.hts.VocodingAudioStream;

import java.io.IOException;
import java.net.URL;

import gov.nist.sphere.jaudio.SphereFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import marytts.util.data.audio.AudioConverterUtils;
import marytts.util.data.audio.DDSAudioInputStream;

import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;

public class AudioUtils {

	public static AudioInputStream getAudioStreamForURL(URL audioFileURL) throws UnsupportedAudioFileException, IOException {
		AudioInputStream ais;
		String lowerCaseURL = audioFileURL.getFile().toLowerCase();
		if (lowerCaseURL.endsWith(".sph") ||
			lowerCaseURL.endsWith(".nis")) {
			AudioFileReader sfr = new SphereFileReader(); 
			ais = sfr.getAudioInputStream(audioFileURL);
		} else if (lowerCaseURL.endsWith(".flac")) {
			ais = null;
			FlacAudioFileReader fafr = new FlacAudioFileReader();
			ais = fafr.getAudioInputStream(audioFileURL);
		} else {
//			System.err.println(AudioSystem.getAudioFileFormat(audioFileURL));
	        ais = AudioSystem.getAudioInputStream(audioFileURL);
		}
		return ais;
	}
	
	public static AudioInputStream get16kAudioStreamForVocodingStream(VocodingAudioStream source) {
		DDSAudioInputStream ddsStream = new DDSAudioInputStream(source, new AudioFormat(source.getSamplingRate(), 16, 1, true, false));
		if (source.getSamplingRate() == 16000) {
			return ddsStream;
		} else {
			try {
				return AudioConverterUtils.downSampling(ddsStream, 16000);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
}
