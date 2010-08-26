/**
 * 
 */
package org.cocolab.inpro.incremental.processor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

/**
 * @author das
 *
 */
public class WordCatcher extends PushBuffer {
	// Pushbuffer stuff
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	List<PushBuffer> listeners;
	
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
			}/* else if (edit.getType() == EditType.COMMIT) {
				wordCaught = false;
			}*/

		}
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		listeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
		wordToCatch = (String) ps.getString(PROP_WORD_TO_CATCH);
	}

}
