package test.unit.inpro.annotation;

import java.io.IOException;
import java.util.Arrays;

import inpro.annotation.TextGrid;

import org.junit.Test;


public class TextGridUnitTest {

	@Test
	public void testNewEmptyTextgrid() {
		TextGrid.newEmptyTextgrid();
	}

	@Test
	public void testNewFromTextGridLines() throws IOException {
		TextGrid.newFromTextGridLines(Arrays.asList(
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
				"    item [2]:",
				"    class = \"IntervalTier\"", 
				"    name = \"MAU:\"", 
				"    xmin = 0", 
				"    xmax = 0.910000", 
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
	}
}
