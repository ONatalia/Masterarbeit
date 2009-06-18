/* 
 * Copyright 2007, 2008, 2009, Gabriel Skantze and the Inpro project
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
package org.cocolab.inpro.pitch;

import org.cocolab.inpro.pitch.util.PitchUtils;

public class PitchCandidate {
	
	public int lag = 0;
	public double score = 0;
	public int frame = 0;
	private int samplingFrequency = 0;
	
	public PitchCandidate(int lag, double score, int samplingFrequency) {
		this.lag = lag;
		this.score = score;
		this.samplingFrequency = samplingFrequency;
	}
	
	public PitchCandidate() {}
	
	public double pitchInCent() {
		double d = PitchUtils.hzToCent(((double) samplingFrequency) / lag);
		if (Math.abs(d - 110) < 0.001) {
			System.err.println(110);
		}
		return d;
	}
	
}
