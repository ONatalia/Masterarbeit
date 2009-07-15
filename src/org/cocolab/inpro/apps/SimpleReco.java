package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.apps.util.RecoCommandLineParser;
import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.sphinx.frontend.RtpRecvProcessor;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.language.grammar.ForcedAlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {
	
	protected static void setupMicrophone(final Microphone mic) {
		Runnable micInitializer = new Runnable() {
			public void run() {
				mic.initialize();
			}
		};
		new Thread(micInitializer).start();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // allow the microphone 3 seconds to initialize
		if (!mic.startRecording()) {
			System.err.println("Could not open microphone. Exiting...");
			System.exit(1);
		}	
		Runnable shutdownHook = new Runnable() {
			public void run() {
				System.err.println("Shutting down microphone.");
				mic.stopRecording();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}
	
	protected static void setupSource(ConfigurationManager cm, RecoCommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getInputMode()) {
			case RecoCommandLineParser.MICROPHONE_INPUT:
				final Microphone mic = (Microphone) cm.lookup("microphone");
				FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
				endpoint.setPredecessor(mic);
				endpoint.initialize();
				setupMicrophone(mic);
				fe.setPredecessor(endpoint);
			break;
			case RecoCommandLineParser.RTP_INPUT:
				RtpRecvProcessor rtp = (RtpRecvProcessor) cm.lookup("RTPDataSource");
				// find component with name RTPDataSource, 
				// set the component's property recvPort
				// to the property clp.rtpPort (which is a string)
				cm.getPropertySheet("RTPDataSource").setString("recvPort", "" + clp.rtpPort);
				rtp.initialize();
				endpoint = (FrontEnd) cm.lookup("endpointing");
				endpoint.setPredecessor(rtp);
				endpoint.initialize();
				fe.setPredecessor(endpoint);
			break;
			case RecoCommandLineParser.FILE_INPUT:
				StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
				sds.initialize();
				fe.setPredecessor(sds);
				URL audioURL = clp.getAudioURL();
				System.err.println("input from " + audioURL.toString() + "\n");
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
	            sds.setInputStream(ais, audioURL.getFile());
			break;
		}
	}

	private static void setupMonitors(ConfigurationManager cm, RecoCommandLineParser clp) throws InstantiationException, PropertyException {
		if (clp.matchesOutputMode(RecoCommandLineParser.OAA_OUTPUT)) {
			cm.lookup("newWordNotifierAgent");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.TED_OUTPUT)) {
			cm.lookup("TEDviewNotifier");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.LABEL_OUTPUT)) {
			cm.lookup("labelWriter");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.INCFEATS_OUTPUT)) {
			cm.lookup("incASRConfidenceFeatureWriter");
		}
		if (clp.verbose()) {
			cm.lookup("memoryTracker");
			cm.lookup("speedTracker");
		}
	}

	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
    	RecoCommandLineParser clp = new RecoCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading recognizer...\n");
    	if (clp.forcedAlignment()) { // should be if (forcedAlignment)
    		System.err.println("Running in forced alignment mode.");
    		System.err.println("Will try to recognize: " + clp.getReferenceText());
        	cm.setGlobalProperty("linguist", "flatLinguist");
        	cm.setGlobalProperty("grammar", "forcedAligner");
	    	Linguist linguist = (Linguist) cm.lookup("flatLinguist");
	    	linguist.allocate();
	    	ForcedAlignerGrammar forcedAligner = (ForcedAlignerGrammar) cm.lookup("forcedAligner");
	    	forcedAligner.setText(clp.getReferenceText());
    	}
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
    	System.err.println("Setting up source...\n");
    	setupSource(cm, clp);
    	System.err.println("Setting up monitors...\n");
    	setupMonitors(cm, clp);
    	System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    	Result result = null;
    	do {
	    	result = recognizer.recognize();
	        if (result != null) {
	        	// Normal Output
	            System.err.println("RESULT: " + result.toString() + "\n");
	        } else {
	            System.err.println("Result: null\n");
	        }
	        if (clp.getInputMode() == RecoCommandLineParser.FILE_INPUT) {
	        	break;
	        }
    	} while ((result != null) && (result.getDataFrames() != null) && (result.getDataFrames().size() > 4));
    	recognizer.deallocate();
    }

}
