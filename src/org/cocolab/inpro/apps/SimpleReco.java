package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.cocolab.inpro.apps.util.RecoCommandLineParser;
import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.processor.CurrentASRHypothesis;
import org.cocolab.inpro.sphinx.decoder.FakeSearch;
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
	
	private static final Logger logger = Logger.getLogger(SimpleReco.class);
	
	private static void setupDeltifier(ConfigurationManager cm, RecoCommandLineParser clp) {
//		if (clp.isIncremental()) {
			String ASRfilter;
			switch (clp.getIncrementalMode()) {
				case RecoCommandLineParser.FIXEDLAG_INCREMENTAL : ASRfilter = "fixedLag"; break;
				case RecoCommandLineParser.INCREMENTAL : ASRfilter = "none"; break;
				case RecoCommandLineParser.NON_INCREMENTAL : ASRfilter = "nonIncr"; break;
				case RecoCommandLineParser.SMOOTHED_INCREMENTAL : ASRfilter = "smoothing"; break;
				default : throw new RuntimeException("something's wrong");
			}
			logger.info("Setting ASR filter to " + ASRfilter);
			cm.setGlobalProperty("deltifier", ASRfilter);
			if (!ASRfilter.equals("none")) {
				logger.info("Setting filter parameter to " + clp.getIncrementalModifier());
				cm.setGlobalProperty("deltifierParam", Integer.toString(clp.getIncrementalModifier()));
			}
//		} else {
//			logger.info("Running in NON-INCREMENTAL (pure sphinx) mode");
//		}
	}

	public static void setupMicrophone(final Microphone mic) {
		// create a Thread to start up the microphone (this avoid a problem 
		// with microphone initialization hanging and taking forever)
		Thread micInitializer = new Thread() {
			@Override
			public void run() {
				mic.initialize();
			}
		};
		micInitializer.start();
		try {
			micInitializer.join(3000); // allow the microphone 3 seconds to initialize
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!mic.startRecording()) {
			logger.fatal("Could not open microphone. Exiting...");
			throw new RuntimeException("Could not open microphone. Exiting...");
		}
		Runnable shutdownHook = new Runnable() {
			public void run() {
				logger.info("Shutting down microphone.");
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
				logger.info("input from " + audioURL.toString());
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
				// make sure that audio is in the right format 
				AudioFormat f = ais.getFormat();
				if (f.getChannels() != 1 ||
					!(f.getEncoding().equals(Encoding.PCM_SIGNED) || f.getEncoding().toString().equals("FLAC")) || 
					f.getSampleRate() != 16000 ||
					f.getSampleSizeInBits() != 16) {
					logger.fatal("Your audio is not in the right format:\nYou must use mono channel,\nPCM signed data,\nsampled at 16000 Hz,\nwith 2 bytes per sample.\nExiting...");
					logger.info("channels: " + f.getChannels());
					logger.info("encoding: " + f.getEncoding());
					logger.info("sample rate: " + f.getSampleRate());
					logger.info("sample size: " + f.getSampleSizeInBits());
					System.exit(1);
				}
				sds.setInputStream(ais, audioURL.getFile());
			break;
		}
	}
	
	private static void setupReco(ConfigurationManager cm, RecoCommandLineParser clp) throws IOException {
    	if (clp.isRecoMode(RecoCommandLineParser.FORCED_ALIGNER_RECO)) {
    		logger.info("Running in forced alignment mode.");
    		logger.info("Will try to recognize: " + clp.getReference());
        	cm.setGlobalProperty("linguist", "flatLinguist");
        	cm.setGlobalProperty("grammar", "forcedAligner");
	    	Linguist linguist = (Linguist) cm.lookup("flatLinguist");
	    	linguist.allocate();
	    	ForcedAlignerGrammar forcedAligner = (ForcedAlignerGrammar) cm.lookup("forcedAligner");
	    	forcedAligner.setText(clp.getReference());
    	} else if (clp.isRecoMode(RecoCommandLineParser.FAKE_RECO)) {
    		logger.info("Running in fake recognition mode.");
    		logger.info("Reading transcript from: " + clp.getReference());
        	cm.setGlobalProperty("searchManager", "fakeSearch");
        	FakeSearch fs = (FakeSearch) cm.lookup("fakeSearch");
        	fs.setTranscript(clp.getReference());
    	} else {
    		logger.info("Loading recognizer...");
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
		CurrentASRHypothesis cah = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
		cah.addListener((PushBuffer) cm.lookup("hypViewer"));
//		if (clp.isIncremental()) {
//			if (clp.matchesOutputMode(RecoCommandLineParser.CURRHYP_OUTPUT)) {
//				logger.info("Adding current hypothesis viewer");
//			} 
//		}
		if (clp.verbose()) {
			cm.lookup("memoryTracker");
			cm.lookup("speedTracker");
		}
	}
	
	private static void recognizeOnce(Recognizer recognizer) {
		Result result = null;
    	do {
	    	result = recognizer.recognize();
	        if (result != null) {
	        	// Normal Output
	        	logger.info("RESULT: " + result.toString() + "\n");
	        } else {
	        	logger.info("Result: null\n");
	        }
    	} while ((result != null) && (result.getDataFrames() != null) && (result.getDataFrames().size() > 4));
	}
	
	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
		BasicConfigurator.configure();
		//PropertyConfigurator.configure("log4j.properties");
    	RecoCommandLineParser clp = new RecoCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	setupDeltifier(cm, clp);

    	setupReco(cm, clp);
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
    	logger.info("Setting up source...");
    	setupSource(cm, clp);
    	logger.info("Setting up monitors...");
    	setupMonitors(cm, clp);
    	if (clp.isInputMode(RecoCommandLineParser.MICROPHONE_INPUT)) {
    		System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    		while(true) {
    			logger.debug("in while loop");
	    		recognizeOnce(recognizer);
	    		recognizer.resetMonitors();
    		}
    	} else {
    		recognizeOnce(recognizer);
    	}
    	recognizer.deallocate();
    	System.exit(0);
    }

}
