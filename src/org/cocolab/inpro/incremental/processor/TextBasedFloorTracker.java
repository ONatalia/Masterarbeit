package org.cocolab.inpro.incremental.processor;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;

/**
 * 
 * @author timo
 */
public class TextBasedFloorTracker extends AbstractFloorTracker {

	public SignalPanel signalPanel;

	public TextBasedFloorTracker() {
		internalState = InternalState.NOT_AWAITING_INPUT;
		signalPanel = new SignalPanel();
		signalPanel.updateButtons();
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
	@SuppressWarnings("unchecked")
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// TODO: check type of IUs (right now, we only expect WordIUs)
		// TODO: not only react to WordIUs, but to more interesting stuff
		mostRecentIUs = (IUList) ius;
		if (isNotInInput() && ius.size() > 0) {
			setSoT();
		}	
	}

	@Override
	public void reset() { // ignore
	}

	private boolean isNotInInput() {
		return internalState.equals(InternalState.AWAITING_INPUT) || internalState.equals(InternalState.NOT_AWAITING_INPUT);
	}
	
	
	public void setExpect() {
		if (internalState.equals(InternalState.NOT_AWAITING_INPUT)) {
			internalState = InternalState.AWAITING_INPUT;
			signalPanel.updateButtons();
		}
	}
	
	public void setSoT() {
		assert isNotInInput();
		signal(InternalState.IN_INPUT, Signal.START);
	}
	

	
	private void signal(InternalState inInput, Signal start) {
		signalListeners(inInput, start);
		signalPanel.updateButtons();
	}

	/** call this to (externally) assert that speech is over */
	public void setEOT() {
		assert internalState.equals(InternalState.IN_INPUT);
		WordIU lastWord = (WordIU) mostRecentIUs.get(mostRecentIUs.size() - 1); 
		Signal signal;
		if (lastWord.hasProsody()) {
			signal = lastWord.pitchIsRising() ? Signal.EOT_RISING : Signal.EOT_FALLING;
		} else {
			signal = Signal.EOT_ANY;
		}
		// TODO: find out what type of signal to send
		signal(InternalState.NOT_AWAITING_INPUT, signal);
	}
	
	public void setNoInput() {
		assert internalState.equals(InternalState.AWAITING_INPUT);
		signal(InternalState.NOT_AWAITING_INPUT, Signal.NO_INPUT);
	}
	
	@SuppressWarnings("serial")
	private class SignalPanel extends JPanel {
		JButton noInputButton = new JButton(new AbstractAction("noInput") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setNoInput();
			}
		});
		JButton eotButton = new JButton(new AbstractAction("EoT") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setEOT();
			}
		});
		
		SignalPanel() {
			add(noInputButton);
			add(eotButton);
		}
		
		void updateButtons() {
			noInputButton.setEnabled(internalState.equals(InternalState.AWAITING_INPUT));
			eotButton.setEnabled(!isNotInInput());
		}
	}

}
