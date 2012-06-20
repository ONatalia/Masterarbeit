package test.unit.inpro.annotation;


import static org.junit.Assert.*;
import inpro.annotation.Label;


import org.junit.Test;

public class LabelUnitTest {

	@Test
	public void testLabelDoubleDoubleString() {
		Label label = new Label(0.5, 1.5, "biff");
		assertEquals(label.getLabel(), "biff");
		assertTrue(label.getStart() == 0.5);
		assertTrue(label.getEnd() == 1.5);
		assertTrue(label.getDuration() == 1.0);
		assertEquals(label.toString(), "0.5	1.5	biff");
		assertEquals(label.toMbrola().toString(), "biff 1000");
	}

	@Test
	public void testLabelString() {
		Label label = new Label("<s>");
		assertEquals(label.getLabel(), "<s>");
		assertTrue(Double.isNaN(label.getDuration()));
		assertTrue(label.isSilence());
	}

	@Test
	public void testLabelLabel() {
		Label label2 = new Label(0.5, 1.5, "biff");		
		Label label = new Label(label2);
		assertEquals(label.getLabel(), "biff");
		assertTrue(label.getStart() == 0.5);
		assertTrue(label.getEnd() == 1.5);
		assertTrue(label.getDuration() == 1.0);		
	}

}
