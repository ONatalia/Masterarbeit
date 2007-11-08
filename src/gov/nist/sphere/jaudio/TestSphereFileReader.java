package gov.nist.sphere.jaudio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class TestSphereFileReader {

	/*
	 * Vorgehen: konvertiere SPHERE-Datei nach WAVE und prüfe, 
	 * ob das Ergebnis mit dem von SOX übereinstimmt. 
	 * 
	 * Versuch mit sample_vm.nis:
	 * Verlust von 8192-1024 Audiobytes am Anfang korrigiert, alles OK
	 * 
	 * Versuch mit sample_swbd.sph:
	 * sample-Count laut header:			2018387
	 * laut Byte-Count in .sph:				2018387
	 * laut sample_swbd.wav:				2018388
	 * laut sox.wav:						2018392
	 * 
	 */
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SphereFileReader sfr = new SphereFileReader();
		String inFilename = "tests/nist-sphere-reader/sample_swbd.sph";
		String outFilename = "/tmp/sample_swbd.wav";
		if (args.length == 2) {
			inFilename = args[0];
			outFilename = args[1];
		}
		File inFile = new File(inFilename);
		File outFile = new File(outFilename);
		try {
			AudioInputStream sphereStream = sfr.getAudioInputStream(inFile);
			AudioSystem.write(sphereStream, AudioFileFormat.Type.WAVE, outFile);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
