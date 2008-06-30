package org.cocolab.inpro.apps;

import java.net.MalformedURLException;
import java.net.URL;

public class RTPCommandLineParser {

	public static final int UNSPECIFIED_INPUT = 0;
	public static final int FILE_INPUT = 1;
	public static final int MICROPHONE_INPUT = 2;
	
	URL configURL;
	boolean verbose;
	int inputMode;
	URL audioURL;
	boolean success;
	String destinationAddress;
	int destPort;
	int localPort;
	
	RTPCommandLineParser(String[] args) {
		success = false;
		try {
			verbose = false;
			inputMode = UNSPECIFIED_INPUT;
			configURL = RTPCommandLineParser.class.getResource("config.xml");
			audioURL = new URL("file:res/DE_1234.wav");
			localPort = 41000;
			destPort = -1;
			destinationAddress = "";
			parse(args);
			if ((destinationAddress.equals("")) || (destPort == -1)) {
				printUsage();
				System.err.println("parameters for IP and port are mandatory!");
			}
			else if (inputMode == UNSPECIFIED_INPUT) {
				printUsage();
				System.err.println("Must specify one of -M, or -F");
			} 
			
			else {
				success = true;
			}
		} catch (Exception e) {
			printUsage();
			e.printStackTrace();
		}
	}
	
	void printUsage() {
		System.err.println("simple RTP tool for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleRTP");
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");		
		System.err.println("output selection:");
		System.err.println("    -ip address    destination IPfor RTP stream");
		System.err.println("    -p port        port on destination IP");
		System.err.println("    -lp port       optional local port");
		System.err.println("options:");
		System.err.println("    -h			   this screen");
		System.err.println("    -c <configURL> sphinx configuration file to use (has a reasonable default)");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
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
			else if (args[i].equals("-v")) {
				verbose = true;
			}
			else if (args[i].equals("-M")) { 
				inputMode = MICROPHONE_INPUT;
			}
			else if (args[i].equals("-F")) {
				inputMode = FILE_INPUT;
				i++;
				audioURL = new URL(args[i]);
			}
			else if (args[i].equals("-ip")) {
				i++;
				destinationAddress = args[i];
			}
			else  if (args[i].equals("-p")) {
				i++;
				destPort = new Integer(args[i]).intValue();
				if ((destPort < 0) || (destPort > 65535)) {
					throw new IllegalArgumentException("port must be between 0 and 65535!");
				}
			}
			else if (args[i].equals("-lp")) {
				i++;
				localPort = new Integer(args[i]).intValue();
				if ((localPort < 0) || (localPort > 65535)) {
					throw new IllegalArgumentException("local port must be between 0 and 65535!");
				}
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
	
	boolean verbose() {
		return verbose;
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
	
	int getLocalPort() {
		return localPort;
	}
	
	int getDestinationPort() {
		return destPort;
	}
	
	String getDestinationAddress() {
		return destinationAddress;
	}
	
}
