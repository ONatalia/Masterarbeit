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

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

public class ConsoleNotifier extends HypothesisChangeListener {

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (edits.size() > 0) {
			System.out.print("\nThe Hypothesis has changed at time: ");
			System.out.println(currentFrame * 0.01);
			System.out.println("Edits since last hypothesis:");
			for (EditMessage<? extends IU> edit : edits) {
				System.out.println(edit.toString());
			}
			System.out.println("Current hypothesis is now:");
			for (IU iu : ius) {
				System.out.println(iu.deepToString());
			} 
		} else {
			System.out.print(".");
		}
	}

}
