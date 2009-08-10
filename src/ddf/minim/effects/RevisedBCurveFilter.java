/* 
 * Copyright 2009, Timo Baumann and the Inpro project
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
package ddf.minim.effects;

public class RevisedBCurveFilter extends IIRFilter {

	public RevisedBCurveFilter() {
		// this only works with 16000 sampled data
		super(1, 16000);
	}

	@Override
	protected void calcCoeff() {
		b = new double[3];
		b[0] = 1.0d;
		b[1] = -2.0d;
		b[2] = 1.0d;
		a = new double[2];
		a[0] = 1.970290358083393d;
		a[1] = -0.970725276153173d;
	}

}
