/* 
 * Copyright 2008, 2009, Timo Baumann and the Inpro project
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
package org.cocolab.inpro.annotation;

public class Label {
	
	double start; // in seconds
	double end; // in seconds
	String label;
	
	public Label(String l) {
		label = l;
	}
	
	public Label(double s, double e, String l) {
		start = s;
		end = e;
		label = l;
	}

	public double getStart() {
		return start;
	}
	
	public double getEnd() {
		return end;
	}

	public String getLabel() {
		return label;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(start);
		sb.append("\t");
		sb.append(end);
		sb.append("\t");
		sb.append(label);
		return sb.toString();
	}
	
	public String toTEDViewXML() {
		StringBuffer sb = new StringBuffer("<event time='");
		sb.append(Math.round(start * 1000.0));
		sb.append("' duration='");
		sb.append(Math.round((end - start) * 1000.0));
		sb.append("'> ");
		sb.append(label.replace("<", "&lt;").replace(">", "&gt;"));
		sb.append(" </event>");
		return sb.toString();
	}

}
