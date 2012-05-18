package inpro.sphinx.frontend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;

/**
 * A frontend processor that splits off (parts of) frontend processing into separate threads.
 * Put this processor as the last element of your frontend pipeline, in order to put the whole 
 * frontend into its separate thread. Your mileage on performance improvements may vary.
 * @author timo
 */
public class ThreadingFrontendBuffer extends BaseDataProcessor implements Runnable {

	private static int queueCapacity = 5;
	
	BlockingQueue<Data> queue = new LinkedBlockingQueue<Data>(queueCapacity);
	
	boolean running = true;
	
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
		drainer = new Thread(this, "threading frontend buffer");
		drainer.setDaemon(true);
		drainer.start();
	}
	
	@Override
	public void run() {
		Data data;
		do {
			data = getPredecessor().getData();
			if (data == null) {
				running = false;
			} else {
				do {
					try {
						queue.put(data);
						data = null;
					} catch (InterruptedException e) {
						// ignore interruption (just try again)
					}
				} while(data != null);
			}
		} while (running);
	}

}
