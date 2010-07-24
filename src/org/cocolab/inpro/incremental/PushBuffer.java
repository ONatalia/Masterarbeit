package org.cocolab.inpro.incremental;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public abstract class PushBuffer implements Resetable, Configurable {

	/**
	 * this should receive a list of current IUs and 
	 * a list of edit messages since the last call to hypChange
	 * 
	 * @param ius while this is a (plain) collection, the collection's iterator()
	 *        method must ensure a sensible ordering of the returned elements.
	 *        For now we have only used Lists (which are ordered), 
	 *        but a Tree of IUs should also be possible and this should gracefully
	 *        work together with processors that expect lists 
	 * @param edits a list of edits since the last call to hypChange
	 */
	public abstract void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits);

	@Override
	public void reset() {
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
	}

}
