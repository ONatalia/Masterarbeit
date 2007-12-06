package org.cocolab.inpro.sphinx;

import java.io.IOException;

import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

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
	
	FrontEnd fe;
	
	String name;
	
    public void allocate() throws IOException {
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
				result = (fe.getData() == null) ? null : emptyResult;
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

	public String getName() {
		return name;
	}

	public void newProperties(PropertySheet ps) throws PropertyException {
        fe = (FrontEnd) ps.getComponent(PROP_FRONTEND, FrontEnd.class);
	}

	public void register(String name, Registry registry) throws PropertyException {
        this.name = name;
        registry.register(PROP_FRONTEND, PropertyType.COMPONENT);		
	}

	
}
