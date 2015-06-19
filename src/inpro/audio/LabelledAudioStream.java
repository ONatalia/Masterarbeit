package inpro.audio;

import inpro.annotation.Label;
import inpro.annotation.LabelFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * an audiostream that comes with a labelFile (i.e., TextGrid) and notifies listeners 
 * when labels in the labelfile are passed by reading the audiostream
 * TODO: listener-functionality needs a test
 */
public class LabelledAudioStream extends AudioInputStream {

	Queue<Label> labels;
	
	private long positionInBytes = 0;
	private long labelStartPosition = 0;
	private long labelEndPosition = 0;
	
	private boolean labelHasStarted = false;
	
	private float bytesPerSecond = 0;
	
	// NOTE TO SELF: extend to have several channels and something like Map<String, List<Listener>>
	// addListener(l) will have to be changed to addListener(l, channel)
	List<Listener> listeners;
	
	/**
	 * create a new labelled audio object from the two files
	 * fileBasename.wav and fileBasename.lab
	 */
	public LabelledAudioStream(String fileURL) throws MalformedURLException, UnsupportedAudioFileException, IOException {
		this(fileURL, fileURL.replaceAll("\\....$", ".lab"));
	}
	
	/** create a new labelled audio object from the two files given */
	public LabelledAudioStream(String fileURL, String labelURL) throws MalformedURLException, UnsupportedAudioFileException, IOException {
		super(AudioUtils.getAudioStreamForURL(new URL(fileURL)), 
			  AudioUtils.getAudioStreamForURL(new URL(fileURL)).getFormat(), 
			  AudioUtils.getAudioStreamForURL(new URL(fileURL)).getFrameLength());
		labels = LabelFile.getLabels(new URL(labelURL).getFile());
		bytesPerSecond = getFormat().getSampleRate() * getFormat().getSampleSizeInBits() / 8;
		listeners = new ArrayList<Listener>(1);
		nextLabel();
	}
	
	@Override
	public int available() throws IOException {
		return 160;
	}
	
	@Override
	public int read(byte[] data) throws IOException {
		int bytesRead = super.read(data);
		positionInBytes += bytesRead;
		if (positionInBytes >= labelEndPosition) {
			labelEnd(currentLabel());
			nextLabel(); // update book keeping
		}
		if (!labelHasStarted && positionInBytes >= labelStartPosition) {
			labelStart(currentLabel());
			labelHasStarted = true;
		}
		return bytesRead;
	}
	
	/** return the current label (or null if their is none at the moment) */
	public Label currentLabel() {
		return labelHasStarted ? labels.peek() : null;
	}
	
	/** advance to the next label and update bookkeeping */
	private void nextLabel() {
		labels.poll(); // remove the label that has just ended
		if (labels.size() > 0) {
			labelStartPosition = Math.round(currentLabel().getStart() * bytesPerSecond);
			labelEndPosition = Math.round(currentLabel().getEnd() * bytesPerSecond);
		}
		labelHasStarted = false;
	
	}
	
	/** what to do when a label starts */
	private void labelStart(Label l) {
		for (Listener listener : listeners)
			listener.labelStarts(l);
	}
	
	
	/** what to do when a label ends */
	private void labelEnd(Label l) {
		for (Listener listener : listeners)
			listener.labelEnds(l);
	}
	
	/** add a label Listener to this stream */ 
	public void addListener(Listener l) {
		listeners.add(l);
	}
	
	/** interface to implement if you're interested in receiving label events */
	public interface Listener {
		/** will be called on the start of a label */
		public void labelStarts(Label l);
		/** wlil be called when the label has ended */
		public void labelEnds(Label l);
	}
	
}
