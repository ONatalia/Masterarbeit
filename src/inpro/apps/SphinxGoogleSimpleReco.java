package inpro.apps;

import inpro.apps.SimpleMonitor;
import inpro.apps.util.GoogleSphinxRCLP;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.apps.util.RecognizerInputStream;
import inpro.audio.AudioUtils;
import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.sink.LabelWriter;
import inpro.sphinx.frontend.DataThrottle;
import inpro.sphinx.frontend.RsbStreamInputSource;
import inpro.incremental.source.GoogleASR;
import inpro.incremental.source.SphinxASR;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.util.TimeUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


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
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.util.VUMeterMonitor;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SphinxGoogleSimpleReco extends IUModule{

	private static final Logger logger = Logger.getLogger(SphinxGoogleSimpleReco.class);

	private final GoogleSphinxRCLP clp;
	private final ConfigurationManager cm;
	private Recognizer recognizer;
	private Recognizer recognizerFA;
	private final GoogleASR gasr;
	private RecognizerInputStream rais;
	private TreeMap <Integer,String> hyps=new TreeMap <Integer,String>();;

	

	
	public SphinxGoogleSimpleReco() throws PropertyException, IOException,
			UnsupportedAudioFileException {
		this(new GoogleSphinxRCLP());
	}

	public SphinxGoogleSimpleReco(ConfigurationManager cm) throws PropertyException,
			IOException, UnsupportedAudioFileException {
		this(cm, new GoogleSphinxRCLP());
	}

	public SphinxGoogleSimpleReco(GoogleSphinxRCLP clp) throws PropertyException,
			IOException, UnsupportedAudioFileException {
		this(new ConfigurationManager(clp.getConfigURL()), clp);
	}

	public SphinxGoogleSimpleReco(ConfigurationManager cm, GoogleSphinxRCLP clp)
			throws IOException, PropertyException,
			UnsupportedAudioFileException {
		this.clp = clp;
		this.cm = cm;
		// setup standard (sphinx-based) speech recognition:
		setupDeltifier();
		//setupDecoder();
		logger.info("Setting up source...");
		setupSource();

		this.recognizer = (Recognizer) cm.lookup("recognizer");
		assert recognizer != null;
		
		this.recognizerFA = (Recognizer) cm.lookup("recognizerFA");
		assert recognizerFA != null;

		logger.info("Setting up monitors...");
		setupMonitors();
		allocateRecognizer();
		allocateRecognizerFA();
		//// deal with GoogleASR based on the above:
		
		
	
		
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			logger.info("Setting up source...");
			
			BaseDataProcessor realtime = new DataThrottle();
			FrontEnd fe=(FrontEnd)cm.lookup("frontend");
			StreamDataSource sdsG = (StreamDataSource) cm.lookup("streamDataSourceGoogle");
			StreamDataSource sdsS = (StreamDataSource) cm.lookup("streamDataSource");
			StreamDataSource sdsFA = (StreamDataSource) cm.lookup("streamDataSourceFA");
			sdsG.initialize();
			sdsS.initialize();
			sdsFA.initialize();
			
			AudioInputStream ais=setupAis(fe);
			rais=new RecognizerInputStream (ais);
	
			AudioInputStream aisS = new AudioInputStream(new ByteArrayInputStream(
			rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
			AudioInputStream aisG = new AudioInputStream(new ByteArrayInputStream(
					rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
			AudioInputStream aisFA = new AudioInputStream(new ByteArrayInputStream(
					rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
		
	        sdsG.setInputStream(aisG, "google");
			sdsS.setInputStream(aisS, "sphinx");
			sdsFA.setInputStream(aisFA,"FA");
			
			realtime.setPredecessor(sdsG);
			fe.setPredecessor(sdsS);
			fe.setPredecessor(sdsFA);
			
			
			
			
			gasr = new GoogleASR(realtime);
			gasr.newProperties(cm.getPropertySheet("googleASR"));
			gasr.setAPIKey(clp.getGoogleAPIkey());
			gasr.setExportFile(clp.getGoogleDumpOutput());
			gasr.setImportFile(clp.getGoogleDumpInput());
			SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
			
			gasr.iulisteners.add(this);
			this.iulisteners=casrh.iulisteners;
			
		}else { gasr = null; }
		
	
		logger.info("Configuration has finished");
		TimeUtil.startupTime = System.currentTimeMillis();
	}

	private void setupDeltifier() {
		String ASRfilter = null;
		switch (clp.getIncrementalMode()) {
		case GoogleSphinxRCLP.FIXEDLAG_INCREMENTAL:
			ASRfilter = "fixedLag";
			break;
		case GoogleSphinxRCLP.INCREMENTAL:
			ASRfilter = "none";
			break;
		case GoogleSphinxRCLP.NON_INCREMENTAL:
			ASRfilter = "nonIncr";
			break;
		case GoogleSphinxRCLP.SMOOTHED_INCREMENTAL:
			ASRfilter = "smoothing";
			break;
		case GoogleSphinxRCLP.DEFAULT_DELTIFIER:
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

	
	
	public void startGoogleThread (final GoogleASR gasr){
		Thread GooogleThread =new Thread(
						"Google Thread") {
					@Override
					public void run() {
						gasr.recognize ();
					}
				};
				GooogleThread.start();
				
				
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
	
	
	public AudioInputStream setupAis (FrontEnd fe) throws UnsupportedAudioFileException, IOException {
		URL audioURL = clp.getAudioURL();
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
		
		return ais;
	}
	
	

	public BaseDataProcessor setupFileInput(FrontEnd fe) throws UnsupportedAudioFileException, IOException  {
		StreamDataSource sds;
		sds = (StreamDataSource) cm.lookup("streamDataSource");
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
		
		
		case GoogleSphinxRCLP.FILE_INPUT:
			fe.setPredecessor(setupFileInput(fe));
			break;
		}
		
	}

	private void setupDecoder() throws IOException {
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			logger.info("Running in forced alignment mode.");
			logger.info("Will try to recognize: " + clp.getReference());
			cm.setGlobalProperty("linguist", "flatLinguist");
			cm.setGlobalProperty("grammar", "forcedAligner");
			Linguist linguist = (Linguist) cm.lookup("flatLinguist");
			linguist = (Linguist) cm.lookup("flatLinguist");
			
			
			linguist.allocate();
			
			AlignerGrammar forcedAligner = (AlignerGrammar) cm
					.lookup("forcedAligner");
			forcedAligner.setText(clp.getReference());
			
		}  else {
			logger.info("Loading recognizer...");
		}
	}

	@SuppressWarnings("unused")
	private void setupMonitors() throws PropertyException {
		ResultListener resultlistener = (ResultListener) cm
				.lookup("currentASRHypothesis");
		recognizer.addResultListener(resultlistener);
		ResultListener resultlistenerFA = (ResultListener) cm
				.lookup("currentASRHypothesis");
		recognizerFA.addResultListener(resultlistenerFA);
		SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
		if (clp.matchesOutputMode(GoogleSphinxRCLP.TED_OUTPUT)) {
			casrh.addListener((PushBuffer) cm.lookup("tedNotifier"));
		}
		if (clp.matchesOutputMode(GoogleSphinxRCLP.LABEL_OUTPUT)) {
			cm.getPropertySheet("labelWriter2").setBoolean(LabelWriter.PROP_WRITE_FILE, true);
			cm.getPropertySheet("labelWriter2").setString(LabelWriter.PROP_FILE_NAME, clp.getLabelPath());
			LabelWriter lw = (LabelWriter) cm.lookup("labelWriter2");
			casrh.addListener(lw);
		
		}
		if (clp.matchesOutputMode(GoogleSphinxRCLP.CURRHYP_OUTPUT)) {
			casrh.addListener((PushBuffer) cm.lookup("hypViewer"));
		}
		if (clp.verbose()) {
			cm.lookup("memoryTracker");
			cm.lookup("speedTracker");
		}
		// this is a little hacky, but so be it
		if (clp.matchesOutputMode(GoogleSphinxRCLP.DISPATCHER_OBJECT_OUTPUT)) {
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
	
	public void allocateRecognizerFA() {
		if (recognizerFA != null && recognizerFA.getState() == Recognizer.State.DEALLOCATED) {
	    	logger.info("Allocating recognizer...");
			recognizerFA.allocate();
		}
	}
	
	/** call this if you want a single recognition */
	public void recognizeOnce() {
		
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			
		
		
		
		
		gasr.recognize(); 
		/*logger.info("Time google start: "+System.currentTimeMillis());
		StreamDataSource sdsS = (StreamDataSource) cm.lookup("streamDataSource");
		
		sdsS.initialize();
		
		

		AudioInputStream aisS = new AudioInputStream(new ByteArrayInputStream(
		rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
		AudioInputStream aisG = new AudioInputStream(new ByteArrayInputStream(
				rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
	
       
		sdsS.setInputStream(aisS, "sphinx");
		this.recognizer = (Recognizer) cm.lookup("recognizer");
		assert recognizer != null;
*/
		//logger.info("Setting up monitors...");
		/*setupMonitors();
		allocateRecognizer();
		
			
			Result result = null;
			
			
			do {
				
				
				
				result = recognizer.recognize();
				 
			
				
				
				logger.info("GSR status"+gasr.getStatus());
				logger.info("clp.getReferenceText()"+clp.getReference());
				
				if (result != null) {
					// Normal Output
					String phones=result.getBestPronunciationResult();
					AlignerGrammar forcedAligner = (AlignerGrammar) cm.lookup("forcedAligner");
					forcedAligner.getInitialNode().dump();
					logger.info("RESULT: " + result.toString());
					logger.info("PHONES: " + phones);
					logger.info("Normal: recognizer after google");
					
					
				
					
					
				} else {
					
					logger.info("Result: null");
					logger.info("Else: recognizer after google");
				}
			} while ((result != null) && (result.getDataFrames() != null)
					&& (result.getDataFrames().size() > 4));
			
			
			
			
			
			
			*/
			
			
			
			

		
		
		}
		
	}
		
		

	

	/** call this if you want to implement recognition looping yourself */
	public Recognizer getRecognizer() {
		return recognizer;
	}
	
	public Recognizer getRecognizerFA() {
		return recognizerFA;
	}

	public static void main(String[] args) throws IOException,
			PropertyException, UnsupportedAudioFileException {
		PropertyConfigurator.configure("log4j.properties");
		GoogleSphinxRCLP clp = new GoogleSphinxRCLP(args);
		if (!clp.parsedSuccessfully()) {
			throw new IllegalArgumentException(
					"No arguments given or illegal combination of arguments.");
		}
		
		
		SphinxGoogleSimpleReco gschsimpleReco = new SphinxGoogleSimpleReco(clp);
		//gschsimpleReco.recognizeOnce();
		
		gschsimpleReco.recognizeSimultaneously();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		gschsimpleReco.getRecognizer().deallocate();
		gschsimpleReco.getRecognizerFA().deallocate();
		System.exit(0);
	}

	private void recognizeSimultaneously() {
		SphinxThread spth=new SphinxThread (recognizer);
		spth.start();
		startGoogleThread(gasr);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		System.err.println("This is left buffer update");
		logger.info("Time by update: "+System.currentTimeMillis());
		
		ArrayList<Double> starttimes=new ArrayList<Double>();
		ArrayList<Double> endtimes=new ArrayList<Double>();
		
		TreeMap<Integer, String> hypsclone = new TreeMap<Integer, String>();
		hypsclone=(TreeMap<Integer, String>)hyps.clone();
		String text=new String ();
		
		
		
		if (!edits.isEmpty()) {
			
			
			for (int i=0; i<edits.size(); i++){
				
				logger.info("edits"+edits.toString());
						
			//add
			if (edits.get(i).getType()==EditType.ADD){
				
				
				Integer iuNumber=edits.get(i).getIU ().getID();
				
				String word=edits.get(i).getIU ().toString().split("\\s+")[3]+" ";
				logger.info("Add Type: "+edits.get(i).getType());
				
				hyps.put(iuNumber, word); 
				
				
			
				starttimes.add((double) edits.get(i).getIU ().startTime());
				endtimes.add((double) edits.get(i).getIU ().endTime());
				
					
			//delete	
			}
				else if (edits.get(i).getType()==EditType.REVOKE){
					
					logger.info("Revoke: "+edits.get(i).getType());
					
					
					String word=edits.get(i).getIU ().toString().split("\\s+")[3]+" ";
					
					for (Entry<Integer, String> entry : hypsclone.entrySet()) {
						 if (entry.getValue().equals(word)){
							hyps.remove(entry.getKey());
								
							starttimes.remove((double) edits.get(i).getIU ().startTime());
							endtimes.remove((double) edits.get(i).getIU ().endTime());
						 }
						  
						 
						}
				}
				
			
			//commit
				else if (edits.get(i).getType()==EditType.COMMIT){
					
					logger.info("Commit"+edits.get(i).getType());
						 
					 
					}
									
				
				else {
					
					
					logger.info("no changes");
				}
	
			
				
				
				
				
				
			} 
		} else {
			logger.info("empty"); 
		} 
		
		
		
		for (Entry<Integer, String> entry : hyps.entrySet()) {
			  text+=entry.getValue();
			  
			}
		
		
		
		
		
		
			try {
				setText(text);
				logger.info("text"+text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		
		
		
				
				updateInputStream(starttimes,endtimes);	
				//setupMonitors();
				//allocateRecognizer();
			
				//this.recognizer = (Recognizer) cm.lookup("recognizer");
				//assert recognizer != null;

				logger.info("Setting up monitors...");
				setupMonitors();
				
		
		Result result = null;
		//logger.info("Recognizer sate" +recognizer.getState().toString());
		logger.info("Recognizer sate" +recognizerFA.getState().toString());
		
		 
		do {
			
			
			
		
			//result = recognizer.recognize();
			result = recognizerFA.recognize();
	
			
			
			
			
			
			if (result != null) {
				// Normal Output
				String phones=result.getBestPronunciationResult();
				logger.info("clp.getReferenceText()"+clp.getReference());
				//logger.info("RESULT: " + result.toString());
				//logger.info("PHONES: " + phones);
				logger.info ("Dump by left buffer update");
				//forcedAligner.getInitialNode().dump();
				logger.info("Normal resullt incr");
			
				
				
				
				
			
				
				
			} else {
				
				logger.info("Result: null");
				logger.info("null resullt incr");
			}
		} while ((result != null) 
				&& (result.getDataFrames() != null)&& (result.getDataFrames().size() > 4)
				);
		
		gasr.getBuffer().clearBuffer();
	}
	
	

	public String []iUToStringArray (IU iu){
		return iu.toString().split("\\s+");
	}
	
	
	public int getEnd(IU iu){
		
		return (int)(Float.valueOf(iUToStringArray(iu) [2])*1000);
	}
	
	public int getStart(IU iu){
		return (int)(Float.valueOf(iUToStringArray(iu) [1])*1000);
	}
	
	public void setText (String text) throws IOException{
		
		
		clp.setReferenceText(text);	
		AlignerGrammar forcedAligner = (AlignerGrammar) cm.lookup("forcedAligner");
		forcedAligner.setText(clp.getReference());
	   
		//logger.info ("searchGraph: "+linguist.getSearchGraph().getInitialState().toPrettyString());
			
		
	}
	
	public void updateInputStream (ArrayList<Double> starttimes, ArrayList<Double> endtimes){
		
			
		int startInBytes = 0;
		int endInBytes = 0;
		double startInSec=0;
		double offset=0;
		//double offset=0.690;
		double endInSec=0;
		logger.info("startInSec"+starttimes.get(0));
		logger.info("endInSec"+endInSec);
		
			
		if (starttimes.get(0)==0)
			{startInSec=starttimes.get(0);
			offset=endtimes.get(0)-0.030;
			logger.info("offset: "+offset);
			
			
			}else
			{startInSec=starttimes.get(0)-offset;
			
			}	
		
		endInSec=endtimes.get(endtimes.size()-1)-offset;
		logger.info("startInSec"+startInSec);
		logger.info("endInSec"+endInSec);
			
		startInBytes=(int)((rais.getChannels()*rais.getSiteInBits()* rais.getSampleRate()/8.0f) *(startInSec));			
		endInBytes=(int)((rais.getChannels()*rais.getSiteInBits()* rais.getSampleRate()/8.0f)*(endInSec));
			
		  
		StreamDataSource sdsS = (StreamDataSource) cm.lookup("streamDataSourceFA");
		sdsS.initialize();
		
		//byte [] buffer =Arrays.copyOfRange(rais.getSoundData(), startInBytes, endInBytes);
		byte [] buffer =Arrays.copyOfRange(rais.getSoundData(), 0, endInBytes);
		
		//AudioInputStream aisS = new AudioInputStream(new ByteArrayInputStream(
			//rais.getSoundData()), rais.getFormat(), rais.getSoundData().length);
		
		logger.info("Start in Bytes"+startInBytes );
		logger.info("End in Bytes"+endInBytes );
		logger.info("Buffer.length"+buffer.length );
		logger.info("soundData length"+rais.getSoundData().length );
		
		AudioInputStream aisS = new AudioInputStream(new ByteArrayInputStream(buffer),rais.getFormat(), buffer.length);;
				
		sdsS.setInputStream(aisS, "sphinx");
				
				
		}
	

	
}
