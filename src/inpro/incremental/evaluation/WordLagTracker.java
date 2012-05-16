package inpro.incremental.evaluation;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * a listener that outputs all WordIUs' lags (relative to the word's end); 
 * this may become an alternative to use INTELIDA to analyze runtime performance 
 * (or could even be used online to adapt ASR or SDS properties)
 * 
 * @author timo
 */
public class WordLagTracker extends BasicEvaluator {

	final static Map<WordIU, Long> wordLagsForAllWords = new HashMap<WordIU, Long>();

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
    	for (IU iu : ius) {
    		if (!wordLagsForAllWords.containsKey(iu) && iu instanceof WordIU) {
    			long delay = iu.getCreationTime() - ((long) (iu.endTime() * TimeUtil.SECOND_TO_MILLISECOND_FACTOR));
    			wordLagsForAllWords.put((WordIU) iu, delay);
    		}
    	}
		checkForCommits(edits);
	}

	@Override
	protected void evaluate() {
		List<IU> words = new ArrayList<IU>(wordLagsForAllWords.keySet());
		Collections.sort(words);
		StringBuilder sb = new StringBuilder("{");
		for (Iterator<IU> it = words.iterator(); it.hasNext(); ) {
			IU iu = it.next();
			sb.append(iu);
			sb.append("=");
			sb.append(wordLagsForAllWords.get(iu));
			if (it.hasNext())
				sb.append(",\n");
		}
		sb.append("}");
		System.err.println(sb);
		wordLagsForAllWords.clear();
	}

}
