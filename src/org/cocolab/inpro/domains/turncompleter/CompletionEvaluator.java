package org.cocolab.inpro.domains.turncompleter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;

public class CompletionEvaluator extends PushBuffer {

	private Queue<EvalEntry> onsetResults = new LinkedList<EvalEntry>();
	
	public void newOnsetResult(WordIU word, double onsetTime, int frameCount) {
		onsetResults.add(new EvalEntry(word, onsetTime, frameCount));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage edit : edits) {
			if (edit.getType().equals(EditType.COMMIT)) {
				WordIU word = (WordIU) edit.getIU();
				System.err.print("!GREPME!\t");
				System.err.print(word.getWord() + "\t");
				while (!onsetResults.isEmpty() && onsetResults.peek().triggerWord.spellingEquals(word)) {
					EvalEntry evalEntry = onsetResults.poll();
//					System.err.println("Word " + word.getWord() + 
//								   " ended at "	+ word.endTime() + 
//								   " and expected next onset was " + expectedOnset);
					System.err.print(String.format(Locale.ENGLISH, "%.3f\t%.3f\t", 
										evalEntry.logicalTimeUntilOnset(), 
										evalEntry.wordendUntilOnset(word)));
				}
				System.err.println();
			}
		}
	}
	
	private class EvalEntry {
		WordIU triggerWord;
		double onset;
		int frameCount;
		
		private EvalEntry(WordIU trigger, double onset, int frameCount) {
			this.triggerWord = trigger;
			this.onset = onset;
			this.frameCount = frameCount;
		}
		
		/** in seconds */
		private double logicalTimeUntilOnset() {
			return onset - (frameCount * ResultUtil.FRAME_TO_SECOND_FACTOR);
		}
		
		private double wordendUntilOnset(WordIU word) {
			return onset - word.endTime();
		}
	}
	
}
