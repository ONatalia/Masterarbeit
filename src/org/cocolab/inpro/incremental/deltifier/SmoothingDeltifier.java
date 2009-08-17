/* 
 * Copyright 2008, 2009, Timo Baumann and the Inpro project
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

public class SmoothingDeltifier extends ASRWordDeltifier {

    @S4Integer(defaultValue = 0)
	public final static String PROP_SMOOTHING = "smoothing";
	int smoothing;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		smoothing = ps.getInt(PROP_SMOOTHING);
	}
	
	List<SmoothingCounter> smoothingQueue = new LinkedList<SmoothingCounter>();
	
	@Override
	protected synchronized void deltify(Token token) {
		// keep a copy of the original wordIUs
		IUList<WordIU> prevWordIUs = new IUList<WordIU>(wordIUs);
		// calculate would-be edits the standard way
		super.deltify(token); 
		if (!recoFinal) {
			// decrease smoothing-counter in all matching enqueued edits in the smoothingQueue
			// stop as soon as the new and enqueued edits don't match anymore 
			Iterator<EditMessage<WordIU>> editsIter = wordEdits.iterator();
			Iterator<SmoothingCounter> smoothIter = smoothingQueue.iterator();
			EditMessage<WordIU> edit = null;
			while (smoothIter.hasNext() && editsIter.hasNext()) {
				SmoothingCounter sc = smoothIter.next();
				edit = editsIter.next();
				if (sc.matches(edit)) {
					sc.count--;
				} else {
					smoothIter.remove();
					break;
				}
				edit = null;
			}
			// now deal with non-matching edits (kill remaining entries in smoothingQueue, enqueue for new edits)
			while (smoothIter.hasNext()) {
				smoothIter.next();
				smoothIter.remove();
			}
			if (edit != null) 
					smoothingQueue.add(new SmoothingCounter(edit));
			while (editsIter.hasNext()) {
				smoothingQueue.add(new SmoothingCounter(editsIter.next()));
			}
			// finally, apply edits from smoothingQueue if their counter has run out, 
			// add them to edit list and update wordIUs
			smoothIter = smoothingQueue.iterator();
			wordEdits = new LinkedList<EditMessage<WordIU>>();
			wordIUs = prevWordIUs;
			while (smoothIter.hasNext()) {
				SmoothingCounter sc = smoothIter.next();
				if (sc.count <= 0) {
					wordIUs.apply(sc.edit);
					wordEdits.add(sc.edit);
					smoothIter.remove();
				} else {
					break;
				}
			}
		}
	}

	private class SmoothingCounter {
		EditMessage<WordIU> edit;
		int count;
		
		SmoothingCounter(EditMessage<WordIU> edit) {
			this.edit = edit;
			count = smoothing;
		}
		
		public String toString() {
			return edit.toString() + "(" + count + ")"; 
		}
		
		boolean matches(EditMessage<WordIU> edit) {
			return this.edit.getIU().wordEquals(edit.getIU());
		}
	}
	
}
