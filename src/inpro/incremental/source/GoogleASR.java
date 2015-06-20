package inpro.incremental.source;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javaFlacEncoder.FLACFileWriter;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;

import org.json.JSONArray;
import org.json.JSONObject;

import inpro.annotation.Label;
import inpro.audio.FrontEndBackedAudioInputStream;
import inpro.incremental.FrameAware;
import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IUList;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.TextualWordIU;
import inpro.incremental.unit.WordIU;
import inpro.util.PathUtil;
import inpro.util.TimeUtil;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/** 
 * Incremental speech recognition based on GoogleASR
 * 
 * Uses Google's Full-Duplex Speech API (Version 1), which uses one 
 * HTTP POST connection for uploading audio and one HTTP GET connection
 * for asynchronously receiving intermediate results during recognition.
 * There's no promise of intermediate results, but they do occur every once in a while.
 * 
 * Google communicates no time-alignments for recognized words so we use a heuristic
 * to estimate timings of word alignments.
 * 
 * @author casey, timo, jtwiefel
 */
public class GoogleASR extends IUSourceModule {

	@S4String(defaultValue = "en-US")
	public final static String PROP_ASR_LANG = "lang";
	private String languageCode = "en-US";
	
	@S4String(defaultValue = "SetThisToYourGoogleApiKey")
	public final static String PROP_API_KEY = "apiKey";
	private String googleAPIkey;
	
	@S4String(defaultValue = "16000")
	public final static String PROP_SAMPLING_RATE = "samplingRate";
	private String samplingRate = "16000";
	
	@S4String(defaultValue = "")
	public final static String PROP_READ_JSON_DUMP = "importJSONDump";
	private URL jsonDumpInput;
	
	@S4String(defaultValue = "")
	public final static String PROP_WRITE_JSON_DUMP = "exportJSONDump";
	private File jsonDumpOutput;
	
	/** used by google as a signal that audio transmission has ended */
	private final static byte[] FINAL_CHUNK = new byte[] { '0', '\r', '\n', '\r', '\n' };
	/** size of the output buffer, how much audio to send to google at a time */
	private final static int BUFFER_SIZE = 320; // 2000 samples = 250ms; 320 = 10ms
	/** that's who we pretend to be when talking with Google */
	private final static String UA_STRING = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
	
	private final FrontEndBackedAudioInputStream ais;

	public GoogleASR(BaseDataProcessor frontend) {
		ais = new FrontEndBackedAudioInputStream(frontend);
		iulisteners = new ArrayList<PushBuffer>(1); // avoid nullpointer exceptions
	}

