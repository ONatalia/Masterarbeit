package org.cocolab.inpro.training;

import java.net.MalformedURLException;
import java.net.URL;

class CommandLineOptions {

	URL configURL = DataCollector.class.getResource("config.xml");
	URL slideURL = SlideShowPanel.class.getResource("slides.xml");
	boolean verbose = false;
	
	CommandLineOptions(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-h")) {
				printUsage();
				System.exit(0);
				return;
			}
			else if (args[i].equals("-c")) {
				i++;
				if (i >= args.length) {
					printUsage("-c switch needs a URL parameter!");
				}
				try {
					configURL = new URL(args[i]);
				} catch (MalformedURLException e) {
					printUsage("Cannot interpret URL " + args[i]);
				}
			}
			else if (args[i].equals("-s")) {
				i++;
				if (i >= args.length) {
					printUsage("-s switch needs a URL parameter!");
				}
				try {
					slideURL = new URL(args[i]);
				} catch (MalformedURLException e) {
					printUsage("Cannot interpret URL " + args[i]);
				}
			}
			else if (args[i].equals("-v")) {
				verbose = true;
			}
		}
	}
	
	void printUsage() {
		System.err.println("usage: java org.cocolab.inpro.training.DataCollector");
		System.err.println("options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -v             more verbose output");
		System.err.println("input selection:");
		System.err.println("    -c <URL>       URL to Sphinx configuration");
		System.err.println("    -s <URL>       URL to slide-show configuration");
	}
	
	void printUsage(String message) {
		printUsage();
		System.err.println("Problem interpreting commandline arguments:");
		System.err.println(message);
		System.exit(1);
	}
		
}