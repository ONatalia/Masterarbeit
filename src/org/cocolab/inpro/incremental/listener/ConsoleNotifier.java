package org.cocolab.inpro.incremental.listener;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class ConsoleNotifier implements Configurable, HypothesisChangeListener {

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		//
	}

	@Override
	public void hypChange(List<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (edits.size() > 0) {
			System.out.println("\nThe Hypothesis has changed.");
			System.out.println("Edits since last hypothesis:");
			for (EditMessage<? extends IU> edit : edits) {
				System.out.println(edit.toString());
			}
			System.out.println("Current hypothesis is now:");
			for (IU iu : ius) {
				System.out.println(iu.toString());
			}
		}
	}

	@Override
	public void reset() {
		// ignore
	}

}
