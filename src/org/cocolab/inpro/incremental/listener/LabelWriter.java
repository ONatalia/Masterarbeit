package org.cocolab.inpro.incremental.listener;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class LabelWriter implements Configurable, HypothesisChangeListener {

	int step = 0;

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		//
	}
	
	@Override
	public void hypChange(List<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (ius.size() > 0) {
			System.out.print("Time: ");
			System.out.println(step / 100.0);
			for (IU iu : ius) {
				System.out.print(iu.toString());
			}
			System.out.println();
		}
		step++;
	}

	@Override
	public void reset() {
		step = 0;
	}

}