	/** enable Sphinx configuration */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		setLanguageCode(ps.getString(PROP_ASR_LANG));
		setAPIKey(ps.getString(PROP_API_KEY));
		setSamplingRate(ps.getString(PROP_SAMPLING_RATE));
		try {
			if (!"".equals(ps.getString(PROP_READ_JSON_DUMP))) {
					setImportFile(PathUtil.anyToURL(ps.getString(PROP_READ_JSON_DUMP)));
			} else {
				setImportFile(null);
			}
		} catch (MalformedURLException e) {
			throw new PropertyException(e);
		}
		if (!"".equals(ps.getString(PROP_WRITE_JSON_DUMP))) {
			setExportFile(new File(ps.getString(PROP_WRITE_JSON_DUMP)));
		} else {
			setExportFile(null);
		}
	}
	
	/** write audio to stream and push results to listeners of our RightBuffer */
	public void recognize() {
		try {
			String pair = getPair();
			GoogleJSONListener jsonlistener;
			OutputStream upStream;
			if (jsonDumpInput == null) {
				// setup connection with Google
				HttpURLConnection upCon = getUpConnection(pair);
				// push audio to Google (on this thread)
				upStream = new DataOutputStream(upCon.getOutputStream());
				// start listening on return connection (separate thread)
				jsonlistener = new LiveJSONListener(pair);
			} else {
				// get downstream 
				jsonlistener = new PlaybackJSONListener(jsonDumpInput);
				// send upstream to /dev/null
				upStream = new OutputStream() {
					@Override public void write(int b) throws IOException { }
				};
			}
			Thread listenerThread = new Thread(jsonlistener);
			listenerThread.start();
			// write to stream
			writeToStream(upStream, ais);
			// on data end, end listening thread and tear down connection with google
			Thread.sleep(400);
			jsonlistener.shutdown();
			listenerThread.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** get connection to Google that we use for uploading our audio data */
	private HttpURLConnection getUpConnection(String pair) throws IOException {
		HttpURLConnection connection;

		String upstream = "https://www.google.com/speech-api/full-duplex/v1/up?"
				+ "key=" + googleAPIkey
				+ "&pair=" + pair
				+ "&lang=" + languageCode
				+ "&maxAlternatives=10&client=chromium&continuous&interim&output=json&xjerr=1";
		URL url = new URL(upstream);
		connection = (HttpURLConnection) url.openConnection();
		// adjust the connection
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setChunkedStreamingMode(1200); // 1200 bytes per read, i.e. 600 samples, TODO: why 600 samples?
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Transfer-Encoding", "chunked");
		connection.setRequestProperty("Content-Type", "audio/x-flac; rate=" + samplingRate);
		connection.setRequestProperty("User-Agent",UA_STRING);
		connection.setConnectTimeout(60000);
		connection.setUseCaches(false);
		return connection;
	}

	/** get connection to Google that we use for receiving JSON results */
	private HttpURLConnection getDownConnection(String pair) throws IOException {
		HttpURLConnection connection = null;
		String downstream = "https://www.google.com/speech-api/full-duplex/v1/down?pair="+pair;
		URL url = new URL(downstream);
		connection = (HttpURLConnection) url.openConnection();
		// adjust the connection
		connection.setDoOutput(true);
		//connection.setChunkedStreamingMode(0);
		//connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", UA_STRING);
		connection.setConnectTimeout(60000);
		connection.setUseCaches(false);
		return connection;
	}

	/** generate a random ID that can be used to couple up- and downstream URLs */
	private String getPair() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(96, random).toString(32);
	}

	/** write audio data to the stream that is connected to our Google-Upstream
	 * @throws IOException 
	 * @throws LineUnavailableException */
	private void writeToStream(OutputStream stream, AudioInputStream ai) throws IOException, LineUnavailableException {
		byte tempBuffer[] = new byte[BUFFER_SIZE];
		FLACFileWriter ffw = new FLACFileWriter(); // initialize just once instead of within the loop
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE); // initialize just once instead of within the loop

		boolean moreAudioLeft = true;
		while (moreAudioLeft) {
			int cnt = -1;
			cnt = ai.read(tempBuffer, 0, BUFFER_SIZE);
			if (cnt > 0) {// if there is data
				InputStream byteInputStream = new ByteArrayInputStream(tempBuffer);
				AudioInputStream ais = new AudioInputStream(byteInputStream, ai.getFormat(), cnt); // open a new audiostream
				ffw.write(ais, FLACFileWriter.FLAC, baos);// convert audio
				stream.write(baos.toByteArray());// write FLAC audio data to
													// the output stream to google
				baos.reset();
			} else {
				if (ai instanceof FrontEndBackedAudioInputStream || (!((FrontEndBackedAudioInputStream) ai).hasMoreAudio())) {
					moreAudioLeft = false;
				}
			}
		}
		stream.write(FINAL_CHUNK);
		stream.close();
	}
	
	/** connect to Google and receive JSON results */
	class LiveJSONListener extends GoogleJSONListener {
		HttpURLConnection con;
		/** used to terminate this thread */
		boolean inShutdown = false;
		
		LiveJSONListener(String pair) throws IOException {
			this.con = getDownConnection(pair);
		}
		
		void shutdown() {
			inShutdown = true;
		}
		
		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while (!inShutdown) {
					// get JSON result
					String decodedString = in.readLine();
					processJSON(decodedString);
				}
				terminateDump();
			} catch (Exception e) {
				con.disconnect();
				throw new RuntimeException(e);
			}
			con.disconnect();
		}
	}
	
	/** consume timed JSON results from a file or URL */
	class PlaybackJSONListener extends GoogleJSONListener {
		BufferedReader input;
		public PlaybackJSONListener(URL jsonDumpInput) throws IOException {
			input = new BufferedReader(new InputStreamReader(jsonDumpInput.openStream()));
		}

		void shutdown() { } // ignore this
		
		@Override
		public void run() {
			try {
				String timedJSON;
				while ((timedJSON = input.readLine()) != null) {
					String[] split = timedJSON.split("\t", 2);
					int time = Integer.parseInt(split[0]);
					String json = split[1];
					long remainingDelay = time - getTotalElapsedTime();
					assert remainingDelay > 0 : remainingDelay;
					Thread.sleep(remainingDelay);
					processJSON(json);
				}
				terminateDump();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/** 
	 * listen for incoming JSON results coming from Google's downstream connection
	 * 
	 * incoming results are analyzed, turned into IUs, amended with (guessed) time-stamp information, 
	 * and finally pushed to our RightBuffer and listener updated 
	 */
	abstract class GoogleJSONListener implements Runnable {
		private long initialTime; // in milliseconds
		private long chunkStartTime; // in milliseconds
		private long prevEndTime; // in milliseconds
		private IUList<WordIU> chunkHyps;
		private int lastResultIndex;
		private WordIU prev;
		private FileWriter dumpOutput;
		
		GoogleJSONListener() throws IOException {
			prevEndTime = 0;
			chunkHyps = new IUList<WordIU>();
			setLastResultIndex(-1);
			prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
			if (jsonDumpOutput != null) {
				dumpOutput = new FileWriter(jsonDumpOutput);
			}
			initialTime = getTimestamp();
			setChunkStartTime(initialTime);
		}
		
		/* functionality of passing results to a dumpFile */

		void processJSON(String decodedString) throws IOException {
			if (decodedString != null) {
				if (dumpOutput != null) {
					dumpOutput.write(Long.toString(getTotalElapsedTime()));
					dumpOutput.write("\t");
					dumpOutput.write(decodedString);
					dumpOutput.write("\n");
				}
				parseJSON(decodedString);
			}
		}

		synchronized void terminateDump() throws IOException {
			if (dumpOutput != null)
				dumpOutput.close();
			dumpOutput = null;
		}

		/* actual result analysis below */
		
		private void parseJSON(String decodedString) {
			JSONObject json = new JSONObject(decodedString);
			JSONArray result = json.getJSONArray("result");
			if (result.length() > 0) {
				String transcript = result.getJSONObject(0).getJSONArray("alternative").getJSONObject(0).getString("transcript");
				int resultIndex = json.getInt("result_index");
				boolean isFinal = result.getJSONObject(0).has("final");
				updateCurrentHyps(transcript, resultIndex, isFinal);
				setLastResultIndex(resultIndex);
			}
		}

		private void updateCurrentHyps(String transcript, int resultIndex, boolean isFinal) {
			List<String> words = Arrays.asList(transcript.toLowerCase().trim().split("\\s+"));
			IUList<WordIU> currentHyps = new IUList<WordIU>();
			// keep working on the frontier
			if (startsNewChunk(resultIndex)) {
				currentHyps.clear();
				chunkHyps.clear();
			}
			double delta = getElapsedChunkTime() / words.size();
			int currentFrame = (int) (getTotalElapsedTime() * TimeUtil.MILLISECOND_TO_FRAME_FACTOR); 
			int wordIndex = 1;
			long lastEndTime = 0;
			for (String word : words) {
				long startTime = prevEndTime + (int) (delta * (wordIndex - 1));
				long endTime = prevEndTime + (int) (delta * wordIndex);
				SegmentIU siu = new SegmentIU(new Label(startTime/TimeUtil.SECOND_TO_MILLISECOND_FACTOR, endTime/TimeUtil.SECOND_TO_MILLISECOND_FACTOR, word)); 
				lastEndTime = endTime;
				List<IU> gIns = new LinkedList<IU>();
				gIns.add(siu);
				WordIU wiu = new WordIU(word, prev, gIns);
				prev = wiu;
				currentHyps.add(wiu);
				wordIndex++;
			}
			// This calculates the differences between the current IU list and the previous, based on payload
			List<EditMessage<WordIU>> diffs = chunkHyps.diffByPayload(currentHyps);
			chunkHyps.clear();
			chunkHyps.addAll(currentHyps);
			if (isFinal) {
				// add remaining WordIUs and commit
				for (WordIU wordIU : chunkHyps) {
					diffs.add(new EditMessage<WordIU>(EditType.COMMIT, wordIU));
				}
				currentHyps.clear();
				chunkHyps.clear();
				prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
			}
			LinkedList<WordIU> ius = new LinkedList<WordIU>();
			for (EditMessage<WordIU> edit: diffs) ius.add(edit.getIU()); 
			if (startsNewChunk(wordIndex)) {
				setChunkStartTime(getTimestamp());
				prevEndTime = lastEndTime;
			}
			// The diffs represents what edits it takes to get from prevList to list, send that to the right buffer
			for (PushBuffer listener : iulisteners) {
				if (listener == null) continue;
				if (listener instanceof FrameAware)
					((FrameAware) listener).setCurrentFrame(currentFrame);
				// update frame count in frame-aware pushbuffers
				if (!diffs.isEmpty())
					listener.hypChange(ius, diffs);
			}
			notifyListeners();
		}
		
		abstract void shutdown();
		
		long getTimestamp() {
			return  System.currentTimeMillis();
		}
		
		protected long getTotalElapsedTime() {
			return getTimestamp() - initialTime;
		}
		
		private long getElapsedChunkTime() {
			return getTimestamp() - chunkStartTime;
		}
		
		private void setChunkStartTime(long startTime) {
			this.chunkStartTime = startTime;
		}
		
		private int getLastResultIndex() {
			return lastResultIndex;
		}

		private void setLastResultIndex(int lastResultIndex) {
			this.lastResultIndex = lastResultIndex;
		}
		
		private boolean startsNewChunk(double i) {
			return i > getLastResultIndex();
		}

	}

	void setImportFile(URL url) {
		jsonDumpInput = url;
	}

	void setExportFile(File file) {
		jsonDumpOutput = file;
	}

	public void setAPIKey(String apiKey) {
		googleAPIkey = apiKey;
	}
	
	public void setSamplingRate(String SamplingRate) {
		samplingRate = SamplingRate;
	}
	
	public void setLanguageCode(String string) {
		languageCode = string;
	}
	
}
