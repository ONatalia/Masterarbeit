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
package org.cocolab.inpro.incremental.deltifier;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.WordIU;

/** allows to query the the current state of ASR input */
public interface ASRResultKeeper {

	/** 
	 * return a list of currently valid edits between 
	 * the last update of internal state and the one preceding that state. 
	 */
	public List<EditMessage<WordIU>> getWordEdits();

	/**
	 * return a list of currently valid WordIUs
	 */
	public List<WordIU> getWordIUs();
	
	/**
	 * return the frame of the {@link edu.cmu.sphinx.result.Result}
	 * that the current state is based on 
	 */
	public int getCurrentFrame();
	
	public double getCurrentTime();

}
