package org.cocolab.inpro.apps.util;

import java.net.MalformedURLException;
import java.net.URL;

public class RecoCommandLineParser extends CommonCommandLineParser {

	public RecoCommandLineParser(String[] args) {
		super(args);
	}

	public int rtpPort;
	int outputMode;
	
	void printUsage() {
		System.err.println("simple sphinx recognizer for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleReco");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <configURL> sphinx configuration file to use (has a reasonable default)");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -R <port>      read data from RTP");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
		System.err.println("output selection:");
		System.err.println("    -A             send messages via OAA");
		System.err.println("    -T             send incremental hypotheses to TEDview");
		System.err.println("    -L             output incremental label-alignments using LabelWriter to stdout");
	}
	
	/*
	 * check whether the configuration is valid
	 */
	boolean checkConfiguration() {
		boolean success = false;
		if (inputMode == UNSPECIFIED_INPUT) {
			printUsage();
			System.err.println("Must specify one of -M, -R, or -F");
		} else {
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
			else if (args[i].equals("-R")) {
				inputMode = RTP_INPUT;
				i++;
				rtpPort = Integer.parseInt(args[i]);
			}
			else if (args[i].equals("-F")) {
				inputMode = FILE_INPUT;
				i++;
				audioURL = new URL(args[i]);
			}
			else if (args[i].equals("-A")) {
				outputMode |= OAA_OUTPUT;
			}
			else if (args[i].equals("-T")) {
				outputMode |= TED_OUTPUT;
			}
			else if (args[i].equals("-L")) {
				outputMode |= LABEL_OUTPUT;
			}
			else {
				printUsage();
				System.err.println("Illegal argument: " + args[i]);
				System.exit(1);
			}
		}
	}
	
	public boolean matchesOutputMode(int mode) {
		return (outputMode & mode) == mode;
	}
	

}
