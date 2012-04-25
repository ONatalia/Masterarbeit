package org.cocolab.inpro.synthesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * the data model that can be manipulated by VisualTTS.
 * We support mbrola data (i.e. segments and associated lists of pitch marks) 
 * and import from and export to mbrola format.
 * @author timo
 */
public class SegmentModel {

	private final List<Segment> segments;
	/** 
	 * one of the segments may be active, meaning that it can be modified
	 * by moveRightBoundaryOfActiveLabel, moveAllBoundariesRightOfActiveLabel, ...)
	 */
	private Segment activeLabel = null;
	/**
	 * one of the pitchmarks may be active. 
	 * it can then be modified by setPitchOfActiveMarkTo().
	 */
	private PitchMark activePitchMark = null;
	
	SegmentModel(List<Segment> segments) {
		this.segments = segments;
	}
	
	public static SegmentModel createTestModel() {
		List<Segment> segments = new ArrayList<Segment>();
		segments.add(new Segment("_", 0, 100));
		segments.add(new Segment("h", 100, 200));
		segments.add(new Segment("a:", 300, 200));
		segments.add(new Segment("l", 500, 300));
		segments.add(new Segment("o:", 800, 300));
		segments.add(new Segment("_", 1100, 100));
		return new SegmentModel(segments);
	}
	
	/** read from a list of lines, which  */
	public static SegmentModel readFromMbrolaLines(List<String> lines) {
		List<Segment> segments = new ArrayList<Segment>(lines.size());
		int startTime = 0; // in milliseconds
    	for (String line : lines) {
    		if (line.matches("^;")) {
    			continue; // skip commented lines
    		} else if (line.matches("^#")) {
    			break; // break after final symbol
    		}
    		StringTokenizer st = new StringTokenizer(line);
    		assert st.countTokens() >= 2 : "Error parsing line " + line;
    		String labelText = st.nextToken();
    		String durationText = st.nextToken();
    		int duration; // in milliseconds
    		try {
    			duration = Integer.parseInt(durationText);
    		} catch (NumberFormatException nfe) {
    			System.err.println("Error in line " + line);
    			nfe.printStackTrace();
    			throw nfe;
    		}
    		List<PitchMark> pitchMarks = new ArrayList<PitchMark>();
    		while (st.hasMoreTokens()) {
    			String pitchMarkString = st.nextToken();
    			pitchMarks.add(new PitchMark(pitchMarkString));
    		}
    		Segment segmentUnderConstruction = new Segment(labelText, startTime, duration, pitchMarks);
    		segments.add(segmentUnderConstruction);
    		startTime += duration;
    	}
		return new SegmentModel(segments);
	}
	
	public static SegmentModel readFromString(String mbrola) {
		List<String> lines = Arrays.<String>asList(mbrola.split("\n"));
		return readFromMbrolaLines(lines);
	}
	
	/** create a segment model from an mbrola-formatted input stream */
	public static SegmentModel readFromStream(InputStream is) {
		List<String> lines;
		try {
			lines = getLines(is);
			System.err.println(lines);
		} catch (IOException e) {
			e.printStackTrace();
			lines = Collections.<String>emptyList();
		}
		return readFromMbrolaLines(lines);
	}
	
