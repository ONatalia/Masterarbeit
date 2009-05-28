package org.cocolab.inpro.sphinx;

import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * recognizing is so time consuming (and sometimes useless)
 * 
 * why not have a recognizer that doesn't recognize at all,
 * just consumes frames from the frontend and calls event 
 * listeners and monitors from time to time
 * 
 * @author timo
 *
 */

public class NoSearch implements SearchManager {

	public static final String PROP_FRONTEND = "frontend";

	protected static final Result emptyResult = new Result(null, null, 0, false, null);
	
	@S4Component(type = FrontEnd.class)
	FrontEnd fe;
	
    public void allocate() {
		// ignore
	}

	public void deallocate() {
		// ignore
	}

	public Result recognize(int nFrames) {
		// TODO Auto-generated method stub
		Result result = null;
		for (int i = 0; i < nFrames; i++) {
			try {
				Data d = fe.getData();
				// only send results for new data, not for signals
				while ((d != null) && (d instanceof Signal)) {
					d = fe.getData();
				}
				result = (d != null) ? emptyResult : null;
			} catch (DataProcessingException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public void startRecognition() {
		// ignore
	}

	public void stopRecognition() {
		// ignore
	}

	public void newProperties(PropertySheet ps) throws PropertyException {
        fe = (FrontEnd) ps.getComponent(PROP_FRONTEND);
	}

	
}
