package inpro.annotation;


import static org.junit.Assert.*;

import inpro.annotation.Label;

import org.junit.Test;

public class LabelUnitTest {
	
	private String testString ="biff", 
			testStringSilence="<s>";
	private double start = 0.5, end = 1.5;

	@Test
	public void testLabelDoubleDoubleString() {
		Label label = new Label(start, end, testString);
		assertEquals(label.getLabel(), testString);
		assertTrue(label.getStart() == start);
		assertTrue(label.getEnd() == end);
		assertTrue(label.getDuration() == end-start);
		assertEquals(label.toString(), start+"\t"+end+"\t"+testString);
		assertEquals(label.toMbrola().toString(), testString+" "+(int)((end-start)*1000));
		assertFalse(label.isSilence());
	}
	
	@Test
	public void testLabelString() {
		Label label = new Label(testStringSilence);
		assertEquals(label.getLabel(), testStringSilence);
		assertTrue(Double.isNaN(label.getDuration()));
		assertTrue(Double.isNaN(label.getStart()));
		assertTrue(Double.isNaN(label.getEnd()));
		assertTrue(label.isSilence());
	}

	@Test
	public void testLabelLabel() {
		Label label2 = new Label(start, end, testString);		
		Label label = new Label(label2);
		assertEquals(label.getLabel(), testString);
		assertTrue(label.getStart() == start);
		assertTrue(label.getEnd() == end);
		assertTrue(label.getDuration() == end-start);
		assertEquals(label.getLabel(),label2.getLabel());
	}
}


