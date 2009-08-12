/* 
 * Copyright 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.incremental.listener;

import java.util.Collection;
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
	double currentTime = 0.0;

	/*
	 * this should receive a list of current IUs and 
	 * a list of edit messages since the last call to hypChange
	 */
	public abstract void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits);

	public void hypChange(ASRResultKeeper deltifier) {
		List<EditMessage<WordIU>> edits = deltifier.getWordEdits();
		List<WordIU> ius = deltifier.getWordIUs();
		currentFrame = deltifier.getCurrentFrame();
		currentTime = deltifier.getCurrentTime(); 
		if (ius != null && edits != null) 
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
