package inpro.io.instantio;

import junit.framework.Assert;

import org.instantreality.InstantIO.OutSlot;
import org.junit.Test;

public class InstantIOInformerTest {

	/**
	 * This is just a simple test that makes sure the singleton is functioning propertly, it doesn't actually
	 * send data on the InstantIO network
	 */
	@Test 
	public void test() {
		
		InstantIOInformer informer = InstantIOInformer.getInstance();
		informer.start();
		//informer.addNamespace("Comprehension");
		OutSlot s1 = informer.addOutSlot("QA", "Comprehension");
		Assert.assertNotNull(s1);
		OutSlot s2 = informer.addOutSlot("Thing", "Comprehension");
		Assert.assertNotNull(s2);
		s1.push("QA stuff pushed");
		s2.push("Thing stuff pushed");
	}
	
}
