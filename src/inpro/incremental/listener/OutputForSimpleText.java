package inpro.incremental.listener;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;


public class OutputForSimpleText extends PushBuffer {
	
	PrintStream outputStream = System.out;
	String outputString = "";
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (ius.size() > 0) {
			boolean commitFlag = false;
			for (EditMessage<?> e : edits) {
				switch (e.getType()) {
				case REVOKE: 
					outputString += "!1 ";
//					outputStream.print("!1 "); 
					break; 
				case ADD: 
					outputString += ((WordIU) e.getIU()).getWord() + " ";
//					outputStream.print(((WordIU) e.getIU()).getWord() + " ");
					break;
				case COMMIT: 
					commitFlag = true;
					break;
				default:
					assert false : "You defined a new edit type without telling me!";
				}		
			}
			if (commitFlag) {
				outputStream.print("For SimpleText: " + outputString);
				outputStream.println();
				outputString = "";
			}
		}
	}

}
