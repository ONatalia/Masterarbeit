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
