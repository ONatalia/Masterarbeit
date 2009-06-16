package org.cocolab.inpro.incremental.listener;

import java.util.List;

import org.cocolab.inpro.incremental.ASRResultKeeper;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public abstract class HypothesisChangeListener implements Configurable, Resetable {

	int currentFrame = 0;


	/*
	 * this should receive a list of current IUs and 
	 * a list of edit messages since the last call to hypChange
	 */
	public abstract void hypChange(List<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits);

	public void hypChange(ASRResultKeeper deltifier) {
		List<EditMessage<WordIU>> edits = deltifier.getEdits();
		List<WordIU> ius = deltifier.getIUs();
		currentFrame = deltifier.getCurrentFrame();
		hypChange(ius, edits);
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		// ignore by default
	}

	@Override
	public void reset() {
		// ignore by default
	}

}
