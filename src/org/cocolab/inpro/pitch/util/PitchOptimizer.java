package org.cocolab.inpro.pitch.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cocolab.inpro.pitch.PitchCandidate;

public class PitchOptimizer extends ShortestPath<PitchCandidate> {
	
    private ArrayList<List<PitchCandidate>> candidateList = new ArrayList<List<PitchCandidate>>();
    private PitchCandidate start = new PitchCandidate();
    private List<PitchCandidate> lastCandidates = null;
    private int numFrames = 0;
	
    public PitchOptimizer () {
    	init();
    }
    
	public void init() {
		clear();
		numFrames = 0;
		lastCandidates = null;
		candidateList.clear();
		setStart(start);
	}
	
	public void addCandidates(List<PitchCandidate> candidates) {
		numFrames++;
		if (candidates != null && !candidates.isEmpty()) {
			candidateList.add(candidates);
			if (lastCandidates == null) {
				for (PitchCandidate c : candidates) {
					c.frame = numFrames-1;
					double cost = c.score * 1000;
					connect(start, c, cost);
	    		}
			} else {
				for (PitchCandidate c1 : lastCandidates) {
					for (PitchCandidate c2 : candidates) {
						c2.frame = numFrames-1;
						double framedist = (c2.frame - c1.frame);
						double centdist = Math.abs(c2.pitchInCent() - c1.pitchInCent());
						double cost = (centdist / framedist) + c2.score * 1000;
						connect(c1, c2, cost);
	    			}
				}
			}
			lastCandidates = candidates;

			//for (PitchCandidate c : candidates) {
				//System.out.print(String.format(Locale.US, "%.2f#%.2f ", c.pitchHz, c.score));
			//}
			//System.out.println("");
		}
	}
	
	/*
	 * List<Double> pitchList pitch is measured in Cent
	 */
	public void optimize(List<Boolean> voicingList, List<Double> pitchList) {
		pitchList.clear();
		voicingList.clear();

		if (lastCandidates == null) {
			for (int frame = 0; frame < numFrames; frame++) {
				voicingList.add(false);
				pitchList.add(0.0);
			}
		} else {

			PitchCandidate target = new PitchCandidate();
			setTarget(target);

			for (PitchCandidate c : lastCandidates) {
				connect(c,target,0);
			}

			List<PitchCandidate> path = super.calculate();
			// Remove the start and target nodes
			path.remove(0);
			path.remove(path.size()-1);

			double[] values = new double[path.size()]; 
			for (int i = 0; i < values.length; i++) {
				values[i] = path.get(i).pitchInCent();
			}
			double [] valuesSmoothed = medianSmooth(values, 5);
			
			int i = 0;
			for (int frame = 0; frame < numFrames; frame++) {
				PitchCandidate candidate = path.get(i);
				if (candidate.frame == frame) {
					//System.out.println(ResultData.pitchCentToHz(valuesSmoothed[i]));
					pitchList.add(valuesSmoothed[i]);
					voicingList.add(true);
					if (i < path.size() - 1) i++;
				} else {
					//System.out.println("");
					pitchList.add(0.0);
					voicingList.add(false);
				}
			}
		}
	}
	
	private double[] medianSmooth(double[] values, int windowsize) {
		assert (windowsize >= 3);
		assert (windowsize % 2 == 1);
		int tailsize = windowsize / 2;
		double[] result = new double[values.length];		
		for (int i = 0; i < values.length; i++) {
			int bufferstart = i - tailsize;
			if (bufferstart < 0) bufferstart = 0;
			int bufferend = i + tailsize + 1;
			if (bufferend > values.length) bufferend = values.length;
			int bufferlength = bufferend - bufferstart;
			double[] buffer = Arrays.copyOfRange(values, bufferstart, bufferend);
			Arrays.sort(buffer);
			if (bufferlength % 2 == 0) {
				result[i] = (buffer[bufferlength / 2] + buffer[bufferlength / 2 - 1]) / 2;
			} else {
				result[i] = buffer[bufferlength / 2];
			}
		}
		return result;
	}
	
}

