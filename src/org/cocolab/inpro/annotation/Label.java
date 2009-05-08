package org.cocolab.inpro.annotation;

public class Label {
	
	double start; // in seconds
	double end; // in seconds
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%.6f", start));
		sb.append("\t");
		sb.append(String.format("%.6f", end));
		sb.append("\t");
		sb.append(label);
		sb.append("\n");
		return sb.toString();
	}
	
	public String toTEDViewXML() {
		StringBuffer sb = new StringBuffer("<event time='");
		sb.append(Math.round(start * 1000.0));
		sb.append("' duration='");
		sb.append(Math.round((end - start) * 1000.0));
		sb.append("'> ");
		sb.append(label);
		sb.append(" </event>");
		return sb.toString();
	}

}
