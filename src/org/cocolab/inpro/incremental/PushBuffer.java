package org.cocolab.inpro.incremental;

import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

public interface PushBuffer {

	/*
	 * this should receive a list of current IUs and 
	 * a list of edit messages since the last call to hypChange
	 */
	public abstract void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits);


}
