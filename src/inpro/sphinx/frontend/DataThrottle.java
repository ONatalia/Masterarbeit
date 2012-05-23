package inpro.sphinx.frontend;

import inpro.util.TimeUtil;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;

/**
 * a data processor that throttles the data flow and re-aligns 
 * microphone and IU world startup times
 */
public class DataThrottle extends BaseDataProcessor {

	@S4Double(defaultValue = 1)
    public final static String PROP_SPEED = "speed";	// in milliseconds
	
	private long startTime;
	
	private double speed = 1.0;
	
    /*
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        speed = ps.getDouble(PROP_SPEED);
    }
	
    
	public Data getData() throws DataProcessingException {
		getTimer().start();
        Data input = getPredecessor().getData();
        // DataStartSignals are used to start the timer
        if (input instanceof DataStartSignal) {
        	long currentTime = System.currentTimeMillis();
        	startTime = ((DataStartSignal) input).getTime();
        	System.out.println("it is now " + currentTime);
        	System.out.println("microphone started at " + startTime);
        	System.out.println("IU world started at " + TimeUtil.startupTime);
        	System.out.println("shifting IU world by " + (TimeUtil.startupTime - startTime) + " ms");
        	TimeUtil.startupTime = startTime;
        } else {
            // everything except signals are delayed
	        if ((input instanceof DoubleData)) {
	        	DoubleData ddinput = (DoubleData) input;
	        	
	        	long audioTime = (long) (((double) (ddinput.getFirstSampleNumber() + ddinput.getValues().length)) 
	        							/ ddinput.getSampleRate() * TimeUtil.SECOND_TO_MILLISECOND_FACTOR);
	        	long realTime = (long) ((System.currentTimeMillis() - startTime) * speed);
	        	
	            long waitTime = audioTime - realTime;
		        if (waitTime > 0) {
		            //System.out.println("waiting " + waitTime + " milliseconds.");
		        	try {
		        		Thread.sleep(waitTime);
		        	} catch (Exception e) {
		        		System.out.println("You woke me up before the time!");
		        	}
		        }
	        }
        }
        getTimer().stop();
		return input;
	}

}
