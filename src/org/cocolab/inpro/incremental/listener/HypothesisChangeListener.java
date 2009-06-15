package org.cocolab.inpro.incremental.listener;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.Configurable;

public interface HypothesisChangeListener extends Configurable, Resetable {

	/*
	 * this should receive a list of current IUs and 
	 * a list of edit messages since the last call to hypChange
	 */
	public void hypChange(List<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits);
	
}
