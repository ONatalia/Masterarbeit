package inpro.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class Tier extends ArrayList<Label> {

	protected String name;
	
	private static List<Pattern> tgPatterns = Arrays.asList( 
		Pattern.compile("^\\s*class = \"IntervalTier\"\\s*$"), 
		Pattern.compile("^\\s*name = \"(.*)\"\\s*$"), 
		Pattern.compile("^\\s*xmin = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*xmax = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*intervals: size = (\\d+)\\s*$") 
	);
	
	static Tier newFromTextGridLines(List<String> lines) throws IOException {
		assert lines.size() >= 5 : lines;
		if (lines.get(0).equals("    item [1]:")) {
			lines.remove(0);
		}
		//assert (lines.size() - 5) % 4 == 0 : lines;
		List<String> params = AnnotationUtil.interpret(lines, tgPatterns);
		String name = params.get(1);
		int size = Integer.parseInt(params.get(4));
		List<Label> labels = new ArrayList<Label>(size);
		try {
			for (int i = 0; i < size; i++) {
				List<String> subList = lines.subList(5 + i * 4 + 1, 5 + i * 4 + 4); 
				Label l = Label.newFromTextGridLines(subList);
				labels.add(l);
			}
		} catch(IOException ioe) {
			throw ioe;
		} catch(Exception e) {
			throw new IOException(e);
		}
		return new Tier(name, labels);
	}
	
	private Tier(String name, List<Label> labels) {
		super(labels);
		this.name = name;
	}
	
	public Tier getSpan(double start, double end) {
		assert (start <= end);
		int startIndex = 0;
		int endIndex = 0;
		for (int i = 0; i < this.size(); i++) {
			Label l = this.get(i);
			if (l.getStart() - 0.0001 <= start) {
				startIndex = i;
			}
			if (l.getEnd() - 0.0001 <= end) {
				endIndex = i + 1;
			} else {
				break;
			}
		}
		assert (startIndex < endIndex) : start + " " + end;
		return new Tier(this.name, subList(startIndex, endIndex));
	}
	
	public Tier getSpan(Label l) {
		return getSpan(l.getStart(), l.getEnd());
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Label l : this) {
			sb.append(l);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException {
		Tier t = newFromTextGridLines(Arrays.asList(
				"    class = \"IntervalTier\"", 
				"    name = \"ORT:\"", 
				"    xmin = 0", 
				"    xmax = 0.910000", 
				"    intervals: size = 3", 
				"    intervals [1]:", 
				"        xmin = 0.000000", 
				"        xmax = 0.550000", 
				"        text = \"\"", 
				"    intervals [2]:", 
				"        xmin = 0.550000", 
				"        xmax = 0.820000", 
				"        text = \"den\"", 
				"    intervals [3]:", 
				"        xmin = 0.820000", 
				"        xmax = 0.910000", 
				"        text = \"\""));
		System.out.println(t.toString());
		System.out.println(t.getSpan(0.55, 0.82).toString());
	}

}
