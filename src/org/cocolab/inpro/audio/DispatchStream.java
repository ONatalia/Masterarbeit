package org.cocolab.inpro.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;
import org.cocolab.inpro.annotation.LabelledAudioStream;
import org.cocolab.inpro.gui.util.SpeechStateVisualizer;
import org.cocolab.inpro.tts.MaryAdapter;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * WARNING: there may be threading issues in this class
 * 
 * TODO: would it be nice to directly output SysInstallmentIUs? --> yes, indeed.
 * @author timo
 */
public class DispatchStream extends InputStream implements Configurable {

	private static Logger logger = Logger.getLogger(DispatchStream.class);

	@S4Component(type = SpeechStateVisualizer.class, mandatory = false)
	public final static String PROP_SPEECH_STATE_VISUALIZER = "speechStateVisualizer";
	SpeechStateVisualizer ssv;

	@S4Boolean(defaultValue = false)
	public final static String PROP_SEND_SILENCE = "sendSilence";
	private boolean sendSilence;

	/** A map of tts strings and corresponding audio files. */
	private static Map<String, String> ttsCache = new HashMap<String, String>();

	InputStream stream;
	Queue<InputStream> streamQueue = new ArrayDeque<InputStream>();
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		sendSilence(ps.getBoolean(PROP_SEND_SILENCE));
		ssv = (SpeechStateVisualizer) ps.getComponent(PROP_SPEECH_STATE_VISUALIZER);
	}
	
	public void initialize() {  }

	public boolean isSpeaking() {
		return stream != null;
	}
	
	/**
	 * Reads file names corresponding to utterances strings from a file
	 * and adds them to a local map if they can be found.
	 */
	public static void initializeTTSCache(String utteranceMapFile, String audioPath) {
		try {
			URL url = new URL(utteranceMapFile);
			// workaround for relative local paths 
			if (url.toURI().isOpaque()) {
				url = new URL(new File(".").toURI().toURL(), url.toString());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			logger.info("Loading utterance map.");
			String line;
			while ((line = br.readLine()) != null) {
				String utterance = line.split(",")[0];
				File audio = new File(line.split(",")[1]);
				if (audio.exists()) {
					ttsCache.put(utterance, audio.toURI().toURL().toString());
				} else if (audioPath != null) {
					audio = new File(audioPath + audio.toString());
					if (audio.exists()) {
						ttsCache.put(utterance, audioPath + audio.toString());
					} else {
						logger.warn("Cannot find and won't add audio file " + audio.toString());
					}
				} else {
					logger.warn("Cannot find and won't add audio file " + audio.toString());
				}
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 
	 * determines whether digital zeroes are sent during silence,
	 * or whether the stream just stalls 
	 */
	public void sendSilence(boolean b) {
		sendSilence = b;
	}

	protected void setIsTalking() {
		if (ssv != null) {
			this.ssv.systemTalking(true);			
		}
	}

	protected void setIsSilent() {
		if (ssv != null) {
			this.ssv.systemTalking(false);
		}
	}
	
	/* * Higher-level audio enqueuing (or direct play) * */
	/**
	 * play audio from file
	 * @param filename path to the file to be played
	 * @param skipQueue determines whether the file should be played 
	 *        immediately (skipQueue==true) or be enqueued to be played
	 *        after all other messages have been played 
	 */
	public void playFile(String filename, boolean skipQueue) {
		if (skipQueue)
			logger.info("Now playing file " + filename);
		else
			logger.info("Now appending file " + filename);
		AudioInputStream audioStream;
		try {
			audioStream = new LabelledAudioStream(filename); 
		} catch (Exception e) {
			logger.error("can't play file " + filename);
			audioStream = null;
			e.printStackTrace();
		}
		playStream(audioStream, skipQueue);
	}
	
	public void playTTS(String tts, boolean skipQueue) {
		if (ttsCache.containsKey(tts)) {
			playFile(ttsCache.get(tts), skipQueue);
		} else {
			try {
				AudioInputStream audioStream = MaryAdapter.getInstance().text2audio(tts);
				if (audioStream.markSupported()) {
					audioStream.mark(Integer.MAX_VALUE);
				}
				File tmpFile = File.createTempFile("ttsCache", ".wav");
				tmpFile.deleteOnExit();
				AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, tmpFile);
				ttsCache.put(tts, tmpFile.toURI().toURL().toString());
				if (audioStream.markSupported()) {
					audioStream.reset();
					playStream(audioStream, skipQueue);
				} else {
					playTTS(tts, skipQueue);
				}
			} catch (IOException e) {
				e.printStackTrace();
				logger.warn("Couldn't cache TTS for " + tts);
				InputStream audioStream = MaryAdapter.getInstance().text2audio(tts);
				playStream(audioStream, skipQueue);
			}
		}
	}
	
	public void playSilence(int ms, boolean skipQueue) {
		if (skipQueue)
			setStream(new SilenceStream(ms));
		else
			addStream(new SilenceStream(ms));		
	}
	
	public void playStream(InputStream audioStream, boolean skipQueue) {
		if (skipQueue)
			setStream(audioStream);
		else
			addStream(audioStream);		
	}
	
	/* * Stream and Stream Queue handling * */
	
	protected void addStream(InputStream is) {
		logger.info("adding stream to queue: " + is);
		synchronized(this) {
			if (stream != null) {
				if (streamQueue.isEmpty())
					streamQueue.add(is);
			} else {
				setIsTalking();
				stream = is;
			}
		}
	}
	
	public void clearStream() {
		setStream(null);
		setIsSilent();
	}
	
	protected void setStream(InputStream is) {
		logger.debug("playing a new stream " + is);
		synchronized(this) {
			streamQueue.clear();
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			stream = is;
			setIsTalking();
		}
	}
	
	/* * InputStream implementation * */
	
	@Override
	public int read() throws IOException {
		synchronized(this) {
			if (stream == null)  {
				stream = streamQueue.poll();
			}
			int returnValue = (stream != null) ? stream.read() : 0;
			if (returnValue == -1) {
				stream.close();
				stream = null;
				returnValue = 0;
			}
			return returnValue;
		}
	}
	
	private void nextStream() {
		stream = streamQueue.poll();
		if (stream == null) {
			setIsSilent();
		} else {
			logger.info("next stream played is " + stream);
			setIsTalking();
		}
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int bytesRead = 0;
		synchronized(this) {
			if (stream == null) {
				nextStream();
			}
			while (stream != null) {
				bytesRead = stream.read(b, off, len);
				if (bytesRead == -1) {
					nextStream();
				} else break;
			}
			if (bytesRead < len) { // if the stream could not provide enough bytes, then it's probably ended
				if (sendSilence && !inShutdown) { 
					if (bytesRead < 0) 
						bytesRead = 0;
// for silence:
					Arrays.fill(b, off + bytesRead, off + len, (byte) 0);
// for low noise:
//					for (int i = off; i < off + len; i++) {
//						b[i] = (byte) randomNoiseSource.nextInt(2);				
//					}
					bytesRead = len;
				}
				if (stream != null) {
					stream.close();
					stream = null;
				}
			}
		}
		return bytesRead;
	}
	
	private boolean inShutdown = false;
	/** whether this dispatchStream has been requested to shut down */ 
	public boolean inShutdown() { return inShutdown; }
	/** waits for the current stream to finish and shuts down the dispatcher */
	public void shutdown() { inShutdown = true; }
	
}
