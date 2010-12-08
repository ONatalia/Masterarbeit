package org.cocolab.inpro.incremental.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;

public class WordLagTracker extends PushBuffer {

	final static Map<WordIU, Long> wordLagsForAllWords;
	
	static {
		wordLagsForAllWords = new HashMap<WordIU, Long>();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
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
			}
		});
	}

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
    	for (IU iu : ius) {
    		if (!wordLagsForAllWords.containsKey(iu) && iu instanceof WordIU) {
    			long delay = iu.getCreationTime() - ((long) (iu.endTime() * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
    			wordLagsForAllWords.put((WordIU) iu, delay);
    		}
    	}
		
	}

}
