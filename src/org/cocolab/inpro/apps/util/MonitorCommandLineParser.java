package org.cocolab.inpro.apps.util;

import java.net.MalformedURLException;
import java.net.URL;

public class MonitorCommandLineParser extends CommonCommandLineParser {

	final static int SPHINX_DATA = 0;
	final static int RAW_DATA = 1;
	
	int localPort;
	// smaller buffer size leads to lower lag, but more clicking noise
	int bufSize;
	public int outputMode;
	int dataMode;

	public MonitorCommandLineParser(String[] args) {
		super(args);
	}

	void printUsage() {
		System.err.println("simple sphinx RTP monitor for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleMonitor");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -v             more verbose output");
		System.err.println("input selection:");
		System.err.println("    -M             recevice from microphone");
		System.err.println("    -OAA           receive and interpret OAA dispatch messages");
		System.err.println("    -RTP           receive RTP stream");
		System.err.println("    -lp	port       optional port to listen on (default: 42000)");
		System.err.println("    -t sphinx|raw  raw data or encoded Sphinx DoubleData (default: sphinx)");
		System.err.println("output selection:");
		System.err.println("    -S             speakers");
		System.err.println("    -buf size      optional audio buffering size in bytes (default: 8192)");
		System.err.println("    -F <fileURL>   dump file");
	}

	@Override
	boolean checkConfiguration() {
		// setup defaults
		if (localPort == 0) { localPort = 42000; }
		if (bufSize == 0) { bufSize = 8192; }
		if (((outputMode & FILE_OUTPUT) == FILE_OUTPUT) & (!audioURL.getProtocol().equals("file"))) {
			printUsage();
			System.err.println("Can only output to file URLs!");
			return false;
		}
		if (inputMode == UNSPECIFIED_INPUT) {
			printUsage();
			System.err.println("You need to specify the input method");
			return false;
		}
		return true;
	}

	@Override
	void parse(String[] args) throws MalformedURLException {
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
			else if (args[i].equals("-F")) {
				outputMode |= FILE_OUTPUT;
				i++;
				audioURL = new URL(args[i]);
			}
			else if (args[i].equals("-S")) {
				outputMode |= SPEAKER_OUTPUT;
			}
			else if (args[i].equals("-M")) {
				inputMode = MICROPHONE_INPUT;
			}
			else if (args[i].equals("-OAA")) {
				inputMode = OAA_DISPATCHER_INPUT;
			}
			else if (args[i].equals("-RTP")) {
				inputMode = RTP_INPUT;
			}
			else if (args[i].equals("-t")) {
				i++;
				if (args[i].equals("raw")) {
					dataMode = RAW_DATA;
				} else {
					dataMode = SPHINX_DATA;
				}
			}
			else if (args[i].equals("-lp")) {
				i++;
				localPort = new Integer(args[i]).intValue();
				if ((localPort < 0) || (localPort > 65535)) {
					throw new IllegalArgumentException("local port must be between 0 and 65535!");
				}
			}
			else if (args[i].equals("-buf")) {
				i++;
				bufSize = new Integer(args[i]).intValue();
				if ((bufSize < 320) || (bufSize > 65536)) {
					throw new IllegalArgumentException("buffer size must be between 320 and 65536!");
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
	
	public int getBufSize() {
		return bufSize;
	}
	
	public boolean isSphinxMode() {
		return dataMode == SPHINX_DATA;
	}
	
	public boolean matchesOutputMode(int mode) {
		return (outputMode & mode) == mode;
	}
	
	public void setInputMode(int mode) {
		inputMode = mode;
	}
	
	public void setOutputMode(int mode) {
		outputMode = mode;
	}
	
}
