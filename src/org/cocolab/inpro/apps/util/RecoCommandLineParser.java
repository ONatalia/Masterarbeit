package org.cocolab.inpro.apps.util;

import java.net.MalformedURLException;
import java.net.URL;

public class RecoCommandLineParser extends CommonCommandLineParser {

	public static final int REGULAR_RECO = 0;
	public static final int FORCED_ALIGNER_RECO = 1;
	public static final int FAKE_RECO = 2;
	
	public static final int INCREMENTAL = 0;
	public static final int NON_INCREMENTAL = 1;
	public static final int SMOOTHED_INCREMENTAL = 2;
	public static final int FIXEDLAG_INCREMENTAL = 3;
	
	public RecoCommandLineParser(String[] args) {
		super(args);
	}

	int recoMode;
	
	public int rtpPort;
	int outputMode;
	int incrementalMode;
	int incrementalModifier;
	String referenceText;
	String referenceFile;
	
	void printUsage() {
		System.err.println("simple sphinx recognizer for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleReco");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <URL>       sphinx configuration file to use (reasonable default)");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -R <port>      read data from RTP");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
		System.err.println("output selection:");
		System.err.println("    -A             send add/revokeWord messages via OAA");
		System.err.println("    -T             send incremental hypotheses to TEDview");
		System.err.println("    -L             incremental output using LabelWriter");
		System.err.println("    -C             show current incremental ASR hypothesis");
		System.err.println("incrementality options:");
		System.err.println("                   by default, incremental results are generated at every frame");
		System.err.println("    -N             no incremental output");
		System.err.println("    -Is <n>        apply smoothing factor");
		System.err.println("    -If <n>        apply fixed lag");
		System.err.println("    -In            no result smoothing/lagging");
		System.err.println("special modes:");
		System.err.println("    -fa <text>     do forced alignment with the given reference text");
		System.err.println("    -tg <file>     do fake recognition from the given reference textgrid");
	}

	/*
	 * check whether the configuration is valid
	 */
	boolean checkConfiguration() {
		boolean success = false;
		if (inputMode == UNSPECIFIED_INPUT) {
			printUsage();
			System.err.println("Must specify one of -M, -R, or -F");
		} else if ((recoMode == FAKE_RECO) && (inputMode != FILE_INPUT)) {
			printUsage();
			System.err.println("You can only combine faked recognition with file input. Sorry.");
		} else {
			success = true;
		}
		return success;
	}
	
	void parse(String[] args) throws MalformedURLException {
		recoMode = REGULAR_RECO;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-h")) {
				printUsage();
				System.exit(0);
				return;
			}
			else if (args[i].equals("-c")) {
				i++;
				configURL = new URL(args[i]);
			}
			else if (args[i].equals("-v")) {
				verbose = true;
			}
			else if (args[i].equals("-fa")) {
				i++; 
				recoMode = FORCED_ALIGNER_RECO;
				referenceText = args[i];
			}
			else if (args[i].equals("-tg")) {
				i++;
				recoMode = FAKE_RECO;
				referenceText = args[i];
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
			else if (args[i].equals("-C")) {
				outputMode |= CURRHYP_OUTPUT;
			}
			else if (args[i].equals("-N")) {
				incrementalMode = NON_INCREMENTAL;
			}
			else if (args[i].equals("-In")) {
				incrementalMode = INCREMENTAL;
			}
			else if (args[i].equals("-Is")) {
				incrementalMode = SMOOTHED_INCREMENTAL;
				i++;
				incrementalModifier = Integer.parseInt(args[i]);
			}
			else if (args[i].equals("-If")) {
				incrementalMode = FIXEDLAG_INCREMENTAL;
				i++;
				incrementalModifier = Integer.parseInt(args[i]);
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

	public String getReference() {
		return referenceText;
	}
	
	public boolean isRecoMode(int mode) {
		return recoMode == mode;
	}
	
	public boolean isIncremental() {
		return !(incrementalMode == NON_INCREMENTAL);
	}
	
	public int getIncrementalMode() {
		return incrementalMode;
	}

	public int getIncrementalModifier() {
		return incrementalModifier;
	}
	
}
