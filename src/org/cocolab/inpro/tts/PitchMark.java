package org.cocolab.inpro.tts;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a PitchMark
 */
public class PitchMark {
	final protected double position; // as a percentage
	protected double pitch;

	protected PitchMark(double position, double pitch) {
		this.position = position;
		this.pitch = pitch;
	}
	
	/** create a pitchMark from an mbrola pitchmark-string */
	public PitchMark(String pitchMarkString) {
		assert pitchMarkString.matches("\\((\\d+),(\\d+)\\)") : pitchMarkString;
		Pattern format = Pattern.compile("\\((\\d+),(\\d+)\\)");
		Matcher m = format.matcher(pitchMarkString);
		if (!m.matches()) { // we have to match, whether assertions are enabled or not
			assert false; 
		}
		assert m.groupCount() == 2;
		position = (new Scanner(m.group(1))).nextDouble() * 0.01;
		pitch = (new Scanner(m.group(2))).nextDouble();
	}
	
	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public String toString() {
		return "(" + ((int) (position * 100)) + "," + ((int) pitch) + ")"; 
	}
	
	/** returns the time of this pitch mark given the label's boundaries */
	public int getTime(int startTime, int duration) {
		return (int) (startTime + position * duration);
	}
	
	public int getPitch() {
		return (int) pitch;
	}
}