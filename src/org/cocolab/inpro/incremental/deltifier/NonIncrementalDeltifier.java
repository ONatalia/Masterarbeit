package org.cocolab.inpro.incremental.deltifier;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.WordIU;

public class NonIncrementalDeltifier extends ASRWordDeltifier {

	@Override
	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		if (recoFinal) {
			return super.getWordEdits();
		} else {
			wordEdits = Collections.<EditMessage<WordIU>>emptyList();
			return wordEdits;
		}
	}

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
