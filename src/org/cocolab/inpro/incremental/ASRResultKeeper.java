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
package org.cocolab.inpro.incremental;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.WordIU;

public interface ASRResultKeeper {

	public List<EditMessage<WordIU>> getEdits();

	public List<WordIU> getIUs();
	
	public int getCurrentFrame();
	
	public double getCurrentTime();

}