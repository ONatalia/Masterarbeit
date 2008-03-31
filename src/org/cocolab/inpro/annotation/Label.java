package org.cocolab.inpro.annotation;

public class Label {
	
	double start;
	double end;
	String label;
	
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

}
