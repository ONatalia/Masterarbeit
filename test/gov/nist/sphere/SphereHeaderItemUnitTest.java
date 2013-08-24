package gov.nist.sphere;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import gov.nist.sphere.SphereHeaderItem;

public class SphereHeaderItemUnitTest {

	private String testString ="biff",
			i="5", j="3.14E5F";
	private float f=3.14E5F;
	private static SphereHeaderItem ihi;
	private static SphereHeaderItem ihi1;
	private static SphereHeaderItem shi;
	private static SphereHeaderItem fhi;
	
	@Test
	public void testCreateHeaderItemForType() {
		ihi=SphereHeaderItem.createHeaderItemForType("i", testString, i);
		ihi1=SphereHeaderItem.createHeaderItemForType("i", testString, "+"+i);
		shi=SphereHeaderItem.createHeaderItemForType("s5", testString, i);
		fhi=SphereHeaderItem.createHeaderItemForType("r", testString, j);
	}

	@Test
	public void testGetName() {
		assertEquals(ihi.getName(),testString);	
		assertEquals(ihi1.getName(),testString);	
		assertEquals(shi.getName(),testString);	
		assertEquals(fhi.getName(),testString);	
	}

	@Test
	public void testGetValueAsString() {
		assertEquals(ihi.getValueAsString(),""+i);	
		assertEquals(ihi1.getValueAsString(),""+i);	
		assertEquals(shi.getValueAsString(),""+i);	
		assertEquals(fhi.getValueAsString(),""+f);	
	}

	@Test
	public void testGetValueAsObject() {
		assertEquals(ihi.getValueAsObject(),Integer.parseInt(i));	
		assertTrue(ihi.getValueAsObject()instanceof Object);
		assertNotNull(ihi.getValueAsObject());
		assertEquals(ihi1.getValueAsObject(),Integer.parseInt(i));	
		assertTrue(ihi1.getValueAsObject()instanceof Object);
		assertNotNull(ihi1.getValueAsObject());
		assertEquals(shi.getValueAsObject(),i);	
		assertTrue(shi.getValueAsObject()instanceof Object);
		assertNotNull(shi.getValueAsObject());
		assertEquals(fhi.getValueAsObject(),f);	
		assertTrue(fhi.getValueAsObject()instanceof Object);
		assertNotNull(fhi.getValueAsObject());
		
	}

	@Test
	public void testToString() {
		assertEquals(ihi.toString(),testString+" "+ihi.getValueAsString());
		assertEquals(ihi1.toString(),testString+" "+ihi1.getValueAsString());
		assertEquals(shi.toString(),testString+" "+shi.getValueAsString());
		assertEquals(fhi.toString(),testString+" "+fhi.getValueAsString());
	}

	@Test
	public void testSphereHeaderItem() {
		//SphereHeaderItem is private
	}

	@Test
	public void testGetType() {
		assertEquals(ihi.getType(),0);
		assertEquals(ihi1.getType(),0);
		assertEquals(fhi.getType(),1);
		assertEquals(shi.getType(),2);
	}

}
