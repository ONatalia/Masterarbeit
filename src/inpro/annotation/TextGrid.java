package inpro.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class TextGrid {
	
	private static final List<Pattern> tgPatterns = Arrays.asList( 
		Pattern.compile("^File type = \"ooTextFile\"$"), 
		Pattern.compile("^Object class = \"TextGrid\"$"), 
		Pattern.compile("^$"), 
		Pattern.compile("^\\s*xmin = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*xmax = (\\d*(\\.\\d+)?)\\s*$"), 
		Pattern.compile("^\\s*tiers\\? <exists>\\s*$"), 
		Pattern.compile("^\\s*size = (\\d+)\\s*$") 
	); 
	
	private HashMap<String,Tier> tiers;
	
	public static TextGrid newFromTextGridFile(String filename) throws IOException {
		return newFromTextGridFile(new File(filename));
	}
	
	public static TextGrid newFromTextGridFile(File file) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1"));
		List<String> lines = new ArrayList<String>();
		String line;
		while ((line = in.readLine()) != null)
            lines.add(line);
		in.close();
		return newFromTextGridLines(lines);
	}

	public static TextGrid newFromTextGridLines(List<String> lines) throws IOException {
		assert lines.size() >= 7;
		List<String> params = AnnotationUtil.interpret(lines, tgPatterns);
		int size = Integer.parseInt(params.get(6));
		List<Tier> tiers = new ArrayList<Tier>(size);
		List<String> tierLines = new ArrayList<String>();
		ListIterator<String> lineIt = lines.listIterator(8);
		String line;
		try {
			while (lineIt.hasNext()) {
				line = lineIt.next();
				if ((Pattern.matches("^\\s*item \\[\\d+\\]:\\s*$", line)) && (tierLines.size() > 0)) {
					tiers.add(Tier.newFromTextGridLines(tierLines));
					tierLines = new ArrayList<String>();
				} else {
					tierLines.add(line);
				}
			}
			tiers.add(Tier.newFromTextGridLines(tierLines));
		} catch(IOException ioe) {
			throw ioe;
		} catch(Exception e) {
			throw new IOException(e);
		}
		return new TextGrid(tiers);
	}
	
	@SuppressWarnings("unchecked")
	public static TextGrid newEmptyTextgrid() {
		return new TextGrid(Collections.EMPTY_LIST);
	}

	private TextGrid(List<Tier> tiers) {
		this.tiers = new HashMap<String, Tier>();
		for (Tier tier : tiers) {
			this.tiers.put(tier.name, tier);
		}
	}
	
	public Collection<String> getTierNames() {
		return tiers.keySet();
	}
	
	public Tier getTierByName(String name) {
		return tiers.get(name);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String tierName : getTierNames()) {
			sb.append(tierName);
			sb.append(":\n");
			sb.append(getTierByName(tierName).toString());
		}
		return sb.toString();
	}
}