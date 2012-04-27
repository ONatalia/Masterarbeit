package inpro.incremental.deltifier;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.WordIU;

import java.util.List;


/**
 * A deltifier which does not output any intermediate hypotheses.
 * Output IUs are only provided when the ASR result is final.
 * Thus, this deltifier effectively "switches off" incrementality.
 * 
 * @author timo
 */
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
			wordEdits.clear();
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
			wordIUs.clear();
			return wordIUs;
		}
	}
	

}
