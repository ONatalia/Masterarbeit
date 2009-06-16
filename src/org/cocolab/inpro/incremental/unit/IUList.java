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
 				this.add(edit.getIU()); 
 				break;
 			case REVOKE: 
 				assert (get(size() - 1)).equals(edit.getIU());
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
	
}
