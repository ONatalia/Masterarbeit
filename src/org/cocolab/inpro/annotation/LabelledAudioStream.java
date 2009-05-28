package org.cocolab.inpro.annotation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;

public class LabelledAudioStream extends AudioInputStream {

	LinkedList<Label> labels;
	
	long positionInBytes = 0;
	long labelEndPosition = 0;

	boolean newLabelFlag = true;
	
	float bytesPerSecond = 0;
	
	/**
	 * create a new labelled audio object from the two files
	 * fileBasename.wav and fileBasename.lab
	 * @throws IOException 
	 * @throws UnsupportedAudioFileException 
	 * @throws MalformedURLException 
	 **/
	public LabelledAudioStream(String fileURL) throws MalformedURLException, UnsupportedAudioFileException, IOException {
		super(AudioUtils.getAudioStreamForURL(new URL(fileURL)), 
			  AudioUtils.getAudioStreamForURL(new URL(fileURL)).getFormat(), 
			  AudioUtils.getAudioStreamForURL(new URL(fileURL)).getFrameLength());
		String labelFileURL = fileURL.replaceAll("\\....$", ".lab");
		labels = LabelFile.getLabels(new URL(labelFileURL).getFile());
		bytesPerSecond = getFormat().getSampleRate() * getFormat().getSampleSizeInBits() / 8;
		labelEndPosition = Math.round(labels.peek().getEnd() * bytesPerSecond);
	}
	
	public String currentLabel() {
		return labels.peek().label;
	}
	
	public double remainingTimeForLabel() {
		return (labelEndPosition - positionInBytes) / bytesPerSecond;
	}
	
	public boolean hasNewLabel() {
		if (newLabelFlag) {
			newLabelFlag = false;
			return true;
		}
		return false;
	}
	
	public int available() throws IOException {
		return 160;
	}
	
	public int read(byte[] data) throws IOException {
		int bytesRead = super.read(data); // 10ms 
		positionInBytes += bytesRead;
		if (positionInBytes >= labelEndPosition) {
			labels.poll();
			newLabelFlag = true;
			labelEndPosition = Math.round(labels.peek().getEnd() * bytesPerSecond);
		}
		return bytesRead;
	}
	
}
