package org.cocolab.inpro.apps.util;

import java.net.MalformedURLException;
import java.net.URL;

public class RTPCommandLineParser extends CommonCommandLineParser {

	String destinationAddress = "";
	int destPort;
	int localPort;
	
	public RTPCommandLineParser(String[] args) {
		super(args);
	}
	
	void printUsage() {
		System.err.println("simple RTP tool for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleRTP");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <configURL> sphinx configuration file to use (has a reasonable default)");
		System.err.println("    -v             more verbose output");
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
		System.err.println("    -D             create an OAA dispatcher and send its output");
		System.err.println("output selection:");
		System.err.println("    -ip address    destination IP for RTP stream");
		System.err.println("    -p port        port on destination IP");
		System.err.println("    -lp port       optional local port (default: 41000)");
	}
	
	boolean checkConfiguration() {
		boolean success = false;
		if (localPort == 0) { localPort = 42000; } // set default in case nothing else was set
		if (("".equals(destinationAddress)) || (destPort == 0)) {
			printUsage();
			System.err.println("parameters for IP and port are mandatory!");
		}
		else if (inputMode == UNSPECIFIED_INPUT) {
			printUsage();
			System.err.println("Must specify one of -M, -F, or -D");
		} 
		else {
			success = true;
		}
		return success;
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
			else if (args[i].equals("-D")) {
				inputMode = OAA_DISPATCHER_INPUT;
			}
			else if (args[i].equals("-ip")) {
				i++;
				destinationAddress = args[i];
			}
			else if (args[i].equals("-p")) {
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
	
	public int getLocalPort() {
		return localPort;
	}
	
	public int getDestinationPort() {
		return destPort;
	}
	
	public String getDestinationAddress() {
		return destinationAddress;
	}
	
}
