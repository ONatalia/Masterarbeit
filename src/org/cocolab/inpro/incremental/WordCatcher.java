/**
 * 
 */
package org.cocolab.inpro.incremental;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cocolab.inpro.incremental.listener.HypothesisChangeListener;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;

/**
 * @author das
 *
 */
public class WordCatcher implements Configurable, PushBuffer {
	// Pushbuffer stuff
	@S4ComponentList(type = HypothesisChangeListener.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	List<PushBuffer> listeners;
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		Iterator<? extends EditMessage<? extends IU>> editIt = edits.iterator();
		while (editIt.hasNext()) { //  we know that the list contains the revokes first, and then the adds
			EditMessage<? extends IU> edit = editIt.next();
			if (edit.getType() == EditType.ADD) {
				String current_word = ((WordIU) edit.getIU()).getWord();
				//System.out.println(current_word);
				if (current_word.equals("kreuz")) {
					System.out.println("Jeder nur ein Kreuz, bitte!");
				}
			}
		}
	}
	

	@Override
	public void reset() {
		
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		listeners = (List<PushBuffer>) ps.getComponentList(PROP_HYP_CHANGE_LISTENERS);
	}

}
