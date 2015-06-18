package inpro.incremental.unit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.MaryAdapter;

import java.util.EnumSet;
import java.util.List;

import marytts.htsengine.HMMData.FeatureType;
import marytts.htsengine.HTSModel;

import org.junit.Test;
import org.junit.Ignore;

public class IncrementalCARTTest {

	// do not test this anymore, as legacy and generated models differ in incremental synthesis
	@Ignore @Test
	public void test() {
		MaryAdapter ma = MaryAdapter.getInstance();
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<IU> ius = (List) ma.text2WordIUs("eins zwei drei vier fünf sechs sieben acht");
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
			for (FeatureType ft : EnumSet.of(FeatureType.STR, FeatureType.MGC)) {
				assertArrayEquals(m1.getMean(ft, i), m2.getMean(ft, i), 0.0001f);
				assertArrayEquals(m1.getVariance(ft, i), m2.getVariance(ft, i), 0.0001f);
			}
		}
		return true;
	}

}
