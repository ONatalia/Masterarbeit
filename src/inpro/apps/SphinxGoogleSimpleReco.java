package inpro.apps;

import inpro.apps.SimpleMonitor;
import inpro.apps.util.GoogleSphinxRCLP;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.apps.util.GoogleSphinxRCLP;
import inpro.audio.AudioUtils;
import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.processor.FAModule;
import inpro.incremental.sink.LabelWriter;
import inpro.sphinx.frontend.DataThrottle;
import inpro.sphinx.frontend.RsbStreamInputSource;
import inpro.incremental.source.GoogleASR;
import inpro.incremental.source.GoogleThread;
import inpro.incremental.source.SphinxASR;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.source.GoogleASR.GoogleJSONListener;
import inpro.incremental.source.GoogleASR.LiveJSONListener;
import inpro.incremental.source.GoogleASR.PlaybackJSONListener;
import inpro.sphinx.frontend.RtpRecvProcessor;
import inpro.util.PathUtil;
import inpro.util.TimeUtil;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.util.Microphone;
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
	private final GoogleASR gasr;
	//private final FAModule fa;
	private Linguist linguist;
	//private boolean googleIsStarted=false;
	private byte [] soundData=null;
	AlignerGrammar forcedAligner;
	Decoder decoder;
	FrontEnd fe;
	int framecounter=1;

	public Linguist getLinguist() {
		return linguist;
	}

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
		setupDecoder();
		logger.info("Setting up source...");
		//setupSource();

		//this.recognizer = (Recognizer) cm.lookup("recognizer");
		//assert recognizer != null;

		//logger.info("Setting up monitors...");
		//setupMonitors();
		//allocateRecognizer();
		//// deal with GoogleASR based on the above:
		//fa=new FAModule(cm,clp, linguist);
		
	
		
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			logger.info("Setting up source...");
			BaseDataProcessor realtime = new DataThrottle();
			FrontEnd feg=(FrontEnd)cm.lookup("googlefrontend");
			realtime.setPredecessor(setupFileInput(feg));
			gasr = new GoogleASR(realtime);
			gasr.newProperties(cm.getPropertySheet("googleASR"));
			gasr.setAPIKey(clp.getGoogleAPIkey());
			gasr.setExportFile(clp.getGoogleDumpOutput());
			gasr.setImportFile(clp.getGoogleDumpInput());
			SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
			
			
			
			
		    //gasr.iulisteners.add(fa);
			gasr.iulisteners.add(this);
			this.iulisteners=casrh.iulisteners;
			
		    //fa.iulisteners=casrh.iulisteners;
			//gasr.iulisteners.add((PushBuffer) cm.lookup("faModule"));
			
			
			
			
			
			//logger.info("fa listeners"+fa.iulisteners.toString());
			logger.info("gasr listeners"+gasr.iulisteners.toString());
			//logger.info("casrh listeners"+casrh.iulisteners.toString());
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
		//FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		FrontEnd feg = (FrontEnd) cm.lookup("frontend");
		final RsbStreamInputSource rsbInputStream = (RsbStreamInputSource) cm
				.lookup("RsbStreamInputSource");
		FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
		setupVuMeter(rsbInputStream);
		vumeter.setPredecessor(rsbInputStream);
		endpoint.setPredecessor(vumeter);
		vumeter.getVuMeterDialog().setVisible(false);
		endpoint.initialize();
		setupRsbInputSource(rsbInputStream);
		//fe.setPredecessor(endpoint);
		feg.setPredecessor(endpoint);
	}

	public BaseDataProcessor setupFileInput(FrontEnd fe) throws UnsupportedAudioFileException, IOException  {
		StreamDataSource sds;
		if (fe.equals((FrontEnd) cm.lookup("googlefrontend" ))){
			sds=(StreamDataSource) cm.lookup("streamDataSourceGoogle");
		}
		else {
			sds = (StreamDataSource) cm.lookup("streamDataSource");
		}
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
		
		
		/*MelFrequencyFilterBank filter=(MelFrequencyFilterBank) cm.lookup("melFilterBank");
		if (filter.getTimer().isStarted()==true) {
			filter.getTimer().stop();
			logger.info("check filter");
		
		}
		*/
		
		
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
		fe = (FrontEnd) cm.lookup("frontend");
		//FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		//FrontEnd feg=(FrontEnd)cm.lookup("googlefrontend");
		switch (clp.getInputMode()) {
		
		
		case GoogleSphinxRCLP.FILE_INPUT:
			fe.setPredecessor(setupFileInput(fe));
			//feg.setPredecessor (setupFileInput(feg));
			break;
		}
		
	}

	private void setupDecoder() throws IOException {
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			logger.info("Running in forced alignment mode.");
			logger.info("Will try to recognize: " + clp.getReference());
			cm.setGlobalProperty("linguist", "flatLinguist");
			cm.setGlobalProperty("grammar", "forcedAligner");
			//Linguist linguist = (Linguist) cm.lookup("flatLinguist");
			linguist = (Linguist) cm.lookup("flatLinguist");
			
			
			linguist.allocate();
			
			
			//AlignerGrammar forcedAligner = (AlignerGrammar) cm
					//.lookup("forcedAligner");
			forcedAligner = (AlignerGrammar) cm
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
	
	/** call this if you want a single recognition */
	public void recognizeOnce() {
		
		if (clp.isRecoMode(GoogleSphinxRCLP.GOOGLE_SPHINX_RECO)) {
			
		
		
		
		
		gasr.recognize(); 
		logger.info("Time google start: "+System.currentTimeMillis());
		SphinxASR casrh = (SphinxASR) cm.lookup("currentASRHypothesis");
		casrh.iulisteners.clear();
		
		
		logger.info("Setting up source...");   
		try {
			setupSource();    
		} catch (PropertyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.recognizer = (Recognizer) cm.lookup("recognizer");
		assert recognizer != null;

		logger.info("Setting up monitors...");
		setupMonitors();
		allocateRecognizer();
		
			
			Result result = null;
			
			
			do {
				
				
			
				result = recognizer.recognize();
				
				
				
				logger.info("clp.getReferenceText()"+clp.getReference());
				
				if (result != null) {
					// Normal Output
					String phones=result.getBestPronunciationResult();
					
					forcedAligner.getInitialNode().dump();
					logger.info("RESULT: " + result.toString());
					logger.info("PHONES: " + phones);
					
					
				
					
					
				} else {
					
					logger.info("Result: null");
				}
			} while ((result != null) && (result.getDataFrames() != null)
					&& (result.getDataFrames().size() > 4));
			

		
		
		}
		
	}
		
		

	

	/** call this if you want to implement recognition looping yourself */
	public Recognizer getRecognizer() {
		return recognizer;
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
		gschsimpleReco.recognizeOnce();
		
		
		gschsimpleReco.getRecognizer().deallocate();
		System.exit(0);
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		System.err.println("This is left buffer update");
		logger.info("Time by update: "+System.currentTimeMillis());
		
		Map <Integer,String> hyps=new TreeMap <Integer,String>();
		ArrayList<Float> starttimes=new ArrayList<Float>();
		ArrayList<Float> endtimes=new ArrayList<Float>();
		String text=new String ();
		int start = 0;
		int end=0;
		
		
		
		if (!edits.isEmpty()) {
			
			logger.info("Current hypothesis is now:");
			for (IU iu : ius) {
				logger.info("IU:"+iu.toString());
				
				
				
				hyps.put(Integer.parseInt(iu.toString().split("\\s+")[0].replace("IU:", "").replace(",", "")), iu.toString().split("\\s+")[3]+" ");
				
				starttimes.add((float) getStart(iu));
				endtimes.add((float) getEnd(iu));
				
					
						
				
				
			} 
		} else {
			logger.info("empty"); 
		} 
		
		
		
		for (Entry<Integer, String> entry : hyps.entrySet()) {
			  text+=entry.getValue();
			}
		
		
		
		
			try {
				setText(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		
		
		
				
				/*updateInputStream(starttimes.get(starttimes.size()-1),endtimes.get(endtimes.size()-1));
			
			
		
		
		Result result = null;
		logger.info("Recognizer sate" +recognizer.getState().toString());
		
		//result=decoder.decode(clp.getReference());
		 
		do {
			
			
			
		
			result = recognizer.recognize();
			
	
			
			
			
			
			
			if (result != null) {
				// Normal Output
				String phones=result.getBestPronunciationResult();
				logger.info("clp.getReferenceText()"+clp.getReference());
				logger.info("RESULT: " + result.toString());
				logger.info("PHONES: " + phones);
				logger.info ("Dump by left buffer update");
				forcedAligner.getInitialNode().dump();
			
				
				
				
				
			
				
				
			} else {
				
				logger.info("Result: null");
			}
		} while ((result != null) 
				&& (result.getDataFrames() != null)&& (result.getDataFrames().size() > 4)
				);
		
		*/
	}
	
	private void resetFA() {
		// TODO Auto-generated method stub
		
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
		
		//cm.setGlobalProperty("linguist", "flatLinguist");
		//cm.setGlobalProperty("grammar", "forcedAligner");
		//linguist = (Linguist) cm.lookup("flatLinguist");
		
		//linguist.deallocate();
		//forcedAligner.deallocate();
		
		
		//linguist.allocate();
		//forcedAligner.allocate();
		
		//forcedAligner = (AlignerGrammar) cm
				//.lookup("forcedAligner");
		clp.setReferenceText(text);	
		forcedAligner.setText(clp.getReference());
	   
		//logger.info ("searchGraph: "+linguist.getSearchGraph().getInitialState().toPrettyString());
		
		
		
		
		
		
		
	}
	
	public void updateInputStream (float startInMillies, float endInMillies){
		
		//setupDeltifier();
		//try {
			//setupDecoder();
		//} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		logger.info("Setting up source...");

		
		StreamDataSource sds1 = (StreamDataSource) cm.lookup("streamDataSource");
		
		sds1.initialize();
		URL audioURL = clp.getAudioURL();
		AudioInputStream ais = null;
		AudioInputStream ais1=null;
		int startInBytes = 0;
		int endInBytes = 0;
		try {
			ais = AudioUtils.getAudioStreamForURL(audioURL);
			AudioFormat format=ais.getFormat();
			
			float sampleRate=format.getSampleRate();
			float channels=format.getChannels();
			float sizeInBits=format.getSampleSizeInBits();
			
			
			long startInSec=(long) (startInMillies/1000.0f);	
			long endInSec=(long) ((endInMillies)/1000.0f);
			
			
			startInBytes=(int)((channels*sizeInBits* sampleRate/8.0f) *(startInSec));			
		    endInBytes=(int)((channels*sizeInBits* sampleRate/8.0f)*(endInSec));
			
		  
			
		    
			
			
			
			
			
			
			int bytesPerSecond = format.getFrameSize() * (int)format.getFrameRate();
		    //ais.skip(startInSec* bytesPerSecond);
		    long framesOfAudioToCopy = (endInSec-startInSec )* (int)format.getFrameRate();
		    //ais1 = new AudioInputStream(ais, format, framesOfAudioToCopy);
		    ais1 = new AudioInputStream(ais, format, 0);
		   
		    
		   
			soundData = new byte [(int) (ais.getFrameLength()*format.getFrameSize())*format.getChannels()];
			
			byte [] buffer=null;
			
		
			
				buffer =new byte [(int) ais.getFrameLength()/4*framecounter];
				
			
			logger.info ("framecounter"+framecounter);
			logger.info ("Frame length"+ais.getFrameLength());
			logger.info ("sound length"+soundData.length);
			logger.info ("buffer length"+buffer.length);
			
			
			if (buffer.length>soundData.length){
				buffer =new byte [(int) ais.getFrameLength()*format.getFrameSize()*format.getChannels()];
			}
			
			ais.read (soundData, 0, soundData.length);
			
			
			System.arraycopy(soundData, 0, buffer,0, buffer.length);
			framecounter++;

			//file distanation
			File destinationFile = new File("/Users/Natalia/inprotk/res/file.wav");


			AudioInputStream is1 = new AudioInputStream(new ByteArrayInputStream(
					buffer), format, buffer.length);
			
			AudioSystem.write(is1, AudioFileFormat.Type.WAVE, destinationFile);
			
			AudioInputStream ais2 = AudioUtils.getAudioStreamForURL(PathUtil.anyToURL("file:../res/file.wav"));
			
			sds1.setInputStream (ais2,PathUtil.anyToURL("file:../res/file.wav").getFile());
		
			
			
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		
	// sds1.setInputStream(ais1, audioURL.getFile());
	   
	  
	 // sds1.setInputStream(ais, audioURL.getFile());
		
	   fe.setPredecessor (sds1);
		
		
			
		
		
			
			
			
		
		
		//this.recognizer = (Recognizer) cm.lookup("recognizer");
		//assert recognizer != null;

		//logger.info("Setting up monitors...");
		setupMonitors();
		//allocateRecognizer();

	}
	

	
}
