package test.unit.gov.nist.sphere;

import static org.junit.Assert.*;
import gov.nist.sphere.SphereException;


import org.junit.Test;

public class SphereExceptionUnitTest {

private String testString ="Ein String";
	
	@Test
	public void testSphereException(){							
		SphereException sphereexception = new SphereException();
		assertEquals(sphereexception.toString(),"gov.nist.sphere.SphereException");
	}
	
	@Test
	public void testSphereExceptionMessage(){					
		SphereException sphereexception = new SphereException(testString);
		assertEquals(sphereexception.toString(),"gov.nist.sphere.SphereException: "+testString);
	}
}