package org.cocolab.inpro.apps.util;

import java.net.MalformedURLException;
import java.net.URL;

public class TextCommandLineParser extends CommonCommandLineParser {

	public TextCommandLineParser(String[] args) {
		super(args);
	}

	@Override
	boolean checkConfiguration() {
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
		}
	}

	@Override
	void printUsage() {
		System.err.println("simple ASR simulator for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleType");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <URL>       sphinx configuration file to use (reasonable default)");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
	}

}
