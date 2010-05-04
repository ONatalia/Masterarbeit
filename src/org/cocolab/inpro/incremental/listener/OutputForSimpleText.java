package org.cocolab.inpro.incremental.listener;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

public class OutputForSimpleText extends HypothesisChangeListener {
	
	PrintStream outputStream = System.out;
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (ius.size() > 0) {
			boolean commitFlag = false;
			for (EditMessage<?> e : edits) {
				switch (e.getType()) {
				case REVOKE: 
					outputStream.print("!1 "); 
					break; 
				case ADD: 
					outputStream.print(((WordIU) e.getIU()).getWord() + " ");
					break;
				case COMMIT: 
					commitFlag = true;
					break;
				default:
					assert false : "You defined a new edit type without telling me!";
				}		
			}
			if (commitFlag) {
				outputStream.println();
				outputStream.println();
			}
		}
	}

}
