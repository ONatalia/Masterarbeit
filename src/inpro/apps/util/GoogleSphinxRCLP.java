package inpro.apps.util;

import inpro.apps.util.CommonCommandLineParser;
import inpro.util.PathUtil;

import java.io.File;
import java.net.URL;

public class GoogleSphinxRCLP extends RecoCommandLineParser {

	
	public static final int GOOGLE_SPHINX_RECO = 6;
	
	public GoogleSphinxRCLP(String... args) {
		super(args);
	}
	
	public GoogleSphinxRCLP() {
		super(new String[0]);
	}

	int recoMode;
	
	int incrementalMode;
	String referenceText;
	
	/* stores location of a grammar or SLM */ 
	
	

	public void setReferenceText(String referenceText) {
		this.referenceText = referenceText;
	}

	String googleAPIkey;
	File googleDumpOutput;


	@Override
	void printUsage() {
		System.err.println("simple sphinx recognizer for the inpro project");
		System.err.println("usage: java inpro.apps.GoogleSphinxSimpleReco");
		System.err.println("general options:");
		System.err.println("    -h	           this screen");
		System.err.println("    -v             more verbose output (speed and memory tracker)");
		System.err.println("input selection:");
		System.err.println("    -F <fileURL>   read data from sound file with given URL");
		System.err.println("    -D <directoryURL>   read data from sound files with given directory");
		System.err.println("output selection:");
		System.err.println("    -O             output dialogue system responses");
		System.err.println("    -T             send incremental hypotheses to TEDview");
		System.err.println("    -In            no result smoothing/lagging, DEFAULT");
		System.err.println("    -C             show current incremental ASR hypothesis");
		System.err.println("special recognition modes:");
		System.err.println("    -faG <text>    do forced alignment with for Google incremental result from Google Speech-API with the given API key");
		System.err.println("    -GSph <file>   dump Google JSON results to a file");
		System.err.println();
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
					configURL = PathUtil.anyToURL(args[i]);
				}
				else if (args[i].equals("-v")) {
					verbose = true;
				}
				
				else if (args[i].equals("-F")) {
					inputMode = FILE_INPUT;
					i++;
					audioURL = PathUtil.anyToURL(args[i]);
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
				
				else if (args[i].equals("-In")) {
					incrementalMode = INCREMENTAL;
				}
				
				
				
				else if (args[i].equals("-GSph")) {
					recoMode = GOOGLE_SPHINX_RECO;
					i++;
					googleDumpOutput = new File(args[i]);
				}
				else if (args[i].equals("-faG")) {
					
					
					recoMode = GOOGLE_SPHINX_RECO;
					//referenceText = "das ist meine Aussage";
					referenceText = "eins zwei";
					
					i++;
					googleAPIkey = args[i];
					//inputMode=GOOGLE_INPUT;
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
	
	public int getIncrementalMode() {
		return incrementalMode;
	}

	
	
	
	public String getGoogleAPIkey() {
		return googleAPIkey;
	}
	
	public File getGoogleDumpOutput() {
		return googleDumpOutput;
	}
	
	
}
