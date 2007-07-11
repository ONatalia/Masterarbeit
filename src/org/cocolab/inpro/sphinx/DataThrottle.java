package org.cocolab.inpro.sphinx;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

public class DataThrottle extends BaseDataProcessor {

    public final static String PROP_SPEED = "speed";	// in milliseconds
	
	private long startTime;
	
	private double speed = 1.0;
	
    /**
     * Constructs a data processor that throttles the data flow
     */
    public void initialize() {
        super.initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        registry.register(PROP_SPEED, PropertyType.DOUBLE);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        speed = ps.getDouble(PROP_SPEED, 1.0);
    }
	
    
	public Data getData() throws DataProcessingException {
        Data input = getPredecessor().getData();
        // DataStartSignals are used to start the timer
        if (input instanceof DataStartSignal) {
        	startTime = System.currentTimeMillis();
        }
        else {
            // everything except signals are delayed
	        if ((input instanceof DoubleData)) {
	        	DoubleData ddinput = (DoubleData) input;
	        	
	        	long audioTime = (long) (((double) (ddinput.getFirstSampleNumber() + ddinput.getValues().length)) 
	        							/ ddinput.getSampleRate() * 1000.0);
	        	long realTime = (long) ((System.currentTimeMillis() - startTime) * speed);
	        	
	            long waitTime = audioTime - realTime;
		        if (waitTime > 0) {
		            System.out.println("waiting " + waitTime + " milliseconds.");
		        	try {
		        		Thread.sleep(waitTime);
		        	} catch (Exception e) {
		        		System.out.println("You woke me up before the time!");
		        	}
		        }
	        }
        }
		return input;
	}

}
