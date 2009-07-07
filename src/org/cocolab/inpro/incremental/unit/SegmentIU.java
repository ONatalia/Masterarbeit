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
package org.cocolab.inpro.incremental.unit;

import org.cocolab.inpro.annotation.Label;

public class SegmentIU extends IU {
	
	// we keep start time, end time and text of the segment in a label
	Label l;
	
	public SegmentIU(String segment, SegmentIU sll) {
		super(sll);
		this.l = new Label(segment);
	}

	public void updateLabel(Label l) {
		assert (this.l.getLabel().equals(l.getLabel()));
		this.l = l;
	}
	
	@Override
	public double startTime() {
		return l.getStart();
	}
	
	@Override
	public double endTime() {
		return l.getEnd();
	}

	@Override
	public String toOAAString() {
		StringBuffer sb = new StringBuffer(Integer.toString(id));
		sb.append(",'");
		sb.append(l.getLabel());
		sb.append("'");
		return sb.toString();
	}

	@Override
	public String toTEDviewXML() {
		return l.toTEDViewXML();
	}
	
}
