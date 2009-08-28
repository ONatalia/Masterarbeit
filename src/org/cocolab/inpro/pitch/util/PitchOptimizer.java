/* 
 * Copyright 2007, 2008, Gabriel Skantze and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

package org.cocolab.inpro.pitch.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.pitch.PitchCandidate;

public class PitchOptimizer extends ShortestPath<PitchCandidate> {
	
    private ArrayList<List<PitchCandidate>> candidateList = new ArrayList<List<PitchCandidate>>();
    private PitchCandidate start = new PitchCandidate();
    private List<PitchCandidate> lastCandidates = null;
    private int numFrames = 0;
    int lookback; // the list of pitch candidates never grows larger than this many entries. 
    			  // pitch is -- after all -- a local phenomenon and the decision should hardly
                  // change if we look back more than (by default) 10 s.
	
    public PitchOptimizer (int lookback) {
    	init();
    	this.lookback = lookback;
    }
    
    public PitchOptimizer() {
    	this(1000);
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
					c.frame = numFrames - 1;
					double cost = c.getScore() * 1000;
					connect(start, c, cost);
	    		}
			} else {
				for (PitchCandidate c1 : lastCandidates) {
					for (PitchCandidate c2 : candidates) {
						c2.frame = numFrames-1;
						double framedist = (c2.frame - c1.frame);
						double centdist = Math.abs(c2.pitchInCent() - c1.pitchInCent());
						double cost = (centdist / framedist) + c2.getScore() * 1000;
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

			int pathsize = path.size();
			double[] values = new double[pathsize];
			int i = 0;
			for (PitchCandidate pc : path) {
				values[i] = pc.pitchInCent();
				i++;
			}
			//double[] valuesSmoothed = medianSmooth(values, 5);
			
			Iterator<PitchCandidate> pathIt = path.iterator();
			PitchCandidate candidate = pathIt.next(); 
			i = 0;
			for (int frame = 0; frame < numFrames; frame++) {
				if (candidate.frame == frame) {
					if (pathIt.hasNext()) {
						candidate = pathIt.next();
					}
					//pitchList.add(valuesSmoothed[i]);
					pitchList.add(values[i]);
					voicingList.add(true);
					if (i < pathsize - 1) i++;
				} else {
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

