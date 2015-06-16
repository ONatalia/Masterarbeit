package inpro.incremental.source;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
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
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

public class GoogleASR extends IUSourceModule {

	@S4String(defaultValue = "en-US")
	public final static String PROP_ASR_LANG = "lang";
	private String languageCode;
	
	@S4String(defaultValue = "Default Key") // default to Timo's key! Really you should use your own.
	public final static String PROP_API_KEY = "apiKey";
	private String googleAPIkey;
	
	@S4String(defaultValue = "16000")
	public final static String PROP_SAMPLING_RATE = "samplingRate";
	private String samplingRate = "16000";
	
	private GoogleJSONListener jsonlistener;
	private FrontEndBackedAudioInputStream ais;

	public GoogleASR(BaseDataProcessor frontend) {
		ais = new FrontEndBackedAudioInputStream(frontend);
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		languageCode = ps.getString(PROP_ASR_LANG);
		googleAPIkey = ps.getString(PROP_API_KEY);
		samplingRate = ps.getString(PROP_SAMPLING_RATE);
	}
	
	public void setAPIKey(String apiKey) {
		googleAPIkey = apiKey;
	}
	
	public void setSamplingRate(String SamplingRate) {
		samplingRate = SamplingRate;
	}
	
	public void setLanguageCode(String string) {
		// TODO Auto-generated method stub
		languageCode = string;
		
	}
	
