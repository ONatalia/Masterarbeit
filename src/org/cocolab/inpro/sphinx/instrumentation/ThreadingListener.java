package org.cocolab.inpro.sphinx.instrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.ResultProducer;
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
public class ThreadingListener implements ResultListener, ResultProducer {

	@S4Component(type = ResultProducer.class)
	public final static String PROP_RECOGNIZER = "recognizer";
	@S4ComponentList(type = ResultListener.class)
    public final static String PROP_LISTENERS = "listeners";
	@S4Integer(range = {1, Integer.MAX_VALUE}, defaultValue = Integer.MAX_VALUE)
	public final static String PROP_QUEUE_SIZE = "queueSize";
	
	private ResultProducer recognizer = null;
    protected List<LinkedBlockingQueue<Result>> resultQueues = new ArrayList<LinkedBlockingQueue<Result>>(0);
    protected int queueSize;
    
    static Logger logger;

	@Override
	public void newResult(Result result) {
		if (result.isFinal()) {
			// final results should always be enqueued
			for (BlockingQueue<Result> queue : resultQueues) {
				try {
					queue.put(result);
				} catch (InterruptedException e) {
					logger.warn("I was interrupted while waiting to put a result into a queue");
				}
			}
		} else {
			// non-final results should be skipped if the queue is full
			for (BlockingQueue<Result> queue : resultQueues) {
				if (!queue.offer(result)) {
					logger.info("Skipping a result due to capacity restrictions");
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		logger = Logger.getLogger(ThreadingListener.class);
		BasicConfigurator.configure();
        recognizer = (ResultProducer) ps.getComponent(PROP_RECOGNIZER);
        recognizer.addResultListener(this);
        queueSize = ps.getInt(PROP_QUEUE_SIZE);
        @SuppressWarnings("unused")
		List<ResultListener> resultListeners = (List<ResultListener>) ps.getComponentList(PROP_LISTENERS);
	}
	
	@Override
	public void addResultListener(ResultListener resultListener) {
    	LinkedBlockingQueue<Result> queue = new LinkedBlockingQueue<Result>(queueSize);
    	resultQueues.add(queue);
    	Thread dispatcher = new DispatchThread(resultListener, queue);
    	dispatcher.start();
	}

	@Override
	public void removeResultListener(ResultListener resultListener) {
		// TODO: ignore for now, FIXME please
	}

	private class DispatchThread extends Thread {
		
		BlockingQueue<Result> resultQueue;
		ResultListener listener;
		
		DispatchThread(ResultListener listener, BlockingQueue<Result> resultQueue) {
			this.listener = listener;
			this.resultQueue = resultQueue;
		}

		@Override
		public void run() {
			while (true) {
				try {
					listener.newResult(resultQueue.take());
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

}
