package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Iterator;
//import java.util.Set;
//import java.util.ArrayList;
//import java.util.HashSet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.apps.util.RecoCommandLineParser;
import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.sphinx.frontend.RtpRecvProcessor;


import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
//import edu.cmu.sphinx.result.Lattice;
//import edu.cmu.sphinx.result.LatticeOptimizer;
//import edu.cmu.sphinx.result.Sausage;
//import edu.cmu.sphinx.result.SausageMaker;
//import edu.cmu.sphinx.result.ConfusionSet;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {

	protected static void setupSource(ConfigurationManager cm, RecoCommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getInputMode()) {
			case RecoCommandLineParser.MICROPHONE_INPUT:
				final Microphone mic = (Microphone) cm.lookup("microphone");
				FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
				endpoint.setPredecessor(mic);
				endpoint.initialize();
				Runnable micInitializer = new Runnable() {
					public void run() {
						mic.initialize();
					}
				};
				new Thread(micInitializer).start();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // allow the microphone 3 seconds to initialize
				if (!mic.startRecording()) {
					System.err.println("Could not open microphone. Exiting...");
					System.exit(1);
				}
				fe.setPredecessor(endpoint);
			break;
			case RecoCommandLineParser.RTP_INPUT:
				RtpRecvProcessor rtp = (RtpRecvProcessor) cm.lookup("RTPDataSource");
				cm.setProperty("RTPDataSource",	"recvPort", "" + clp.rtpPort);
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
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
    	System.err.println("Setting up source...\n");
    	setupSource(cm, clp);
    	System.err.println("Setting up monitors...\n");
    	setupMonitors(cm, clp);
    	System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    	Result result;
    	do {
	    	result = recognizer.recognize();
	        if (result != null) {
	        	// Normal Output
	            System.err.println("RESULT: " + result.toString() + "\n");
				// N-Best Writing...
//	            List list = result.getResultTokens();
//	        	Iterator it = list.iterator();
//	        	while (it.hasNext()) {
//	        		Token t = (Token) it.next();
//		        	System.err.println("RESULT: " + t.getWordPath() + "\n");
//	        	}
				// Uniqued N-Best Writing...
//	            List list = result.getResultTokens();
//	            Set set = new HashSet();
//	        	Iterator it = list.iterator();
//	        	while (it.hasNext()) {
//	        		Token t = (Token) it.next();
//	        		set.add(t.getWordPath());
//	        	}
//	            ArrayList ulist = new ArrayList(set);
//	        	Iterator uit = ulist.iterator();
//	        	while (uit.hasNext()) {
//		        	System.err.println("RESULT: " + uit.next() + "\n");
//	        	}
	        	// Lattice Optimizing...
//	        	Lattice lat = new Lattice(result);
//	        	LatticeOptimizer lo = new LatticeOptimizer(lat);
//	        	lo.optimize();
//	        	List allPaths = lat.allPaths();
//	        	Iterator pathIterator = allPaths.iterator();
//	        	while (pathIterator.hasNext()) {
//		        	System.err.println("Something: " + pathIterator.next() + "\n");
//	        	}
				// Sausage Making...
//	        	SausageMaker sm = new SausageMaker(lat);
//	        	Sausage sau = sm.makeSausage();
//	        	Iterator csi = sau.confusionSetIterator();
//	        	while (csi.hasNext()) {
//	        		ConfusionSet cs = (ConfusionSet) csi.next();
//		        	System.err.println("ConfusionSet: " + cs.toString() + "\n");
//	        	}
//	        	System.err.println("HIER IST SCHLUSS\n");
	        } else {
	            System.err.println("Result: null\n");
	        }
    	} while (result.getDataFrames().length > 4);
    }

}
