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
		if (edit.type == EditType.ADD) {
			this.add(edit.getIU());
		} else if (edit.type == EditType.REVOKE) {
			assert (get(size() - 1)).equals(edit.getIU());
			this.remove(size() - 1);
		} else if (edit.type == EditType.COMMIT) {
			// don't do anything on commit
		} else {
			throw new RuntimeException("If you implement new EditTypes, you should also implement their handling!");
		} 		
 	}
 	
 	public void apply(List<EditMessage<IUType>> edits) {
		for (EditMessage<IUType> edit : edits) {
			apply(edit);
		}
	}
	
}
