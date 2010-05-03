package org.cocolab.inpro.incremental.deltifier;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;

public class NonIncrementalDeltifier extends ASRWordDeltifier {

	/** 
	 * only if recognition is final, output the results of the
	 * underlying deltifier; otherwise return an empty list;
	 * which will then contain a list of all ADD edits for the
	 * words in the final result. COMMIT edits are automatically
	 * output by CurrentASRHypothesis  
	 */
	@Override
	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		if (recoFinal) {
			return super.getWordEdits();
		} else {
			wordEdits = Collections.<EditMessage<WordIU>>emptyList();
			return wordEdits;
		}
	}

	/** 
	 * only if recognition is final, output the results of the
	 * underlying deltifier; otherwise return an empty list
	 * which will then contain the words in the final result.
	 */
	@Override
	public synchronized List<WordIU> getWordIUs() {
		if (recoFinal) {
			return super.getWordIUs();
		} else {
			wordIUs = new IUList<WordIU>();
			return wordIUs;
		}
	}
	

}
