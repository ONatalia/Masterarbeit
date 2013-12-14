/**
 * 
 */
package inpro.incremental.sink;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

/**
 * A dummy module that expects WordIUs and beeps upon the reception of a specific word.
 * @author das, timo
 */
public class WordCatcher extends PushBuffer {
	// Pushbuffer stuff
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	
	@S4String()
	public final static String PROP_WORD_TO_CATCH = "word";
	private String wordToCatch;
	//private boolean wordCaught = false;
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		Iterator<? extends EditMessage<? extends IU>> editIt = edits.iterator();
		while (editIt.hasNext()) { //  we know that the list contains the revokes first, and then the adds
			EditMessage<? extends IU> edit = editIt.next();
			if (edit.getType() == EditType.ADD) {
				String current_word = ((WordIU) edit.getIU()).getWord();
				if (current_word.equals("kreuz")) {
					// das: why catch only once?? Commented this out again.
					//if (!wordCaught) {
					//	wordCaught = true;
					java.awt.Toolkit.getDefaultToolkit().beep();
					System.out.println("Jeder nur ein " + wordToCatch + ", bitte!");
					//}
				}
			}/* else if (edit.getType().isCommit()) {
				wordCaught = false;
			}*/

		}
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		wordToCatch = ps.getString(PROP_WORD_TO_CATCH);
	}

}
