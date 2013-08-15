package test.unit.inpro.annotation;



import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

public class AnnotationUtilUnitTest {

	@Test
	public void testInterpret() {
		
		/*
		 * Set up params to be sent into the method
		 */
		List<Pattern> tgPatterns = Arrays.asList( 
				Pattern.compile("^\\s*xmax = (\\d*(\\.\\d+)?)\\s*$")
			); 
		String line = "xmax = 1.300090702947846\n";
		List<String> lines = new ArrayList<String>();
		lines.add(line);

		/*
		 * Set up the invocation for a private static method
		 */
		try {
			Class<?>[] argtypes = new Class[2];
			argtypes[0] = List.class;
			argtypes[1] = List.class;
			Method m = inpro.annotation.AnnotationUtil.class.getDeclaredMethod("interpret", argtypes);
			m.setAccessible(true); //when the method is not public, set this to true
			assertEquals("AnnotationUtil.interpret", m.invoke(null, lines, tgPatterns).toString(), "[1.300090702947846]"); //use null if method is static
		} 
		catch (NoSuchMethodException e) {
			fail("Method not implemented.");
		} 
		catch (SecurityException e) {
			fail("SecurityException.");
		} 
		catch (IllegalAccessException e) {
			fail("IllegalAccessException");
		} 
		catch (IllegalArgumentException e) {
			fail("IllegalArgumentException");
		} 
		catch (InvocationTargetException e) {
			fail("InvocationTargetException");
		}
	}

}
