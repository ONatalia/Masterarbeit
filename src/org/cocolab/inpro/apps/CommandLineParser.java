package org.cocolab.inpro.apps;

import java.net.MalformedURLException;
import java.net.URL;

public class CommandLineParser {

	public static final int UNSPECIFIED_MODE = 0;
	public static final int FILE_MODE = 1;
	public static final int MICROPHONE_MODE = 2;
	public static final int RTP_MODE = 3;
	
	int mode;
	URL configURL;
	URL audioURL;
	boolean success;
	
	CommandLineParser(String[] args) {
		success = false;
		try {
			mode = UNSPECIFIED_MODE;
			configURL = CommandLineParser.class.getResource("config.xml");
			audioURL = new URL("file:res/DE_1234.wav");
			parse(args);
			if (mode == UNSPECIFIED_MODE) {
				printUsage();
				System.err.println("Must specify one of -M, -R, or -F");
			} else {
				success = true;
			}
		} catch (Exception e) {
			printUsage();
			e.printStackTrace();
		}
	}
	
	void printUsage() {
		System.err.println("simple sphinx recognizer for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleReco");
		System.err.println("options:");
		System.err.println("    -h			   this screen");
		System.err.println("    -c <configURL> sphinx configuration file to use (has a reasonable default)");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -R             read data from RTP");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
	}
	
	void parse(String[] args) throws MalformedURLException {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-h")) {
				printUsage();
				return;
			}
			else if (args[i].equals("-c")) {
				i++;
				configURL = new URL(args[i]);
			}
			else if (args[i].equals("-M")) { 
				mode = MICROPHONE_MODE;
			}
			else if (args[i].equals("-R")) {
				mode = RTP_MODE;
			}
			else if (args[i].equals("-F")) {
				i++;
				mode = FILE_MODE;
				audioURL = new URL(args[i]);
			}
		}
	}
	
	boolean parsedSuccessfully() {
		return success;
	}
	
	URL getConfigURL() {
		return configURL;
	}
	
	URL getAudioURL() {
		return audioURL;
	}
	
	int getMode() {
		return mode;
	}
	
}
