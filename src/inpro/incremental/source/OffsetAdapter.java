package inpro.incremental.source;

import inpro.incremental.deltifier.ASRWordDeltifier;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;


/**
 *  there are two alternatives to set the offset:
 *  - put an OffsetAdapter into the FrontEnd (after VAD)    
 * 	- alternatively, use ASRWordDeltifier.signalOccurred() ; for this you have to call 
 *    FrontEnd.addSignalListener(deltifier) somewhere (CurrentHypothesis-setup)
 *    
 *     here, the offset is given in centiseconds (frames)
 */

public class OffsetAdapter extends BaseDataProcessor {

	@S4Component(defaultClass = SphinxASR.class, type = SphinxASR.class)
	public static final String PROP_CASRH = "casrh";
	
	SphinxASR casrh;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		casrh = (SphinxASR) ps.getComponent(PROP_CASRH);
	}

    private boolean inAudio = false; 
    @Override
	public Data getData() throws DataProcessingException {
		Data data = getPredecessor().getData();
		ASRWordDeltifier asrDeltifier = casrh.getDeltifier();
		if ((!inAudio) && (data instanceof DoubleData)) {
			long chunkStartSample = ((DoubleData) data).getFirstSampleNumber();
			long collectTime = ((DoubleData) data).getCollectTime();
			asrDeltifier.setOffset((int) chunkStartSample / 160); // divide by 16000, multiply by 100 -> frames
			asrDeltifier.setCollectTime(collectTime);
			
			inAudio = true;
		}
		if ((data instanceof Signal)) {
			asrDeltifier.signalOccurred((Signal) data);
			inAudio = false;
		}
		return data;
	}
}
