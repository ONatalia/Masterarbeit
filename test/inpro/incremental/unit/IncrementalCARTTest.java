package inpro.incremental.unit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.MaryAdapter;

import java.util.List;

import marytts.htsengine.HTSModel;

import org.junit.Test;

public class IncrementalCARTTest {

	@Test
	public void test() {
		MaryAdapter ma = MaryAdapter.getInstance();
		List<IU> ius = ma.text2IUs("eins zwei drei vier fünf sechs sieben acht");
		for (IU iu : ius) {
			WordIU p = (WordIU) iu;
			for (SegmentIU s : p.getSegments()) {
				assert s instanceof SysSegmentIU;
				SysSegmentIU ss = (SysSegmentIU) s;
				assertTrue(same(ss.legacyHTSmodel, ss.generateHTSModel()));
			}
		}
	}

	public static boolean same(HTSModel m1, HTSModel m2) {
		assertEquals(m1.getPhoneName(), m2.getPhoneName());
		//assertEquals("in phone " + m1.getPhoneName(), m1.getTotalDur(), m2.getTotalDur());
		//assertEquals(m1.getNumVoiced(), m2.getNumVoiced());
		for (int i = 0; i < 5; i++) {
			if (m1.getDur(i) != m2.getDur(i))
				System.err.println("in phone " + m1.getPhoneName() + ", state " + i + ": m1 " + m1.getDur(i) + " vs. m2 " + m2.getDur(i));
			assertEquals("in phone " + m1.getPhoneName() + ", state " + i, m1.getDur(i), m2.getDur(i));
			assertArrayEquals(m1.getLf0Mean(i), m2.getLf0Mean(i), 0.0001f);
			assertArrayEquals(m1.getLf0Variance(i), m2.getLf0Variance(i), 0.0001f);
			assertArrayEquals(m1.getStrMean(i), m2.getStrMean(i), 0.0001f);
			assertArrayEquals(m1.getStrVariance(i), m2.getStrVariance(i), 0.0001f);
			//assertArrayEquals(m1.getMagMean(i), m2.getMagMean(i), 0.0001f);
			//assertArrayEquals(m1.getMagVariance(i), m2.getMagVariance(i), 0.0001f);
			assertArrayEquals(m1.getMcepMean(i), m2.getMcepMean(i), 0.0001f);
			assertArrayEquals(m1.getMcepVariance(i), m2.getMcepVariance(i), 0.0001f);
		}
		return true;
	}

}
