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
package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class IUList<IUType extends IU> extends ArrayList<IUType> {

	public IUList() {
		super();
	}
	
	public IUList(List<IUType> base) {
		super(base);
	}
	
 	public void apply(EditMessage<IUType> edit) {
 		switch (edit.type) {
 			case ADD: 
 				assert get(size() - 1).endTime() <= edit.getIU().endTime() : "better sort your IUs: " + this + edit;
 				this.add(edit.getIU()); 
 				break;
 			case REVOKE: 
 				assert (get(size() - 1)).equals(edit.getIU()) : "Can't apply this edit to the list: " + this + edit;
 				this.remove(size() - 1);
 				break;
 			case COMMIT:
 				// don't do anything on commit
 				break;
 			default:
 				throw new RuntimeException("If you implement new EditTypes, you should also implement their handling!");
 		}
 	}
 	
 	public void apply(List<EditMessage<IUType>> edits) {
		for (EditMessage<IUType> edit : edits) {
			apply(edit);
		}
	}
 	
 	public void add(IUType e, boolean deepSLL) {
 		if (deepSLL) {
 			e.connectSLL(get(size()));
 		} else {
 			e.setSameLevelLink(get(size()));
 		}
 		add(e);
 	}
	
}
