package org.cocolab.inpro.incremental;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.util.TedAdapter;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

/**
 * Abstract class of an incremental module in InproTk.
 * Implementing modules should implement leftBufferUpdate() and,
 * from within this method, use one of the rightBuffer.setBuffer() methods
 * to set their output after this processing increment.
 * 
 * @author timo
 */
public abstract class IUModule extends PushBuffer {

	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	List<PushBuffer> listeners;
	
    @S4Integer(defaultValue = 2000)
    public final static String PROP_TEDVIEW_LOG_PORT = "tedLogPort";
    @S4String(defaultValue = "localhost")
    public final static String PROP_TEDVIEW_LOG_ADDRESS = "tedLogAddress";
    
    protected TedAdapter tedLogAdapter;
    
	/** the right buffer of this module */
	protected final RightBuffer rightBuffer = new RightBuffer();
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		listeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
		int tedPort = ps.getInt(PROP_TEDVIEW_LOG_PORT);
		String tedAddress = ps.getString(PROP_TEDVIEW_LOG_ADDRESS);
		tedLogAdapter = new TedAdapter(tedAddress, tedPort);
	}
	
	/**
	 * the method that IU modules must implement 
	 * @param ius list of IUs that make up the current hypothesis
	 * @param edits a list of edits since the last call
	 */
	protected abstract void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits);

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		leftBufferUpdate(ius, edits);
		rightBuffer.notify(listeners);		
	}

	/* * * utility methods * * */
	
	public long getTime() {
		return System.currentTimeMillis() - IU.startupTime;
	}
	
	public void logToTedView(String message) {
		String tedTrack = this.getClass().getSimpleName();
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(getTime());
		sb.append("' originator='");
		sb.append(tedTrack);
		sb.append("'>");
		sb.append(message.replace("<", "&lt;").replace(">", "&gt;"));
		sb.append("</event>");
		tedLogAdapter.write(sb.toString());
	}
	
	/**
	 * Encapsulates the module's output in two representations. 
	 * 
	 * Modules should call either of the setBuffer methods in their 
	 * implementation of leftBufferUpdate().
	 * 
	 * @author timo
	 */
	protected class RightBuffer {
		/** true if the content has changed since the last call to notify */
		boolean hasUpdates = false;
		IUList<IU> ius = new IUList<IU>();
		List<EditMessage<IU>> edits;
		
		// just a list of IUs, automatically infers the edits since the last call 
		public void setBuffer(Collection<? extends IU> outputIUs) {
			IUList<IU> newList = new IUList<IU>();
			newList.addAll(outputIUs);
			edits = ius.diff(newList);
			ius = newList;
			hasUpdates = !edits.isEmpty();
		}
		
		// just a list of edits, automatically updates IUs from last call
		@SuppressWarnings("unchecked")
		public void setBuffer(List<? extends EditMessage<? extends IU>> edits) {
			ius.apply((EditMessage<IU>) edits);
			hasUpdates = !edits.isEmpty();
		}
		
		// both ius and edits
		@SuppressWarnings("unchecked")
		public void setBuffer(Collection<? extends IU> outputIUs,
				List<? extends EditMessage<? extends IU>> outputEdits) {
			ius = new IUList<IU>();
			ius.addAll(outputIUs);
			edits = (List<EditMessage<IU>>) outputEdits;
			hasUpdates = !edits.isEmpty();
		}
		
		public void notify(PushBuffer listener) {
			if (hasUpdates) {
				listener.hypChange(ius, edits);
			}
		}
		
		public void notify(List<PushBuffer> listeners) {
			for (PushBuffer listener : listeners) {
				notify(listener);
			}
			hasUpdates = false;
		}	
	}
	
}
