package org.cocolab.inpro.incremental.listener;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

public class LabelWriter extends HypothesisChangeListener {

	@Override
	public void hypChange(List<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (ius.size() > 0) {
			System.out.print("Time: ");
			System.out.println(currentFrame * 0.01);
			for (IU iu : ius) {
				System.out.print(iu.toString());
			}
			System.out.println();
		}
	}

}
