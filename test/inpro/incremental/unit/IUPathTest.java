package inpro.incremental.unit;

import static org.junit.Assert.*;

import org.junit.Test;

public class IUPathTest {

	static SysInstallmentIU instIU = new SysInstallmentIU("eins zwei drei vier");
	
	@Test
	public void testArgFormatting() {
		// test that bogus commands are caught
		boolean exceptionThrown = false;
		try {
			instIU.getFromNetwork("nextup");
		} catch(IllegalArgumentException iae) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		// test that wrong kinds of arguments are caught
		exceptionThrown = false;
		try {
			instIU.getFromNetwork("up(1)");
		} catch(IllegalArgumentException iae) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		// test that only numbers are allowed as arguments
		exceptionThrown = false;
		try {
			instIU.getFromNetwork("up[qwe123]");
		} catch(IllegalArgumentException iae) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}
	
	@Test
	public void testGetFromNetwork() {
		// there's nothing above a newly created installment:
		assertNull(instIU.getFromNetwork("up"));
		
		assertSame(instIU.getFromNetwork("down", "next", "back"), instIU.getFromNetwork("down"));
		assertSame(instIU.getFromNetwork("down", "next[-1]", "back[0]"), instIU.getFromNetwork("down"));
		assertSame(instIU.getFromNetwork("down", "down", "down"), instIU.getSegments().get(0));
		//TODO: this is a problem with SysInstallmentIUs: they point downwards to words, but the words point up to chunks...
		//assertSame(instIU.getFromNetwork("down", "down", "up", "up"), instIU);
		assertSame(instIU.getFromNetwork("down", "down", "up"), instIU.getFromNetwork("down"));
		// the last segment of the last syllable of the first word should be the same as the segment preceding the second word's first 
		assertSame(instIU.getFromNetwork("down", "next", "down", "down", "back"), instIU.getFromNetwork("down", "down[-1]", "down[-1]"));
		assertSame(instIU.getFromNetwork("down[1]", "down", "down", "back"), instIU.getFromNetwork("down", "down[-1]", "down[-1]"));
	}

}
