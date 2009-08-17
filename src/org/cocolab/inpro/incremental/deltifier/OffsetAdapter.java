package org.cocolab.inpro.incremental.deltifier;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class OffsetAdapter extends BaseDataProcessor {

	@S4Component(defaultClass = ASRWordDeltifier.class, type = ASRWordDeltifier.class)
	public static final String PROP_DELTIFIER = "deltifier";
	
	ASRWordDeltifier asrDeltifier;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		asrDeltifier = (ASRWordDeltifier) ps.getComponent(PROP_DELTIFIER);
	}

    private boolean inAudio = false; 
    @Override
	public Data getData() throws DataProcessingException {
		Data data = getPredecessor().getData();
		if ((!inAudio) && (data instanceof DoubleData)) {
			long chunkStartSample = ((DoubleData) data).getFirstSampleNumber();
			asrDeltifier.setOffset((int) chunkStartSample / 160); // divide by 16000, multiply by 100 -> frames
			inAudio = true;
		}
		if ((data instanceof Signal)) {
			inAudio = false;
		}
		return data;
	}
}
