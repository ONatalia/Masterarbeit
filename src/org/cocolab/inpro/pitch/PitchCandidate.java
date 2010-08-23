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
	
	private double score = 0;
	public int frame = 0;
	private double pitchHz = Double.NaN;
	
	public PitchCandidate() {}
	
	public PitchCandidate(double pitchHz, double score) {
		this.pitchHz = pitchHz;
		this.score = score;
	}
	
	public double getScore() {
		return this.score;
	}
	
	public double pitchInHz() {
		return pitchHz;
	}
	
	public double pitchInCent() {
		return PitchUtils.hzToCent(pitchHz);
	}
	
}
