package inpro.incremental.listener;

import inpro.incremental.FrameAware;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.util.TedAdapter;

import java.util.List;
import java.util.Collection;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

/**
 * A notifier that outputs IU structures to TEDview
 * 
 * Even though TedViewNotifier is an IU-sink (i.e., not a real processor),
 * it is implemented as an IUModule, because IUModules already have the 
 * necessary TEDview communication code available.
 * 
 * @author timo
 */
public class TEDviewNotifier extends IUModule implements FrameAware {

    @S4Integer(defaultValue = 2000)
    public final static String PROP_TEDVIEW_PORT = "tedPort";
    @S4String(defaultValue = "localhost")
    public final static String PROP_TEDVIEW_ADDRESS = "tedAddress";

    /** 
     * if set to true, TEDview event times will disregard any processing delays 
     * and use theoretical frame timings instead; 
     * if false, actual wall-clock time will be used for event times (default) 
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_LOGICAL_TIME = "useLogicalTime";

    private boolean useLogicalTime = false;
    
    private TedAdapter tedAdapter;
    
	int currentFrame = 0;

	@Override
	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		int tedPort = ps.getInt(PROP_TEDVIEW_PORT);
		String tedAddress = ps.getString(PROP_TEDVIEW_ADDRESS);
		int tedLogPort = ps.getInt(PROP_TEDVIEW_LOG_PORT);
		String tedLogAddress = ps.getString(PROP_TEDVIEW_LOG_ADDRESS);
		useLogicalTime = ps.getBoolean(PROP_LOGICAL_TIME);
		if (tedPort != tedLogPort || !tedAddress.equals(tedLogAddress))
			tedAdapter = new TedAdapter(tedAddress, tedPort);
		else
			tedAdapter = tedLogAdapter;
	}

	@Override
	public synchronized void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (tedAdapter.isConnected() && (edits.size() > 0) && (ius.size() > 0)) {
	    	StringBuilder sbIUs = new StringBuilder();
	    	sbIUs.append("<event time='");
	    	sbIUs.append(getTime());
	    	IU iuType = ius.iterator().next();
	    	sbIUs.append("' originator='" + iuType.getClass().getSimpleName() + "'>");
	    	for (IU iu : ius) {
	    		sbIUs.append(iu.toTEDviewXML());
	    	}
	    	sbIUs.append("</event>");
	    	tedAdapter.write(sbIUs.toString());
		}
	}
	
	@Override
	public int getTime() {
		if (useLogicalTime)
			return currentFrame * 10;
		else
			return super.getTime();
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		throw new UnsupportedOperationException("not implemented");
	}

}
