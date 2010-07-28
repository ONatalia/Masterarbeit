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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Label {
	
	public static final Set<String> SILENCE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"<sil>", "SIL", "<p:>", "<s>", "</s>", "", "_")));

	private final double start; // in seconds
	private final double end; // in seconds
	private final String label;
	
	private static List<Pattern> tgPatterns = Arrays.asList( 
		Pattern.compile("^\\s*xmin = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*xmax = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*text = \"(.*)\"\\s*$") 
	); 
	
	static Label newFromTextGridLines(List<String> lines) throws IOException {
		assert lines.size() == 3;
		List<String> params = AnnotationUtil.interpret(lines, tgPatterns);
		return new Label(Double.parseDouble(params.get(0)), 
						 Double.parseDouble(params.get(1)), 
						 params.get(2));
	}
	
	public Label(String l) {
		this(Double.NaN, Double.NaN, l);
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
		StringBuilder sb = new StringBuilder();
		sb.append(start);
		sb.append("\t");
		sb.append(end);
		sb.append("\t");
		sb.append(label);
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException {
		Label l = newFromTextGridLines(Arrays.asList(
				"        xmin = 0.910000", 
				"        xmax = 1.970000", 
				"        text = \"Quader\""));
		System.out.println(l.toString());
	}

	public boolean isSilence() {
		return SILENCE.contains(getLabel()); 
	}

}
