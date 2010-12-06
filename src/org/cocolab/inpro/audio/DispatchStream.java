package org.cocolab.inpro.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import javax.sound.sampled.AudioInputStream;

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
 * Exception in thread "dispatcher object source" java.lang.ArrayIndexOutOfBoundsException: Array index out of range: -1
	at java.util.Arrays.rangeCheck(Arrays.java:1309)
	at java.util.Arrays.fill(Arrays.java:2567)
	at org.cocolab.inpro.audio.DispatchStream.read(DispatchStream.java:164)
	at org.cocolab.inpro.apps.SimpleMonitor$2.run(SimpleMonitor.java:206)
	at java.lang.Thread.run(Thread.java:662)

-> the problem occurs, when bytesRead was -1, which - I believe - can happen
if an audio stream has a duration in a multiple of 160 bytes.
i hope this has been fixed, please tell me when you still see this error. (timo) 

Exception in thread "dispatcher object source" java.lang.ArrayIndexOutOfBoundsException: Array index out of range: -1
	at java.util.Arrays.rangeCheck(Arrays.java:1309)
	at java.util.Arrays.fill(Arrays.java:2567)
	at org.cocolab.inpro.audio.DispatchStream.read(DispatchStream.java:176)
	at org.cocolab.inpro.apps.SimpleMonitor$2.run(SimpleMonitor.java:206)
	at java.lang.Thread.run(Thread.java:662)


 * TODO: would it be nice to directly output SysInstallmentIUs?
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

	InputStream stream;
	Queue<InputStream> streamQueue = new ArrayDeque<InputStream>();
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		sendSilence(ps.getBoolean(PROP_SEND_SILENCE));
		ssv = (SpeechStateVisualizer) ps.getComponent(PROP_SPEECH_STATE_VISUALIZER);
	}

	public void initialize() { }
	
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
		InputStream audioStream = MaryAdapter.getInstance().text2audio(tts);
		playStream(audioStream, skipQueue);
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
				if (sendSilence) { 
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

}
