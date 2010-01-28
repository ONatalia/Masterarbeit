package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;

/**
 * The floor manager tracks which dialogue participant has the floor
 * (i.e. the user or the system) and takes decisions when the floor 
 * should be taken or released 
 * 
 * @author timo
 *
 */
public class FloorManager implements PushBuffer {

	@S4ComponentList(type = Listener.class)
	public final static String PROP_STATE_LISTENERS = "listeners";
	List<Listener> listeners;

	@Override
	@SuppressWarnings("unchecked")
	public void newProperties(PropertySheet ps) throws PropertyException {
		listeners = (List<Listener>) ps.getComponentList(PROP_STATE_LISTENERS); 
	}

	/*
	 * the floor manager will have to become a living thing sooner or later.
	 * 
	 * for this to happen it will have to become its own thread in the constructor,
	 * that waits for hypChanges to occur (that is, be added to a thread-safe queue),
	 * for timers to run out or for other fun things 
	 * 
	 * alternatively, it could live through the fact that many short-living
	 * runnables are being created and die after a while. 
	 * 
	 * while the first options runs the risk of loosing control (because the thread
	 * wanders off to another processor), the second runs the risk of creating
	 * incredible amounts of threads, which is no better 
	 */
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// TODO: check type of IUs (right now, we only expect WordIUs)
		// TODO: not only react to WordIUs, but to more interesting stuff
		if ((edits != null) && (edits.size() > 0) && (edits.get(0).getType().equals(EditType.COMMIT))) {
			for (Listener l : listeners) {
				l.floor(State.AVAILABLE, State.UNAVAILABLE, this);
			}
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub, there's nothing to reset yet
	}

	/**
	 * externally visible floor states
	 * other modules are notified about floor state if they implement FloorManager.Listener
	 * and are registered to listen in the configuration file 
	 * @author timo
	 */
	public enum State {
		AVAILABLE, UNAVAILABLE, CONTESTED, GONE;
	}

	/**
	 * the interface that the floor manager's listeners must implement
	 * @author timo
	 */
	public interface Listener extends Configurable {
		/**
		 * will be called when the floor state changes
		 * @param floorState	the new state of the floor
		 * @param previousState	the previous state of the floor
		 * @param floorManager	the floor manager itself. in this way a consumer can request e.g. to change timeouts 
		 */
		public void floor(State floorState, State previousState, FloorManager floorManager);
	}

}
