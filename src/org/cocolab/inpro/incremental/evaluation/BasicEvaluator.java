package org.cocolab.inpro.incremental.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

public abstract class BasicEvaluator extends PushBuffer {

	/** list of committed words */
	protected List<WordIU> committedWords = new ArrayList<WordIU>();
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		checkForCommits(edits);
	}
	
	@SuppressWarnings("unchecked")
	protected void checkForCommits(List<? extends EditMessage<? extends IU>> edits) {
		boolean committedFlag = false;
		for (EditMessage edit : edits) {
			if (edit.getType().isCommit()) {
				WordIU word = (WordIU) edit.getIU();
				committedWords.add(word);
				committedFlag = true;
			}
		}
		if (committedFlag)
			evaluate();		
	}
	
	protected abstract void evaluate();
	
}
