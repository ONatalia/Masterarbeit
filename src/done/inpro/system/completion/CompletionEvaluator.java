package done.inpro.system.completion;

import inpro.incremental.evaluation.BasicEvaluator;
import inpro.incremental.unit.WordIU;
import inpro.util.TimeUtil;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


/**
 * receives timing predictions from the TurnCompleter and evalutes 
 * them once these words are committed
 */
public class CompletionEvaluator extends BasicEvaluator {

	private final Queue<EvalEntry> onsetResults = new LinkedList<EvalEntry>();
	
	public void newOnsetResult(WordIU word, double onsetTime, int frameCount, WordIU nextWord, double nextWordEndEstimate, double scalingFactor) {
		onsetResults.add(new EvalEntry(word, onsetTime, frameCount, nextWord, nextWordEndEstimate, scalingFactor));
	}

	protected void evaluate() {
		for (int i = 0; i < committedWords.size(); i++) {
			WordIU word = committedWords.get(i);
			WordIU nextWord = i + 1 < committedWords.size() ? committedWords.get(i + 1) : null;
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
						"(wstart:%.3f\tdec:%.3f\test:%.3f\tnest:%.3f\tscal:%.3f) ",
						evalEntry.triggerWord.startTime(), // start time of the triggering word *when the decision was taken* (this may change during recognition)
						evalEntry.logicalTimeOfDecision(),
						evalEntry.estimatedOnsetTime(), // of the next word
						nextWordEstimation, // estimation of when the next word ends
						evalEntry.ttsScaling
					)
				);
			}
			System.err.println();
		}
		committedWords.clear();
	}
	
	private class EvalEntry {
		WordIU triggerWord;
		double onset; // the time at which we believe the trigger word to be over
		int frameCount; // the time at which this decision was taken
		double ttsScaling; // the scaling factor applied in TTS-based micro-timing estimations
		WordIU nextWord;
		double nextWordEndEstimate;
		
		private EvalEntry(WordIU trigger, double onset, int frameCount, WordIU nextWord, double nextWordEndEstimate, double scalingFactor) {
			this.triggerWord = trigger;
			this.onset = onset;
			this.frameCount = frameCount;
			this.nextWord = nextWord;
			this.nextWordEndEstimate = nextWordEndEstimate;
			this.ttsScaling = scalingFactor;
		}
		
		/** in seconds */
		@SuppressWarnings("unused")
		private double logicalTimeUntilOnset() {
			return onset - (frameCount * TimeUtil.FRAME_TO_SECOND_FACTOR);
		}
		
		@SuppressWarnings("unused")
		private double wordendUntilOnset(WordIU word) {
			return onset - word.endTime();
		}
		
		private double logicalTimeOfDecision() {
			return frameCount * TimeUtil.FRAME_TO_SECOND_FACTOR;
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
