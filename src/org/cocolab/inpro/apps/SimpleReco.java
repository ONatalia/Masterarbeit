package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cocolab.inpro.apps.util.RecoCommandLineParser;
import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.processor.CurrentASRHypothesis;
import org.cocolab.inpro.sphinx.decoder.FakeSearch;
import org.cocolab.inpro.sphinx.frontend.Microphone;
import org.cocolab.inpro.sphinx.frontend.RtpRecvProcessor;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.language.grammar.ForcedAlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {
	
	private static final Logger logger = Logger.getLogger(SimpleReco.class);
	
	private final RecoCommandLineParser clp;
	private final ConfigurationManager cm;
	private final Recognizer recognizer;
	
	public SimpleReco() throws PropertyException, IOException, InstantiationException, UnsupportedAudioFileException {
		this(new RecoCommandLineParser());
	}
	
	public SimpleReco(ConfigurationManager cm) throws PropertyException, IOException, InstantiationException, UnsupportedAudioFileException {
		this(cm, new RecoCommandLineParser());
	}
	
	public SimpleReco(RecoCommandLineParser clp) throws PropertyException, IOException, InstantiationException, UnsupportedAudioFileException {
		this(new ConfigurationManager(clp.getConfigURL()), clp);
	}
	
	public SimpleReco(ConfigurationManager cm, RecoCommandLineParser clp) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
		this.clp = clp;
		this.cm = cm;
    	setupDeltifier();
    	this.recognizer = (Recognizer) cm.lookup("recognizer");
    	assert recognizer != null;

    	setupReco();
    	allocateRecognizer();
    	logger.info("Setting up source...");
    	setupSource();
    	logger.info("Setting up monitors...");
    	setupMonitors();
    	if (clp.isInputMode(RecoCommandLineParser.MICROPHONE_INPUT)) {
    		System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    		while(true) {
    			recognizeOnce();
	    		recognizer.resetMonitors();
    		}
    	} else {
    		recognizeOnce();
    	}
    	recognizer.deallocate();
    	System.exit(0);
	}
	
	private void setupDeltifier() {
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
	}

	public static void setupMicrophone(final Microphone mic) {
		// create a Thread to start up the microphone (this avoid a problem 
		// with microphone initialization hanging and taking forever)
		Thread micInitializer = new Thread("microphone initializer") {
			@Override
			public void run() {
				mic.initialize();
			}
		};
		micInitializer.start();
		try {
			micInitializer.join(3000); // allow the microphone 3 seconds to initialize
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
				Thread micStopper = new Thread("shutdown microphone") {
					@Override
					public void run() {
						mic.stopRecording();
					}
				};
				micStopper.start();
				try {
					micStopper.join(3000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}
	
	public void setupMicrophoneWithEndpointing() {
		allocateRecognizer();
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		final Microphone mic = (Microphone) cm.lookup("microphone");
		FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
		endpoint.setPredecessor(mic);
		endpoint.initialize();
		setupMicrophone(mic);
		fe.setPredecessor(endpoint);
	}
	
	public void allocateRecognizer() {
		if (recognizer.getState() == Recognizer.State.DEALLOCATED) {
	    	logger.info("Allocating recognizer...");
			recognizer.allocate();
		}
	}
	
	protected void setupSource() throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getInputMode()) {
			case RecoCommandLineParser.MICROPHONE_INPUT:
				setupMicrophoneWithEndpointing();
			break;
			case RecoCommandLineParser.RTP_INPUT:
				RtpRecvProcessor rtp = (RtpRecvProcessor) cm.lookup("RTPDataSource");
				// find component with name RTPDataSource, 
				// set the component's property recvPort
				// to the property clp.rtpPort (which is a string)
				cm.getPropertySheet("RTPDataSource").setString("recvPort", "" + clp.rtpPort);
				rtp.initialize();
				FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
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
					Math.abs(f.getSampleRate() - 16000f) > 1f ||
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
	
	private void setupReco() throws IOException {
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

	private void setupMonitors() throws InstantiationException, PropertyException {
		if (clp.matchesOutputMode(RecoCommandLineParser.OAA_OUTPUT)) {
			cm.lookup("newWordNotifierAgent");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.TED_OUTPUT)) {
			cm.lookup("TEDviewNotifier");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.LABEL_OUTPUT)) {
			cm.lookup("labelWriter");
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.CURRHYP_OUTPUT)) {
			CurrentASRHypothesis cah = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
			cah.addListener((PushBuffer) cm.lookup("hypViewer"));
		}
		if (clp.verbose()) {
			cm.lookup("memoryTracker");
			cm.lookup("speedTracker");
		}
	}
	
	private void recognizeOnce() {
		Result result = null;
    	do {
    		if (clp.ignoreErrors()) {
				try {
			    	result = recognizer.recognize();
				} catch (Throwable e) { // also includes AssertionError
					e.printStackTrace();
					logger.warn("Something's wrong further down, trying to continue anyway" , e);
				}
    		} else {
    			result = recognizer.recognize();
    		}
	        if (result != null) {
	        	// Normal Output
	        	logger.info("RESULT: " + result.toString());
	        } else {
	        	logger.info("Result: null");
	        }
    	} while ((result != null) && (result.getDataFrames() != null) && (result.getDataFrames().size() > 4));
	}
	
	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
		PropertyConfigurator.configure("log4j.properties");
    	RecoCommandLineParser clp = new RecoCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	new SimpleReco(clp);
	}

	public Recognizer getRecognizer() {
		return recognizer;
	}
}
