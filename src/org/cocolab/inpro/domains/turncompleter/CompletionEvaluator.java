package org.cocolab.inpro.domains.turncompleter;

import java.util.ArrayList;
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
	
	private List<WordIU> wordsToEvaluate = new ArrayList<WordIU>();
	
	public void newOnsetResult(WordIU word, double onsetTime, int frameCount, WordIU nextWord, double nextWordEndEstimate) {
		onsetResults.add(new EvalEntry(word, onsetTime, frameCount, nextWord, nextWordEndEstimate));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		boolean committedFlag = false;
		for (EditMessage edit : edits) {
			if (edit.getType().equals(EditType.COMMIT)) {
				WordIU word = (WordIU) edit.getIU();
				wordsToEvaluate.add(word);
				committedFlag = true;
			}
		}
		if (committedFlag)
			evaluate();
	}
	
	private void evaluate() {
		for (int i = 0; i < wordsToEvaluate.size(); i++) {
			WordIU word = wordsToEvaluate.get(i);
			WordIU nextWord = i + 1 < wordsToEvaluate.size() ? wordsToEvaluate.get(i + 1) : null;
			System.err.print("!GREPME!\t");
			// add all reference word info here, only eval-stuff goes into the while-loop below
			System.err.print(String.format(Locale.ENGLISH, 
								"%s wstart:%.3f wend:%.3f nend:%.3f\t", 
								word.getWord(), 
								word.startTime(), 
								word.endTime(), 
								nextWord != null && !nextWord.isSilence() ? nextWord.endTime() : Double.NaN));
			while (!onsetResults.isEmpty() && onsetResults.peek().triggerWord.spellingEquals(word)) {
				EvalEntry evalEntry = onsetResults.poll();
				Double nextWordEstimation = Double.NaN;
				if (evalEntry.nextWord.spellingEquals(nextWord)) {
					nextWordEstimation = evalEntry.nextWordEndEstimate;
				}
				System.err.print(
					String.format(Locale.ENGLISH, 
						"(wstart:%.3f\tdec:%.3f\test:%.3f\tnest:%.3f) ",
						evalEntry.triggerWord.startTime(), // start time of the triggering word *when the decision was taken* (this may change during recognition)
						evalEntry.logicalTimeOfDecision(),
						evalEntry.estimatedOnsetTime(), // of the next word
						nextWordEstimation // estimation of when the next word ends
					)
				);
			}
			System.err.println();
		}
		wordsToEvaluate.clear();
	}
	
	private class EvalEntry {
		WordIU triggerWord;
		double onset; // the time at which we believe the trigger word to be over
		int frameCount; // the time at which this decision was taken
		WordIU nextWord;
		double nextWordEndEstimate;
		
		private EvalEntry(WordIU trigger, double onset, int frameCount, WordIU nextWord, double nextWordEndEstimate) {
			this.triggerWord = trigger;
			this.onset = onset;
			this.frameCount = frameCount;
			this.nextWord = nextWord;
			this.nextWordEndEstimate = nextWordEndEstimate;
		}
		
		/** in seconds */
		@SuppressWarnings("unused")
		private double logicalTimeUntilOnset() {
			return onset - (frameCount * ResultUtil.FRAME_TO_SECOND_FACTOR);
		}
		
		@SuppressWarnings("unused")
		private double wordendUntilOnset(WordIU word) {
			return onset - word.endTime();
		}
		
		private double logicalTimeOfDecision() {
			return ((double) frameCount) * ResultUtil.FRAME_TO_SECOND_FACTOR;
		}
		
		private double estimatedOnsetTime() {
			return onset;
		}
		
		@Override
		public String toString() {
			return triggerWord.toString() + ", frames: "+ frameCount + ", onset: " + onset;
		}
	}
	
}
