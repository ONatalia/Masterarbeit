package inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

import org.junit.Test;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ThreadingModuleTest extends IUModule {
	
	@Test
	public void test() {
		if (iulisteners != null) {
			iulisteners.clear();
		}
		IUModule threadingModule = new ThreadingModule();
		threadingModule.addListener(myListeningIUModule);
		//addListener(myListeningIUModule);
		addListener(threadingModule);
		addListener(new LabelWriter());
		for (int i = 0; i < 10; i++) {
			addIU();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void addIU() {
		rightBuffer.addToBuffer(new MinimalIU());
		rightBuffer.notify(iulisteners);
	}
	
	class MinimalIU extends IU {
		@Override
		public String toPayLoad() {
			return String.valueOf(getID());
		}
	}	

	IUModule myListeningIUModule = new IUModule() {
		@Override
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			System.err.println("received list of IUs: " + ius);
			System.err.println("received list of edits: " + edits);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	};

	/** I'm a source, don't call me */
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		throw new NotImplementedException(); }
	
}
