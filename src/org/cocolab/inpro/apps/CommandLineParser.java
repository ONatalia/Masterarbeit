package org.cocolab.inpro.apps;

import java.net.MalformedURLException;
import java.net.URL;

public class CommandLineParser {

	public static final int UNSPECIFIED_INPUT = 0;
	public static final int FILE_INPUT = 1;
	public static final int MICROPHONE_INPUT = 2;
	public static final int RTP_INPUT = 3;
	
	public static final int NO_OUTPUT = 0;
	public static final int OAA_OUTPUT = 1;
	
	int inputMode;
	int outputMode;
	URL configURL;
	URL audioURL;
	int rtpPort;
	String rtpSrcIP;
	int rtpSrcPort;
	boolean success;
	
	CommandLineParser(String[] args) {
		success = false;
		try {
			inputMode = UNSPECIFIED_INPUT;
			outputMode = NO_OUTPUT;
			configURL = CommandLineParser.class.getResource("config.xml");
			audioURL = new URL("file:res/DE_1234.wav");
			parse(args);
			if (inputMode == UNSPECIFIED_INPUT) {
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
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -R <port> <srcIP> <srcPort> read data from RTP");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
		System.err.println("output selection:");
		System.err.println("    -A             send messages via OAA");
		
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
				inputMode = MICROPHONE_INPUT;
			}
			else if (args[i].equals("-R")) {
				inputMode = RTP_INPUT;
				i++;
				rtpPort = Integer.parseInt(args[i]);
				i++;
				rtpSrcIP = args[i];
				i++;
				rtpSrcPort = Integer.parseInt(args[i]);
			}
			else if (args[i].equals("-F")) {
				inputMode = FILE_INPUT;
				i++;
				audioURL = new URL(args[i]);
			}
			else if (args[i].equals("-A")) {
				outputMode |= OAA_OUTPUT;
			}
			else {
				printUsage();
				System.err.println("Illegal argument: " + args[i]);
				System.exit(1);
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
	
	int getInputMode() {
		return inputMode;
	}
	
	int getOutputMode() {
		return outputMode;
	}
	
}
