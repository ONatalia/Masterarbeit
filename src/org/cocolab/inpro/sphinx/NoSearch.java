package org.cocolab.inpro.sphinx;

import java.io.IOException;

import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.frontend.DataEndSignal;
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

	Result emptyResult = new Result(null, null, 0, false, null);
	
	FrontEnd fe;
	
	String name;
	
    public void allocate() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void deallocate() {
		// TODO Auto-generated method stub
		
	}

	public Result recognize(int nFrames) {
		// TODO Auto-generated method stub
		for (int i = 0; i < nFrames; i++) {
			try {
				if (fe.getData() instanceof DataEndSignal) {
					emptyResult = null;
				}
			} catch (DataProcessingException e) {
				e.printStackTrace();
				emptyResult = null;
			}
		}
		return emptyResult;
	}

	public void startRecognition() {
		// TODO Auto-generated method stub
		
	}

	public void stopRecognition() {
		// TODO Auto-generated method stub
		
	}

	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	public void newProperties(PropertySheet ps) throws PropertyException {
        fe = (FrontEnd) ps.getComponent(PROP_FRONTEND, FrontEnd.class);
		
	}

	public void register(String name, Registry registry) throws PropertyException {
		// TODO Auto-generated method stub
        this.name = name;
        registry.register(PROP_FRONTEND, PropertyType.COMPONENT);		
	}

	
}
