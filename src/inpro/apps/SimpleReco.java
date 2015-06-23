package inpro.apps;

import inpro.apps.SimpleMonitor;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.apps.util.RecoCommandLineParser;
import inpro.audio.AudioUtils;
import inpro.incremental.PushBuffer;
import inpro.incremental.sink.LabelWriter;
import inpro.sphinx.frontend.DataThrottle;
import inpro.sphinx.frontend.RsbStreamInputSource;
import inpro.incremental.source.GoogleASR;
import inpro.incremental.source.SphinxASR;
import inpro.sphinx.frontend.RtpRecvProcessor;
import inpro.util.TimeUtil;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.util.VUMeterMonitor;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {

	private static final Logger logger = Logger.getLogger(SimpleReco.class);

	private final RecoCommandLineParser clp;
	private final ConfigurationManager cm;
	private final Recognizer recognizer;
	private final GoogleASR gasr;

	public SimpleReco() throws PropertyException, IOException,
			UnsupportedAudioFileException {
		this(new RecoCommandLineParser());
	}

	public SimpleReco(ConfigurationManager cm) throws PropertyException,
			IOException, UnsupportedAudioFileException {
		this(cm, new RecoCommandLineParser());
	}

	public SimpleReco(RecoCommandLineParser clp) throws PropertyException,
			IOException, UnsupportedAudioFileException {
		this(new ConfigurationManager(clp.getConfigURL()), clp);
	}

	public SimpleReco(ConfigurationManager cm, RecoCommandLineParser clp)
			throws IOException, PropertyException,
			UnsupportedAudioFileException {
		this.clp = clp;
		this.cm = cm;
		// setup standard (sphinx-based) speech recognition:
		setupDeltifier();
		setupDecoder();
		logger.info("Setting up source...");
		setupSource();

		this.recognizer = (Recognizer) cm.lookup("recognizer");
		assert recognizer != null;

		logger.info("Setting up monitors...");
		setupMonitors();
		allocateRecognizer();
		// deal with GoogleASR based on the above:
		if (clp.isRecoMode(RecoCommandLineParser.GOOGLE_RECO)) {
			logger.info("Setting up source...");
			BaseDataProcessor realtime = new DataThrottle();
			realtime.setPredecessor(setupFileInput());
			gasr = new GoogleASR(realtime);
			gasr.newProperties(cm.getPropertySheet("googleASR"));
			gasr.setAPIKey(clp.getGoogleAPIkey());
			gasr.setExportFile(clp.getGoogleDumpOutput());
			gasr.setImportFile(clp.getGoogleDumpInput());
			SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
			gasr.iulisteners = casrh.iulisteners;
		} else { gasr = null; }
		logger.info("Configuration has finished");
		TimeUtil.startupTime = System.currentTimeMillis();
	}

	private void setupDeltifier() {
		String ASRfilter = null;
		switch (clp.getIncrementalMode()) {
		case RecoCommandLineParser.FIXEDLAG_INCREMENTAL:
			ASRfilter = "fixedLag";
			break;
		case RecoCommandLineParser.INCREMENTAL:
			ASRfilter = "none";
			break;
		case RecoCommandLineParser.NON_INCREMENTAL:
			ASRfilter = "nonIncr";
			break;
		case RecoCommandLineParser.SMOOTHED_INCREMENTAL:
			ASRfilter = "smoothing";
			break;
		case RecoCommandLineParser.DEFAULT_DELTIFIER:
			break;
		default:
			throw new RuntimeException("something's wrong");
		}
		if (ASRfilter != null) {
			logger.info("Setting ASR filter to " + ASRfilter);
			cm.setGlobalProperty("deltifier", ASRfilter);
			if (!ASRfilter.equals("none")) {
				logger.info("Setting filter parameter to "
						+ clp.getIncrementalModifier());
				cm.setGlobalProperty("deltifierParam",
						Integer.toString(clp.getIncrementalModifier()));
			}
		} else {
			logger.info("Leaving ASR filter at config-file's value.");
		}
	}

	public static void setupMicrophone(final Microphone mic) {
		// create a Thread to start up the microphone (this avoids a problem
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
		Runtime.getRuntime().addShutdownHook(
				new Thread(shutdownHook, "microphone shutdown hook"));
	}

	public static void setupRsbInputSource(final RsbStreamInputSource rsbInput) {
		// create a Thread to start up the microphone (this avoid a problem
		// with microphone initialization hanging and taking forever)
		Thread RsbInputSourceInitializer = new Thread(
				"RsbInputSource initializer") {
			@Override
			public void run() {
				rsbInput.initialize();
			}
		};
		RsbInputSourceInitializer.start();
		try {
			RsbInputSourceInitializer.join(3000); // allow the microphone 3
													// seconds to initialize
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!rsbInput.startRecording()) {
			logger.fatal("Could not open RsbInputSource. Exiting...");
			throw new RuntimeException(
					"Could not open RsbInputSource. Exiting...");
		}
		Runnable shutdownHook = new Runnable() {
			public void run() {
				logger.info("Shutting down RsbInputSource.");
				Thread rsbInputStopper = new Thread("shutdown RsbInputSource") {
					@Override
					public void run() {
						rsbInput.stopRecording();
					}
				};
				rsbInputStopper.start();
				try {
					rsbInputStopper.join(3000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(
				new Thread(shutdownHook, "microphone shutdown hook"));
	}

	VUMeterMonitor vumeter;

	private void setupVuMeter(final DataProcessor mic) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					vumeter = new VUMeterMonitor();
					if (mic != null)
						vumeter.setPredecessor(mic);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		vumeter.getVuMeterDialog().setLocation(690, 100);

	}
	
	private void setupVuMeter() {
		setupVuMeter(null);
	}

	public BaseDataProcessor setupMicrophoneWithEndpointing()  {
    	Microphone mic = (Microphone) cm.lookup("microphone");
		FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
		setupVuMeter();
		vumeter.setPredecessor(mic);
		endpoint.setPredecessor(vumeter);
		endpoint.initialize();
		setupMicrophone(mic);
		return endpoint;
	}

	public void setupRsbInputSourceWithEndpointing() {
		FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		final RsbStreamInputSource rsbInputStream = (RsbStreamInputSource) cm
				.lookup("RsbStreamInputSource");
		FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
		setupVuMeter(rsbInputStream);
		vumeter.setPredecessor(rsbInputStream);
		endpoint.setPredecessor(vumeter);
		vumeter.getVuMeterDialog().setVisible(false);
		endpoint.initialize();
		setupRsbInputSource(rsbInputStream);
		fe.setPredecessor(endpoint);
	}

	public BaseDataProcessor setupFileInput() throws UnsupportedAudioFileException, IOException {
		StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
		sds.initialize();
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
		if (clp.playAtRealtime()) {
			BaseDataProcessor throttle = (BaseDataProcessor) cm.lookup("dataThrottle");
			throttle.initialize();
			BaseDataProcessor feMonitor = (BaseDataProcessor) cm.lookup("feMonitor");
			feMonitor.initialize();
			setupVuMeter();
			BaseDataProcessor endpointing = (BaseDataProcessor) cm.lookup("endpointing");
			endpointing.initialize();
			throttle.setPredecessor(sds);
			vumeter.setPredecessor(throttle);
			feMonitor.setPredecessor(vumeter);
			endpointing.setPredecessor(feMonitor);
			return endpointing;
		} else {
			return sds;
		}
	}

	protected void setupSource() throws PropertyException,
			UnsupportedAudioFileException, IOException {
		FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getInputMode()) {
		case RecoCommandLineParser.MICROPHONE_INPUT:
			fe.setPredecessor(setupMicrophoneWithEndpointing());
			break;
		case RecoCommandLineParser.RTP_INPUT:
			RtpRecvProcessor rtp = (RtpRecvProcessor) cm
					.lookup("RTPDataSource");
			// find component with name RTPDataSource,
			// set the component's property recvPort
			// to the property clp.rtpPort (which is a string)
			cm.getPropertySheet("RTPDataSource")
					.setInt("recvPort", clp.rtpPort);
			rtp.initialize();
			FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
			endpoint.setPredecessor(rtp);
			endpoint.initialize();
			fe.setPredecessor(endpoint);
			break;
		case RecoCommandLineParser.STREAM_INPUT:
			System.out.println("*******RSB Stream Input");
			setupRsbInputSourceWithEndpointing();
			break;
		case RecoCommandLineParser.FILE_INPUT:
			fe.setPredecessor(setupFileInput());
			break;
		}
	}

	private void setupDecoder() throws IOException {
		if (clp.isRecoMode(RecoCommandLineParser.FORCED_ALIGNER_RECO)) {
			logger.info("Running in forced alignment mode.");
			logger.info("Will try to recognize: " + clp.getReference());
			cm.setGlobalProperty("linguist", "flatLinguist");
			cm.setGlobalProperty("grammar", "forcedAligner");
			Linguist linguist = (Linguist) cm.lookup("flatLinguist");
			linguist.allocate();
			AlignerGrammar forcedAligner = (AlignerGrammar) cm
					.lookup("forcedAligner");
			forcedAligner.setText(clp.getReference());
		} else if (clp.isRecoMode(RecoCommandLineParser.FAKE_RECO)) {
			logger.info("Running in fake recognition mode.");
			logger.info("Reading transcript from: " + clp.getReference());
			cm.setGlobalProperty("searchManager", "fakeSearch");
			cm.getPropertySheet("fakeSearch").setString("transcriptName",
					clp.getReference());
		} else if (clp.isRecoMode(RecoCommandLineParser.GRAMMAR_RECO)) {
			URL lmUrl = clp.getLanguageModelURL();
			logger.info("Running with grammar " + lmUrl);
			cm.setGlobalProperty("linguist", "flatLinguist");
			cm.setGlobalProperty("searchManager", "simpleSearch");
			Pattern regexp = Pattern.compile("^(.*)/(.*?).gram$");
			Matcher regexpResult = regexp.matcher(lmUrl.toString());
			if (!regexpResult.matches()) {
				throw new RuntimeException("mal-formatted grammar URL.");
			}
			String grammarLocation = regexpResult.group(1);
			String grammarName = regexpResult.group(2);
			logger.info(grammarLocation);
			cm.getPropertySheet("jsgfGrammar").setString("grammarLocation",
					grammarLocation);
			logger.info(grammarName);
			cm.getPropertySheet("jsgfGrammar").setString("grammarName",
					grammarName);
		} else if (clp.isRecoMode(RecoCommandLineParser.SLM_RECO)) {
			logger.info("Running with language model "
					+ clp.getLanguageModelURL().toString());
			cm.setGlobalProperty("linguist", "lexTreeLinguist");
			String lmLocation = clp.getLanguageModelURL().toString();
			cm.getPropertySheet("ngram").setString("location", lmLocation);
		} else {
			logger.info("Loading recognizer...");
		}
	}

	@SuppressWarnings("unused")
	private void setupMonitors() throws PropertyException {
		ResultListener resultlistener = (ResultListener) cm
				.lookup("currentASRHypothesis");
		recognizer.addResultListener(resultlistener);
		SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
		if (clp.matchesOutputMode(RecoCommandLineParser.TED_OUTPUT)) {
			casrh.addListener((PushBuffer) cm.lookup("tedNotifier"));
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.LABEL_OUTPUT)) {
			cm.getPropertySheet("labelWriter2").setBoolean(LabelWriter.PROP_WRITE_FILE, true);
			cm.getPropertySheet("labelWriter2").setString(LabelWriter.PROP_FILE_NAME, clp.getLabelPath());
			LabelWriter lw = (LabelWriter) cm.lookup("labelWriter2");
			casrh.addListener(lw);
		}
		if (clp.matchesOutputMode(RecoCommandLineParser.CURRHYP_OUTPUT)) {
			casrh.addListener((PushBuffer) cm.lookup("hypViewer"));
		}
		if (clp.verbose()) {
			cm.lookup("memoryTracker");
			cm.lookup("speedTracker");
		}
		// this is a little hacky, but so be it
		if (clp.matchesOutputMode(RecoCommandLineParser.DISPATCHER_OBJECT_OUTPUT)) {
			MonitorCommandLineParser clp = new MonitorCommandLineParser(
					new String[] { "-F", "file:/tmp/monitor.raw", "-S", "-M" // -M is just a placeholder here, it's immediately overridden in the next line:
					});
			clp.setInputMode(MonitorCommandLineParser.DISPATCHER_OBJECT_INPUT);
			try {
				new SimpleMonitor(clp, cm);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	public void allocateRecognizer() {
		if (recognizer != null && recognizer.getState() == Recognizer.State.DEALLOCATED) {
	    	logger.info("Allocating recognizer...");
			recognizer.allocate();
		}
	}
	
	/** call this if you want a single recognition */
	public void recognizeOnce() {
		if (clp.isRecoMode(RecoCommandLineParser.GOOGLE_RECO)) {
			gasr.recognize();
		} else {
			// for Sphinx:
			Result result = null;
			do {
				result = recognizer.recognize();
				if (result != null) {
					// Normal Output
					logger.info("RESULT: " + result.toString());
				} else {
					logger.info("Result: null");
				}
			} while ((result != null) && (result.getDataFrames() != null)
					&& (result.getDataFrames().size() > 4));
		}
	}

	/** call this if you want recognition to loop forever */
	public void recognizeInfinitely() {
		if (clp.isRecoMode(RecoCommandLineParser.GOOGLE_RECO)) {
			throw new RuntimeException("live recognition using Google is not yet implemented. Sorry.");
		}
		while (true) {
			recognizeOnce();
			recognizer.resetMonitors();
		}
	}

	/** call this if you want to implement recognition looping yourself */
	public Recognizer getRecognizer() {
		return recognizer;
	}

	public static void main(String[] args) throws IOException,
			PropertyException, UnsupportedAudioFileException {
		PropertyConfigurator.configure("log4j.properties");
		RecoCommandLineParser clp = new RecoCommandLineParser(args);
		if (!clp.parsedSuccessfully()) {
			throw new IllegalArgumentException(
					"No arguments given or illegal combination of arguments.");
		}
		SimpleReco simpleReco = new SimpleReco(clp);
		if (clp.isInputMode(RecoCommandLineParser.MICROPHONE_INPUT)
				|| clp.isInputMode(RecoCommandLineParser.STREAM_INPUT)) {
			System.err.println("Starting recognition, use Ctrl-C to stop...\n");
			simpleReco.recognizeInfinitely();
		} else {
			simpleReco.recognizeOnce();
		}
		simpleReco.getRecognizer().deallocate();
		System.exit(0);
	}
}
