package inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

public class ThreadingModule extends IUModule {

	final ModuleState mostRecentState = new ModuleState();
	boolean inShutdown = false;
	
	public ThreadingModule() {
		new Thread(bufferProcessor).start();
	}
	
	/** we need to override IUModule.hypChange, because we notify the rightBuffer on our own */
	@Override
	public synchronized void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		leftBufferUpdate(ius, edits);
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		synchronized(mostRecentState) {
			mostRecentState.ius = ius;
			mostRecentState.enqueuedState = true;
			mostRecentState.notifyAll();
		}
	}
	
	@Override
	public void reset() {
		synchronized(mostRecentState) {
			mostRecentState.enqueuedState = false;
			mostRecentState.notifyAll();
		}
	}
	
	public void shutdown() {
		this.inShutdown = true;
		reset();
	}
	
	private class ModuleState {
		Collection<? extends IU> ius;
		boolean enqueuedState = false;
	}
	
	private Runnable bufferProcessor = new Runnable() {
		@Override
		public void run() {
			try {	
				while (!inShutdown) {
					Collection<? extends IU> ius = null;
					synchronized(mostRecentState) {
						if(mostRecentState.enqueuedState) {
							ius = mostRecentState.ius;
							mostRecentState.enqueuedState = false;
						}
					}
					if (ius != null) {
						rightBuffer.setBuffer(mostRecentState.ius);
						rightBuffer.notify(iulisteners);
					} else {
						synchronized(mostRecentState) {
							mostRecentState.wait(500);
						}
					}
				}
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		}
	};

}
