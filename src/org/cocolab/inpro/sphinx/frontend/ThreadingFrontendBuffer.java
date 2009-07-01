package org.cocolab.inpro.sphinx.frontend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;

public class ThreadingFrontendBuffer extends BaseDataProcessor implements Runnable {

	private static int queueCapacity = 5;
	
	BlockingQueue<Data> queue = new LinkedBlockingQueue<Data>(queueCapacity);
	
	Thread drainer = null;
	
	@Override
	public Data getData() throws DataProcessingException {
		if (drainer == null) {
			start();
		}
		Data data = null;
		while (data == null) { 
			try {
				data = queue.take();
			} catch (InterruptedException e) {
				// ignore interruption (just try again)
			}
		}
		return data;
	}

	private void start() {
		drainer = new Thread(this);
		drainer.start();
	}
	
	@Override
	public void run() {
		Data data;
		do {
			data = getPredecessor().getData();
			do {
				try {
					queue.put(data);
					data = null;
				} catch (InterruptedException e) {
					// ignore interruption (just try again)
				}
			} while(data != null);
		} while (true);
	}

}