	public void recognize() {
		try {
			String pair = getPair();
			// setup connection with Google
			HttpURLConnection upCon = getUpConnection(pair);
			// start listening on return connection (separate thread)
			jsonlistener = new GoogleJSONListener(pair);
			Thread listenerThread = new Thread(jsonlistener);
			listenerThread.start();
			// push audio to Google (on this thread)
			DataOutputStream stream = new DataOutputStream(upCon.getOutputStream());
			// write to stream
			writeToStream(stream, ais);
			upCon.disconnect();

			// on data end, end listening thread and tear down connection with google
			Thread.sleep(100);
			jsonlistener.shutdown();
			listenerThread.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** get connection to Google */
	private HttpURLConnection getUpConnection(String pair) throws IOException {
		HttpURLConnection connection;

		String upstream = "https://www.google.com/speech-api/full-duplex/v1/up?"
				+ "key="
				+ googleAPIkey
				+ "&pair="
				+ pair
				+ "&lang=" + languageCode
				+ "&maxAlternatives=10&client=chromium&continuous&interim&output=json&xjerr=1";
		URL url = new URL(upstream);
		connection = (HttpURLConnection) url.openConnection();
		// adjust the connection
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setChunkedStreamingMode(1200);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Transfer-Encoding", "chunked");
		connection.setRequestProperty("Content-Type", "audio/x-flac; rate=" + samplingRate);
		connection.setRequestProperty("User-Agent",
			"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36");
		connection.setConnectTimeout(60000);
		connection.setUseCaches(false);
		return connection;
	}

	private HttpURLConnection getDownConnection(String pair) throws IOException {
		HttpURLConnection connection = null;
		// String request = "https://www.google.com/speech-api/v2/recognize?" +
		// "xjerr=1&client=chromium&lang=en-US&maxresults=10&pfilter=0&key="+key+"&output=json";
		String downstream = "https://www.google.com/speech-api/full-duplex/v1/down?pair="+pair;

		URL url = null;
		url = new URL(downstream);
		connection = (HttpURLConnection) url.openConnection();
		// adjust the connection
		//connection.setDoInput(true);
		connection.setDoOutput(true);
		//connection.setChunkedStreamingMode(0);
		//connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("GET");
		connection.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36");
		connection.setConnectTimeout(60000);
		connection.setUseCaches(false);
		return connection;
	}

	private String getPair() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(96, random).toString(32);
	}

	private final static byte[] FINAL_CHUNK = new byte[] { '0', '\r', '\n', '\r', '\n' };
	
	/** write audio data to the stream 
	 * @throws IOException 
	 * @throws LineUnavailableException */
	private void writeToStream(DataOutputStream stream, AudioInputStream ai) throws IOException, LineUnavailableException {
		int buffer_size = 320; // 2000 samples = 250ms; 320 = 10ms
		byte tempBuffer[] = new byte[buffer_size];

//		Printer.printWithTime(TAG, "buffer size: " + buffer_size);
	
		boolean run = true;
		InputStream byteInputStream;
		FLACFileWriter ffw = new FLACFileWriter();
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		AudioInputStream ais;
//		Printer.printWithTime(TAG, "recording started");
		while (run) {
//			System.err.println("sending chunk " + i);
			int cnt = -1;
			// read data from the audio input stream
			cnt = ai.read(tempBuffer, 0, buffer_size);
			if (cnt > 0) {// if there is data
				byteInputStream = new ByteArrayInputStream(tempBuffer);
				ais = new AudioInputStream(byteInputStream, ai.getFormat(),
						cnt); // open a new audiostream
				ffw.write(ais, FLACFileWriter.FLAC, boas);// convert audio
				stream.write(boas.toByteArray());// write FLAC audio data to
													// the output stream to
													// google
				boas.reset();
			} else {
				if (ai instanceof FrontEndBackedAudioInputStream && (((FrontEndBackedAudioInputStream) ai).hasMoreAudio())) {
//					stream.write(FINAL_CHUNK);
//					stream.flush();
					// then get more audio in next step
				} else
					run = false;
			}
		}
		stream.write(FINAL_CHUNK);
		stream.flush();
		stream.close();
	}
	
	public GoogleJSONListener getJSONListener() {
		return this.jsonlistener;
	}

	public class GoogleJSONListener implements Runnable {
		
		HttpURLConnection con;
		boolean inShutdown = false;
		
		private double startTime;
		private double prevTime;
		private IUList<WordIU> chunkHyps;
		private int lastResultIndex;
		private WordIU prev;
		private double initialTime;
		
		public GoogleJSONListener(String pair) throws IOException {
			prevTime = 0;
			chunkHyps = new IUList<WordIU>();
			setLastResultIndex(-1);
			prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
			this.con = getDownConnection(pair);
			setInitialTime(getTimestamp());
			setStartTime(getTimestamp());
		}

		private void setInitialTime(double timestamp) {
			initialTime = timestamp;
		}
		
		private double getInitialTime() {
			return initialTime;
		}

		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while (!inShutdown) {
					// get JSON result
					String decodedString;
					while ((decodedString = in.readLine()) != null) {
						parseJSON(decodedString);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public void parseJSON(String decodedString) {
			JSONObject json = new JSONObject(decodedString);
			JSONArray result = json.getJSONArray("result");
			if (result.length() == 0) return;
			
			String transcript = result.getJSONObject(0).getJSONArray("alternative").getJSONObject(0).getString("transcript");
			int resultIndex = json.getInt("result_index");
			
			boolean isFinal = result.getJSONObject(0).has("final");
			updateCurrentHyps(transcript, resultIndex, isFinal);
			setLastResultIndex(resultIndex);
		}

		private void updateCurrentHyps(String transcript, int resultIndex, boolean isFinal) {
			
			List<String> words = Arrays.asList(transcript.toLowerCase().trim().split("\\s+"));
					
			IUList<WordIU> currentHyps = new IUList<WordIU>();

//			keep working on the frontier
			if (startNewChunk(resultIndex)) {
				currentHyps.clear();
				chunkHyps.clear();
			}
			
			double currentTimestamp = getTimestamp();
			
			double delta = (currentTimestamp - getStartTime()) / (double) words.size();
			int currentFrame = (int) (currentTimestamp - getInitialTime()) / 10; // I have no clue why we divide by 10, something with the timestamp 
			double i = 1.0;
			double lastEndTime = 0.0;
			for (String word : words) {
				double startTime = prevTime + delta * (i-1);
				double endTime = prevTime + delta * i;
//				System.out.println(word + " " + startTime + " " + endTime + " " + startTime/1000.0 + " " +  endTime/1000.0);
				SegmentIU siu = new SegmentIU(new Label(startTime/1000.0, endTime/1000.0, word)); 
				lastEndTime = endTime;
				List<IU> gIns = new LinkedList<IU>();
				gIns.add(siu);
				WordIU wiu = new WordIU(word, prev, gIns);
				prev = wiu;
				currentHyps.add(wiu);
				i++;
			}
			
//			This calculates the differences between the current IU list and the previous, based on payload
			List<EditMessage<WordIU>> diffs = chunkHyps.diffByPayload(currentHyps);
			chunkHyps.clear();
			chunkHyps.addAll(currentHyps);
			if (isFinal) {
//				add remaining WordIUs and commit
				for (WordIU wordIU : chunkHyps) {
					diffs.add(new EditMessage<WordIU>(EditType.COMMIT, wordIU));
				}
				currentHyps.clear();
				chunkHyps.clear();
				prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
				
			}
			LinkedList<WordIU> ius = new LinkedList<WordIU>();
			for (EditMessage<WordIU> edit: diffs) ius.add(edit.getIU()); 
			
			if (startNewChunk(i)) {
				setStartTime(getTimestamp());
				prevTime = lastEndTime;
			}
			
//			The diffs represents what edits it takes to get from prevList to list, send that to the right buffer
			for (PushBuffer listener : iulisteners) {
				if (listener == null) continue;
				if (listener instanceof FrameAware)
					((FrameAware) listener).setCurrentFrame(currentFrame);
				// update frame count in frame-aware pushbuffers
				if (diffs != null && !diffs.isEmpty())
					listener.hypChange(ius, diffs);
			}
			notifyListeners();
		}
		
		public double getTimestamp() {
			return  (System.currentTimeMillis());
		}
		
		public IUList<WordIU> getChunkHyps() {
			return chunkHyps;
		}

		public void shutdown() {
			inShutdown = true;
		}

		public int getLastResultIndex() {
			return lastResultIndex;
		}

		public void setLastResultIndex(int lastResultIndex) {
			this.lastResultIndex = lastResultIndex;
		}
		
		public boolean startNewChunk(double i) {
			return i > getLastResultIndex();
		}

		public double getStartTime() {
			return startTime;
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}
		
	}

}
