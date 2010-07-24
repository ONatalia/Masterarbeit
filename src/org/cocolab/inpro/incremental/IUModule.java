package org.cocolab.inpro.incremental;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.domains.greifarm.ActionIU;
import org.cocolab.inpro.incremental.listener.FrameAwarePushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;

public abstract class IUModule extends PushBuffer {

	@S4ComponentList(type = FrameAwarePushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	List<PushBuffer> listeners;
	/** the right buffer of this module */
	protected final RightBuffer rightBuffer = new RightBuffer();

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		listeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
	}
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		leftBufferUpdate(ius, edits);
		rightBuffer.notify(listeners);		
	}
	
	public abstract void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits);

	protected class RightBuffer {
		
		boolean hasUpdates = false;
		Collection<? extends IU> ius;
		List<? extends EditMessage<? extends IU>> edits;
		
		public void setBuffer(Collection<? extends IU> outputIUs) {
			hasUpdates = true;
			ius = outputIUs;
			// TODO:
			edits = Collections.<EditMessage<ActionIU>>singletonList(new EditMessage<ActionIU>(EditType.ADD, null));
		}
		
		public void setBuffer2() {
			// TODO
			// just a list of IUs, automatically infers the edits since the last call 
		}
		
		public void setBuffer3() {
			// TODO
			// just a list of edits, automatically updates IU from last call
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
