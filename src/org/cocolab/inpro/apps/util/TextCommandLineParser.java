package org.cocolab.inpro.apps.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

public class TextCommandLineParser extends CommonCommandLineParser {

	private Reader textReader;
	
	public TextCommandLineParser(String[] args) {
		super(args);
	}

	@Override
	boolean checkConfiguration() {
		return true;
	}
	
	public boolean hasTextFromReader() {
		return textReader != null;
	}
	
	public Reader getReader() {
		return textReader;
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
			else if (args[i].equals("-T")) {
				StringBuilder sb = new StringBuilder();
				i++;
				while (i < args.length) {
					sb.append(args[i]);
					sb.append(" ");
					i++;
				}
				textReader = new StringReader(sb.toString());
			}
			else if (args[i].equals("-F")) {
				i++;
				try {
					textReader = new InputStreamReader(new URL(args[i]).openStream());
				} catch (IOException e) {
					System.err.println("Could not open URL " + args[i]);
					e.printStackTrace();
					System.exit(1);
				}
			}
			else if (args[i].equals("-STDIN")) {
				textReader = new InputStreamReader(System.in);
			}
		}
	}
	
	@Override
	void printUsage() {
		System.err.println("simple interactive ASR simulator for the inpro project");
		System.err.println("usage: java org.cocolab.inpro.apps.SimpleType");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <URL>       sphinx configuration file to use (reasonable default)");
//		System.err.println("    -v             more verbose output (speed and memory tracker)");
		System.err.println("input selection:");
		System.err.println("    -T \"<text>\"    simulate input of the given text");
		System.err.println("    -F <URL>       read input from file (one line will be committed at a time)");
		System.err.println("    -STDIN         read input from standard in");
	}

}