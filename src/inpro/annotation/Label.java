package inpro.annotation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Label {
	
	/** set of predefined labels that are to be understood as silence */
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
	
	/** factory for labels from textgrid lines */
	static Label newFromTextGridLines(List<String> lines) throws IOException {
		assert lines.size() == 3;
		List<String> params = AnnotationUtil.interpret(lines, tgPatterns);
		return new Label(Double.parseDouble(params.get(0)), 
						 Double.parseDouble(params.get(1)), 
						 params.get(2));
	}

	/** construct a label from given start and end times, with the given label text */ 
	public Label(double s, double e, String l) {
		start = s;
		end = e;
		label = l;
	}

	/** construct a label with start and end set to NaN */
	public Label(String l) {
		this(Double.NaN, Double.NaN, l);
	}
	/** construct a label from another label */
	public Label(Label l) {
		this(l.getStart(), l.getEnd(), l.getLabel());
	}
		
	/** in seconds */
	public double getStart() {
		return start;
	}
	
	/** in seconds */
	public double getEnd() {
		return end;
	}
	
	/** in seconds */
	public double getDuration() {
		return end - start;
	}

	/** the label itself */
	public String getLabel() {
		return label;
	}

	/** whether the label appears to mark a silence */
	public boolean isSilence() {
		return SILENCE.contains(getLabel()); 
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
	
	public StringBuilder toMbrola() {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		sb.append(" ");
		sb.append((int) ((end - start) * 1000));
		return sb;
	}

	public static void main(String[] args) throws IOException {
		Label l = newFromTextGridLines(Arrays.asList(
				"        xmin = 0.910000", 
				"        xmax = 1.970000", 
				"        text = \"Quader\""));
		System.out.println(l.toString());
	}

}
