package inpro.io.webspeech;

import junit.framework.Assert;
import inpro.io.webspeech.model.AsrHyp;

import org.junit.Test;

public class AsrHypTest {
	
	@Test 
	public void test() {
		AsrHyp asrHyp = new AsrHyp("super DUPER hyp", 0.88);
		Assert.assertEquals(asrHyp.getHyp(), "super duper hyp");
		Assert.assertEquals(asrHyp.getConfidence(), 0.88);
	}

}
