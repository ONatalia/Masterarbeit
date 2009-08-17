package org.cocolab.inpro.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	
	HashMap<String,Tier> tiers;
	
	public static TextGrid newFromTextGridFile(String filename) throws IOException {
		return newFromTextGridFile(new File(filename));
	}
	
	public static TextGrid newFromTextGridFile(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		List<String> lines = new LinkedList<String>();
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
		Iterator<String> lineIt = lines.listIterator(8);
		try {
			while (lineIt.hasNext()) {
				String line = lineIt.next();
				if (Pattern.matches("^\\s*item \\[\\d\\]:\\s*$", line)) {
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

	public static void main(String[] args) throws IOException {
		TextGrid tg = newFromTextGridLines(Arrays.asList(
				"File type = \"ooTextFile\"",
				"Object class = \"TextGrid\"",
				"",
				"xmin = 0",
				"xmax = 6.450000",
				"tiers? <exists>",
				"size = 2",
				"    item [1]:",
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
				"        text = \"\"",
				"    item [1]:",
				"    class = \"IntervalTier\"", 
				"    name = \"MAU:\"", 
				"    xmin = 0", 
				"    xmax = 6.450000", 
				"    intervals: size = 5", 
				"    intervals [1]:", 
				"        xmin = 0.000000", 
				"        xmax = 0.550000", 
				"        text = \"<p:>\"", 
				"    intervals [2]:", 
				"        xmin = 0.550000", 
				"        xmax = 0.580000", 
				"        text = \"d\"", 
				"    intervals [3]:", 
				"        xmin = 0.580000", 
				"        xmax = 0.710000", 
				"        text = \"e:\"", 
				"    intervals [4]:", 
				"        xmin = 0.710000", 
				"        xmax = 0.820000", 
				"        text = \"n\"", 
				"    intervals [5]:", 
				"        xmin = 0.820000", 
				"        xmax = 0.910000", 
				"        text = \"<p:>\""));
		System.out.println(tg.toString());
		tg = newFromTextGridFile("/home/timo/inpro/Scripts/mausI/test1.TextGrid");
		System.out.println(tg.toString());
	}

}