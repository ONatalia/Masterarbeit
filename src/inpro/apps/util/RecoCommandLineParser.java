package inpro.apps.util;

import inpro.apps.util.CommonCommandLineParser;

import java.net.URL;

public class RecoCommandLineParser extends CommonCommandLineParser {

	public static final int REGULAR_RECO = 0;
	public static final int FORCED_ALIGNER_RECO = 1;
	public static final int FAKE_RECO = 2;
	public static final int GRAMMAR_RECO = 3;
	public static final int SLM_RECO = 4;
	public static final int GOOGLE_RECO = 5;
	
	public static final int DEFAULT_DELTIFIER = -1;
	public static final int INCREMENTAL = 0;
	public static final int NON_INCREMENTAL = 1;
	public static final int SMOOTHED_INCREMENTAL = 2;
	public static final int FIXEDLAG_INCREMENTAL = 3;
	
	public RecoCommandLineParser(String... args) {
		super(args);
	}
	
	public RecoCommandLineParser() {
		super(new String[0]);
	}

	int recoMode;
	
	public int rtpPort;
	int incrementalMode;
	int incrementalModifier;
	String referenceText;
	
	/* stores location of a grammar or SLM */ 
	URL languageModelURL; 
	
	/**
	 * @return the languageModelURL
	 */
	public URL getLanguageModelURL() {
		return languageModelURL;
	}

	private boolean dataThrottle;
	
	protected boolean ignoreErrors;
	
	@Override
	void printUsage() {
		System.err.println("simple sphinx recognizer for the inpro project");
		System.err.println("usage: java inpro.apps.SimpleReco");
		System.err.println("general options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -c <URL>       sphinx configuration file to use (reasonable default)");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
		System.err.println("    -f             force operation, i.e. try to ignore all errors");
		System.err.println("input selection:");
		System.err.println("    -M             read data from microphone");
		System.err.println("    -S             read data from RsbStreamInput");
		System.err.println("    -R <port>      read data from RTP");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
//		System.err.println("dialogue system options:");
//		System.err.println("    -D configname  use the named dialogue manager (see documentation)");
		System.err.println("output selection:");
		System.err.println("    -O             output dialogue system responses");
		System.err.println("    -T             send incremental hypotheses to TEDview");
		System.err.println("    -L             incremental output using LabelWriter");
		System.err.println("    -Lp	<path>     write the inc_reco file from LabelWriter to this file/path specification, implies -L");
		System.err.println("incrementality options:");
		System.err.println("                   by default, incremental results are generated at every frame");
		System.err.println("    -N             no incremental output");
		System.err.println("    -Is <n>        apply smoothing factor (only for internal Sphinx-ASR)");
		System.err.println("    -If <n>        apply fixed lag (only for internal Sphinx-ASR");
		System.err.println("    -In            no result smoothing/lagging, DEFAULT");
		System.err.println("    -C             show current incremental ASR hypothesis");
		System.err.println("special recognition modes:");
		System.err.println("    -fa <text>     do forced alignment with the given reference text");
		System.err.println("    -tg <file>     do fake recognition from the given reference textgrid");
		System.err.println("    -gr <URL>      recognize using the given JSGF grammar");
		System.err.println("    -lm <URL>      recognize using the given language model");
		System.err.println("                   (-fa, -tg, -gr, and -lm are exclusive and all use the Sphinx-4 engine)");
		System.err.println("    -rt	           when reading from file, run no faster than real-time (includes VAD)");
		System.err.println("    -G <apiKey>    Google Speech-API with the given API key"); //TODO
	}

	/**
	 * check validity of the configuration
	 * additionally warns if configuration makes no sense
	 * @return true for valid combinations of configuration options
	 */
	@Override
	boolean checkConfiguration() {
		boolean success = false;
		// check for necessary requirements
		if (inputMode == UNSPECIFIED_INPUT) {
			printUsage();
			System.err.println("Must specify one of -M, -R, or -F");
		} else if ((recoMode == FAKE_RECO) && (inputMode != FILE_INPUT)) {
			printUsage();
			System.err.println("You can only combine faked recognition with file input. Sorry.");
		} else {
			success = true;
		}
		// test for nonsense option combinations
		if (dataThrottle && (inputMode != FILE_INPUT)) 
			System.err.println("Warning: only throttling speed for file input; your microphone's not faster anyway.");
		return success;
	}
	
	@Override
	void parse(String[] args) throws IllegalArgumentException {
		// set defaults
		recoMode = REGULAR_RECO;
		incrementalMode = DEFAULT_DELTIFIER;
		// look at arguments
		for (int i = 0; i < args.length; i++) {
			try {
				if (args[i].equals("-h")) {
					printUsage();
					System.exit(0);
					return;
				}
				else if (args[i].equals("-c")) {
					i++;
					configURL = anyToURL(args[i]);
				}
				else if (args[i].equals("-v")) {
					verbose = true;
				}
				else if (args[i].equals("-f")) {
					ignoreErrors = true;
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

				else if (args[i].equals("-S")) { 
					inputMode = STREAM_INPUT;
				}
				else if (args[i].equals("-R")) {
					inputMode = RTP_INPUT;
					i++;
					rtpPort = Integer.parseInt(args[i]);
				}
				else if (args[i].equals("-F")) {
					inputMode = FILE_INPUT;
					i++;
					audioURL = anyToURL(args[i]);
				}
				else if (args[i].equals("-T")) {
					outputMode |= TED_OUTPUT;
				}
				else if (args[i].equals("-L")) {
					outputMode |= LABEL_OUTPUT;
				}
				else if (args[i].equals("-Lp")) {
					outputMode |= LABEL_OUTPUT;
					i++;
					setLabelPath(new String(args[i]));
				} 					
				else if (args[i].equals("-C")) {
					outputMode |= CURRHYP_OUTPUT;
				} 
				else if (args[i].equals("-O")) {
					outputMode |= DISPATCHER_OBJECT_OUTPUT;
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
				else if (args[i].equals("-lm")) {
					recoMode = SLM_RECO;
					i++;
					languageModelURL = anyToURL(args[i]);
				} 
				else if (args[i].equals("-gr")) {
					recoMode = GRAMMAR_RECO;
					i++;
					languageModelURL = anyToURL(args[i]);
				} 
				else if (args[i].equals("-rt")) {
					dataThrottle = true;
				}
				else if (args[i].equals("-G")) {
					recoMode = GOOGLE_RECO;
				}
				else {
					throw new IllegalArgumentException(args[i]);
				}
			} catch (Exception e) {
				printUsage();
				if (i < args.length) {
					System.err.println("Illegal argument: " + args[i]);
					//throw new IllegalArgumentException(args[i]);
				} else {
					System.err.println("Something was wrong with the program arguments.");
				}
				System.exit(1);
			}
		}
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
	
	public boolean ignoreErrors() {
		return ignoreErrors;
	}
	
	public int getIncrementalMode() {
		return incrementalMode;
	}

	public int getIncrementalModifier() {
		return incrementalModifier;
	}
	
	public boolean playAtRealtime() {
		return dataThrottle;
	}
}
