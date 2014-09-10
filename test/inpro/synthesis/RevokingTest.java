package inpro.synthesis;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.sink.CurrentHypothesisViewer;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.Progress;
import inpro.synthesis.MaryAdapter;

import org.junit.Test;

/**
 * Test revoking. Based on CARCHASE 2 Incremental Articulator.
 * @author Alexis Engelke
 */
public class RevokingTest {
	private int ongoing, upcoming, completed, total;

	@Test(timeout=30000)
	public void test() {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();

		SynthesisModule synthesisModule = new SynthesisModule(dispatcher);
		RTIUSource s = new RTIUSource();
		s.addListener(synthesisModule);
		s.addListener(new CurrentHypothesisViewer().show());
		
		// This works, as everything is synchronous.
		s.addListener(new PushBuffer() {
			public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
				ongoing = completed = upcoming = total = 0;
				System.err.println("----");
				for (IU iu : ius) {
					total++;
					if (iu.isCompleted()) {
						completed++;
					} else if (iu.isOngoing()) {
						ongoing++;
						System.err.println("this is an ongoing IU: " + iu.toLabelLine());
						assertTrue("this is the second ongoing IU: " + iu.toLabelLine(), ongoing <= 1);
					} else
						upcoming++;
				}
			}
		});
		synthesisModule.addListener(new CurrentHypothesisViewer().show());
		synthesisModule.addListener(new LabelWriter());
		
		delay(1000);
		s.say("Dies ist die dorfführende dörfliche Dorfstraßen-Straße.", "Dies ist die Dorfstraße.", false, false);
		s.say("Dies ist die feldführende feldliche Feldstraßen-Straße.", "Dies ist die Feldstraße.", false, false);
		delay(500);
		reduceOffset(s);
		s.say("Dies ist die nordführende nördliche Nordstraßen-Straße.", "Dies ist die Nordstraße.", false, false);
		s.say("Dies ist die westführende westliche Weststraßen-Straße.", "Dies ist die Weststraße.", false, false);
		delay(500);
		reduceOffset(s);
		delay(20000);
	}
	
	private void delay(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {}
	}

	private void reduceOffset(RTIUSource s) {
		s.beginChanges();
		ArrayList<IU> ius = s.revokeUpcoming();
		for (IU iu : ius) {
			if (!(iu instanceof ChunkIU)) continue;
			String shorter = (String) iu.getUserData("shorter");
			String normal = (String) iu.getUserData("normal");
			if (shorter != null) {
				if (shorter.length() > 10) // Always true here.
					s.say(normal, shorter, true, true);
			} else {
				s.say(normal, shorter, false, true);
			}
		}
		s.doneChanges();
		System.out.println("ONGOING: " + ongoing + "; UPCOMING: " + upcoming + "; COMPLETED: " + completed + "; TOTAL: " + total);
		
		// There cannot be more than one ongoing (Chunk)IU at the same time.
		assertTrue(ongoing <= 1);
		assertTrue(upcoming + ongoing + completed == total);
	}

	private static class RTIUSource extends IUModule {
		private boolean changing;

		@Override
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw new RuntimeException();
		}
		
		public ArrayList<IU> revokeUpcoming() {
			ArrayList<IU> upcoming = new ArrayList<IU>();
			ArrayList<IU> all = new ArrayList<IU>();
			for (IU lastIU : rightBuffer.getBuffer())  {
				if (lastIU.getProgress() != Progress.UPCOMING) continue;
				if (lastIU.isCommitted()) continue;
				all.add(lastIU);
				if (!(lastIU instanceof ChunkIU)) continue;
				upcoming.add(lastIU);
			}
			for (int i = all.size() - 1; i >= 0; i--)
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, all.get(i)));
			if (!changing)
				rightBuffer.notify(iulisteners);
			return upcoming;
		}
		
		public void say(String a1, String a2, boolean shorter, boolean commit) {
			ChunkIU iu = new ChunkIU(shorter ? a2 : a1);
			iu.setUserData("normal", a1);
			iu.setUserData("shorter", a2);
			rightBuffer.addToBuffer(iu);
			if (commit)
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.COMMIT, iu));
			if (!changing)
				rightBuffer.notify(iulisteners);
		}
		
		public void beginChanges() {
			changing = true;
		}
		
		public void doneChanges() {
			changing = false;
			rightBuffer.notify(iulisteners);
		}
	}
}