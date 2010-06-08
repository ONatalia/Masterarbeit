package org.cocolab.inpro.sphinx.instrumentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.ResultProducer;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4Integer;

/** 
 * read listeners from configuration, listen in on Recognizer, 
 * then dispatch resultListeners on separate threads on newResult 
 */
public class ThreadingListener implements ResultListener, 
                                          ResultProducer,
                                          StateListener,
                                          Monitor {

	@S4Component(type = ResultProducer.class)
	public final static String PROP_RECOGNIZER = "recognizer";
	@S4ComponentList(type = ResultListener.class)
    public final static String PROP_LISTENERS = "listeners";
	@S4Integer(range = {1, Integer.MAX_VALUE}, defaultValue = Integer.MAX_VALUE)
	public final static String PROP_QUEUE_SIZE = "queueSize";
	
	private ResultProducer recognizer = null;
	protected Map<ResultListener, LinkedBlockingQueue<Result>> resultQueues = new LinkedHashMap<ResultListener, LinkedBlockingQueue<Result>>();
    protected Map<ResultListener, DispatchThread> listenerThreads = new LinkedHashMap<ResultListener, DispatchThread>();
    protected int queueSize;
    
    static Logger logger;

	@Override
	public void newResult(Result result) {
		if (result.isFinal()) {
			// final results should always be enqueued
			for (BlockingQueue<Result> queue : resultQueues.values()) {
				try {
					queue.put(result);
					Thread.yield(); // give the other threads a chance to pick up their results 
				} catch (InterruptedException e) {
					logger.warn("I was interrupted while waiting to put a result into a queue");
				}
			}
		} else {
			// non-final results should be skipped if the queue is full
			for (BlockingQueue<Result> queue : resultQueues.values()) {
				if (!queue.offer(result)) {
					logger.info("Skipping a result due to capacity restrictions");
				}
			}
		}
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		logger = Logger.getLogger(ThreadingListener.class);
		recognizer = (ResultProducer) ps.getComponent(PROP_RECOGNIZER);
        recognizer.addResultListener(this);
        if (recognizer instanceof Recognizer) {
        	((Recognizer) recognizer).addStateListener(this);
        }
        queueSize = ps.getInt(PROP_QUEUE_SIZE);
        @SuppressWarnings("unused")
		List<ResultListener> resultListeners = ps.getComponentList(PROP_LISTENERS, ResultListener.class);
	}
	
	@Override
	public void addResultListener(ResultListener resultListener) {
    	LinkedBlockingQueue<Result> queue = new LinkedBlockingQueue<Result>(queueSize);
    	resultQueues.put(resultListener, queue);
    	DispatchThread dispatcher = new DispatchThread(resultListener, queue);
    	listenerThreads.put(resultListener, dispatcher);
    	dispatcher.start();
	}

	@Override
	public void removeResultListener(ResultListener resultListener) {
		resultQueues.remove(resultListener);
		DispatchThread dispatcher = listenerThreads.remove(resultListener);
		dispatcher.run = false;
		dispatcher.interrupt();
	}
	
	@Override
	public void statusChanged(State status) {
		if (status == State.DEALLOCATING) {
			for (ResultListener listener : listenerThreads.keySet()) {
				removeResultListener(listener);
			}
		}
	}

	private class DispatchThread extends Thread {
		
		BlockingQueue<Result> resultQueue;
		ResultListener listener;
		boolean run = true;
		
		DispatchThread(ResultListener listener, BlockingQueue<Result> resultQueue) {
			super("result dispatch thread for " + listener);
			this.listener = listener;
			this.resultQueue = resultQueue;
		}

		@Override
		public void run() {
			while (run)
				try {
					listener.newResult(resultQueue.take());
				} catch (InterruptedException e) {
				}
		}
	}

}