	private static List<String> getLines(InputStream is) throws IOException {
        List<String> list = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = reader.readLine()) != null) {
        	list.add(line);
		}
		reader.close();
        return list;
	}
	
	/** create a segment model from an mbrola-formatted file */
	public static SegmentModel readFromFile(String filename) throws IOException {
		return readFromStream(new FileInputStream(filename));
	}
	
	public void saveToFile(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		fw.append(toString());
		fw.close();
	}
	
	/** overall duration in milliseconds */
	public int getDuration() {
		return segments.get(segments.size() - 1).getEndTime();
	}
	
	public void setActiveLabel(Segment l) {
		activeLabel = l;
	}
	
	public void moveRightBoundaryOfActiveLabelTo(int newTime) {
		final int MINIMUM_DURATION = 10; // milliseconds
		if (activeLabel != null) {
			Segment followingLabel = getSuccessor(activeLabel);
			activeLabel.setRightBoundaryTo(newTime);
			if (followingLabel != null)
				followingLabel.setLeftBoundaryTo(newTime);
			if (activeLabel.duration < MINIMUM_DURATION || 
				(followingLabel != null && followingLabel.duration < MINIMUM_DURATION)) 
				activeLabel = null;
		}
	}
	
	public void moveAllBoundariesRightOfActiveLabel(int newTime) {
		final int MINIMUM_DURATION = 10; // milliseconds
		if (activeLabel != null) {
			int delta = newTime - activeLabel.getEndTime();
			activeLabel.duration += delta;
			int firstIndex = segments.lastIndexOf(activeLabel) + 1;
			for (int i = firstIndex; i < segments.size(); i++) {				
				segments.get(i).startTime += delta;
			}
			if (activeLabel.duration < MINIMUM_DURATION) 
				activeLabel = null;
		}
	}
	
	public void setActivePitchMark(PitchMark pm) {
		activePitchMark = pm;
	}
	
	public void setPitchOfActiveMarkTo(float pitch) {
		if (activePitchMark != null && pitch > 30)
			activePitchMark.setPitch(pitch);
	}
	
	/**  
	 * insert a segment with the given label at a given time.
	 * the place of insertion into the segment list is ensured depending
	 * on whether time t is before or after the time of the segment currently
	 * spanning this time.
	 */
	public void insertSegment(String newSegment, int time) {
		boolean insertAfter;
		Segment currentLabel = getSegmentAt(time);
		if (currentLabel != null) {
			insertAfter = (time - currentLabel.getCenter()) > 0;
		} else { // this must be after the very last segment
			currentLabel = segments.get(segments.size() - 1);
			insertAfter = true;
		}
		int currentIndex = segments.indexOf(currentLabel);
		if (insertAfter) {
			int endTime = currentLabel.getEndTime();
			currentLabel.setRightBoundaryTo(time);
			Segment newLabel = new Segment(newSegment, time, endTime - time);
			segments.add(currentIndex + 1, newLabel);
		} else {
			int startTime = currentLabel.getStartTime();
			currentLabel.setLeftBoundaryTo(time);
			Segment newLabel = new Segment(newSegment, startTime, time - startTime);
			segments.add(currentIndex, newLabel);
		}
	}

	public void removeSegment(Segment l) {
		if (l != null) {
			Segment successor = getSuccessor(l);
			if (successor != null) {
				successor.startTime = l.startTime;
				successor.duration += l.duration;
			}
			segments.remove(l);
		}
	}
	
	public void insertPitchMark(int time, int pitch) {
		Segment l = getSegmentAt(time);
		l.addPitchMark(time, pitch);
	}
	
	public static void remove(SegmentBoundPitchMark pm) {
		if (pm != null) {
			pm.owner.removePitchMark(pm);
		}
	}
	
	public Segment getPredecessor(Segment segment) {
		return getSegmentAt(segment.getStartTime() - 1);
	}
	
	public Segment getSuccessor(Segment segment) {
		return getSegmentAt(segment.getEndTime() + 1);
	}
	
	/** lookup the segment that spans time t (if any) */
	public Segment getSegmentAt(int time) {
		if (time < 0)
			return null;
		for (Segment l : segments) {
			if (l.getStartTime() <= time && l.getEndTime() >= time) 
				return l;
		}
		return null; 
	}
	
	public List<Segment> getSegments() {
		return Collections.unmodifiableList(segments);
	}

	public PitchRange getPitchRange() {
		int min = Integer.MAX_VALUE;
		int max = 1;
		for (Segment l : segments) {
			min = Math.min(l.getMinimumPitch(), min);
			max = Math.max(l.getMaximumPitch(), max);
		}
		if (min == Integer.MAX_VALUE) {
			min = 0;
		}
		if (max - min < 10) {
			max += 10;
		}
		return new PitchRange(min, max);
	}

	/** return the segmentModel in mbrola format */ 
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Segment segment : segments) {
			sb.append(segment.toString());
		}
		return sb.toString();
	}
	
	/**
	 * A segment of speech, usually a phone/phoneme.
	 * Segments have a label, a start and duration, 
	 * and may contain zero or more pitch marks
	 */
	public static class Segment {
		private String label;
		private int startTime;
		private int duration;
		private List<SegmentBoundPitchMark> pitchMarks;

		public Segment(String label, int startTime, int duration, List<PitchMark> pms) {
			this.label = label;
			this.startTime = startTime;
			this.duration = duration;
			this.pitchMarks = new ArrayList<SegmentBoundPitchMark>(pms.size());
			for (PitchMark pm : pms) {
				pitchMarks.add(new SegmentBoundPitchMark(pm, this));
			}
		}
		
		public Segment(String labelText, int startTime, int duration) {
			this(labelText, startTime, duration, Collections.<PitchMark>emptyList());
		}
		
		public String getText() {
			return label;
		}
		
		public int getStartTime() {
			return startTime;
		}
		
		public int getEndTime() {
			return startTime + duration;
		}
		
		public int getCenter() {
			return (startTime + duration / 2);
		}
		
		public int getDuration() {
			return duration;
		}
		
		/** set the left boundary, leaving the right boundary alone (i.e. adapting duration) */
		private void setLeftBoundaryTo(int startTime) {
			int endTime = this.startTime + duration; 
			this.startTime = startTime;
			duration = endTime - startTime;
		}
		
		/** set the right boundary, leaving the left boundary alone (i.e. change duration) */
		private void setRightBoundaryTo(int endTime) {
			duration = endTime - startTime;
		}
		
		public void setText(String labelText) {
			this.label = labelText;
		}

		/** return an unmodifiable view of the pitch marks */
		public List<SegmentBoundPitchMark> getPitchMarks() {
			return Collections.unmodifiableList(pitchMarks);
		}
		
		public void removePitchMark(PitchMark pm) {
			assert pitchMarks.contains(pm);
			pitchMarks.remove(pm);
		}
		
		public void addPitchMark(int time, int pitch) {
			SegmentBoundPitchMark newpm = new SegmentBoundPitchMark(time, pitch, this);
			int index = 0;
			for (SegmentBoundPitchMark pm : pitchMarks) {
				if (pm.position > newpm.position) {
					break;
				}
				index++;
			}
			pitchMarks.add(index, newpm);
		}
		
		public int getMinimumPitch() {
			int min = Integer.MAX_VALUE;
			for (PitchMark pm : pitchMarks) {
				min = Math.min(pm.getPitch(), min);
			}
			return min;
		}

		public int getMaximumPitch() {
			int max = Integer.MIN_VALUE;
			for (PitchMark pm : pitchMarks) {
				max = Math.max(pm.getPitch(), max);
			}
			return max;
		}
		
		/** return the segment and its pitchmarks in mbrola format */
		public String toString() {
			StringBuilder sb = new StringBuilder(label);
			sb.append(" ");
			sb.append(duration);
			for (PitchMark pm : pitchMarks) {
				sb.append(" ");
				sb.append(pm);
			}
			sb.append("\n");
			return sb.toString();
		}
	}
	
	public static class SegmentBoundPitchMark extends PitchMark {
		Segment owner;
		
		public SegmentBoundPitchMark(PitchMark pm, Segment owner) {
			super(pm.position, pm.pitch);
			this.owner = owner;
		}
		
		public SegmentBoundPitchMark(int time, int pitch, Segment owner) {
			super((time - owner.startTime) / (float) owner.duration, pitch);
			this.owner = owner;
		}
		
		/** returns the time of this pitch mark given the owning label */
		public int getTime() {
			return getTime(owner.startTime, owner.duration);
		}

	}
	
	/**
	 * PitchRange is a simple container for a minimum and maximum pitch.
	 */
	public static final class PitchRange {
		final int min;
		final int max;
		
		PitchRange(int min, int max) {
			this.min = min;
			this.max = max;
		}
		
		public int getMin() {
			return min;
		}
		
		public int range() {
			return (max - min);
		}
	}

}
