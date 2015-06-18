package inpro.incremental.unit;

import static org.junit.Assert.*;
import inpro.incremental.unit.IU.IUUpdateListener;

import org.junit.Test;

public class IUUpdateListenerTest {

	/** multiple adds of the same listener should not result in multiple calls to the listener 
	 * @throws InterruptedException */ 
	@Test
	public void testMultipleAddsSingleListener() throws InterruptedException {
		MyCountingIUUpdateListener l = new MyCountingIUUpdateListener();
		IU iu = new IU() {
			@Override
			public String toPayLoad() {
				return null;
			}
		};
		iu.addUpdateListener(l);
		iu.addUpdateListener(l);
		iu.notifyListeners();
		Thread.sleep(1000);
		assertEquals(1, l.numCalls);
	}
	
	static class MyCountingIUUpdateListener implements IUUpdateListener {
		int numCalls = 0;
		public void update(IU updatedIU) {
			numCalls++;
		}
	}
	
	/** ensure that frequent and deeply nested GRIN updates do not take forever 
	 * @throws InterruptedException */
	@Test
	public void testDeeplyNestedGRINUpdates() throws InterruptedException {
		SysInstallmentIU instIU = new SysInstallmentIU("eins zwei drei vier");
		instIU.updateOnGrinUpdates();
		final int repetitions = 1000;
		int counts = instIU.getSegments().size() * repetitions;
		MyTimingCountsIUUpdateListener mtciuul = new MyTimingCountsIUUpdateListener(counts);
		instIU.addUpdateListener(mtciuul);
		long startTime = System.currentTimeMillis();
		for (SegmentIU seg : instIU.getSegments()) {
			for (int i = 0; i < repetitions; i++) {
				seg.notifyListeners();
			}
		}
		while (mtciuul.count > 0) {
			System.out.println(mtciuul.count);
			Thread.sleep(10);
		}
		long duration = mtciuul.time - startTime;
		System.out.println("executing " + counts + " segment-level updates took " + duration + " milliseconds");
	}
	
	/** record the time at which a certain number of calls to the listener have occurred */
	static class MyTimingCountsIUUpdateListener implements IUUpdateListener {
		int count;
		long time;
		MyTimingCountsIUUpdateListener(int count) {
			this.count = count;
		}
		public void update(IU updatedIU) {
			synchronized(this) {
				count--;
			}
			if (count == 0) {
				time = System.currentTimeMillis();
			}
		}
	}
}