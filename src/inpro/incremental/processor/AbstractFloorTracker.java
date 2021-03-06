package inpro.incremental.processor;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;

/**
 * A floor tracker tracks which dialogue participant has the floor
 * (i.e. the user or the system) and takes decisions when the floor 
 * should be taken or released 
 * 
 * @author timo
 *
 */
public abstract class AbstractFloorTracker extends IUModule {

	@S4ComponentList(type = Listener.class)
	public final static String PROP_STATE_LISTENERS = "ftlisteners";
	public List<Listener> ftlisteners;

	/** this is used to infer prosody as needed */
	List<WordIU> mostRecentIUs;
	
	/**
	 * other modules are notified about floor state if they implement FloorManager.Listener
	 * and are registered to listen in the configuration file 
	 */
	public enum Signal { NO_INPUT, START, EOT_FALLING, EOT_RISING, EOT_NOT_RISING }

	/**
	 * the internal state of the floor tracker:
	 * - user is silent and we're not expecting her to speak
	 * - user is silent but we are expecting her to speak (this may lead to a time-out)
	 * - user is talking (currently no provisions for whether we want her to speak or not) 
	 */
	protected enum InternalState { NOT_AWAITING_INPUT, AWAITING_INPUT, IN_INPUT } // not implementing POST_INPUT, because it doesn't mean anything for text 
	
	InternalState internalState = InternalState.NOT_AWAITING_INPUT;
	
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		mostRecentIUs = (List<WordIU>) ius;
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		List<Object> listeners = ps.getComponentList(PROP_STATE_LISTENERS, Object.class);
		ftlisteners = new ArrayList<Listener>();
		for (Object o : listeners)
			if (o instanceof Listener)
				ftlisteners.add((Listener) o);
	}

	protected void signalListeners(InternalState state, Signal signal) {
		logger.debug("Notifying my ftlisteners about " + signal);
		internalState = state;
		for (Listener l : ftlisteners) {
			l.floor(signal, this);
		}
	}

	protected boolean isNotInInput() {
		return internalState.equals(InternalState.AWAITING_INPUT) || internalState.equals(InternalState.NOT_AWAITING_INPUT);
	}
	

	/** the interface that the floor manager's listeners must implement */
	public interface Listener extends Configurable {
		/**
		 * will be called when the floor state changes
		 * @param signal the signal describing the change in the floor
		 * @param floorManager	the floor manager itself. in this way a consumer can request e.g. to change timeouts 
		 */
		public void floor(Signal signal, AbstractFloorTracker floorManager);
	}
	
}
