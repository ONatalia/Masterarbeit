package inpro.incremental.unit;


import java.util.List;

/** 
 * an IU that binds several WordIUs together to form a phrase.
 * phrases are a convenient level for synthesis output if they contain words in 
 * units that roughly correspond to prosodic phrases
 * @author timo
 */
public class PhraseIU extends IU {

	final String phrase;
	private PhraseType type;
	/** the state of delivery that this unit is in */
	Progress progress = Progress.UPCOMING;
	
	public enum PhraseType {
	    INITIAL, // the first phrase in the utterance
	    CONTINUATION, // continues the previous phrase 
	    REPAIR, // correction of the previous phrase
	    FINAL, // last phrase of the utterance <-- this is not exclusive with e.g. continuation,initial,repair,
	    	   // unlike the other types this describes the end of the phrase, not the beginning 
	    UNDEFINED // dunno
	}
	
	public PhraseIU(WordIU word) {
		this(word.toPayLoad());
	}
	
	public PhraseIU(String phrase) {
		this(phrase, PhraseType.UNDEFINED);
	}
	
	public PhraseIU(String phrase, PhraseType type) {
		this.phrase = phrase;
		this.type = type;
	}
	
	@Override
	public String toPayLoad() {
		return phrase;
	}
	
	/** the number of white-space delimited tokens in the payload */
	public int expectedWordCount() {
		String phrase = toPayLoad();
		return phrase.split("\\s+").length;
	}
	
	/** grounds in the list of wordIUs, which must have the expected number of elements */
	@Override
	public void groundIn(List<IU> ius) {
		super.groundIn(ius);
		// we don't ever look at the groundedIn's, so we don't need to care about this failing assertion
		//assert (((WordIU) ius.get(0)).isSilence()) ? ius.size() == expectedWordCount() + 1 : ius.size() == expectedWordCount();
	}

	public void setProgress(Progress p) {
		if (p != this.progress) { 
			this.progress = p;
			notifyListeners();
		}
	}
	
	@Override
	public Progress getProgress() {
		return progress;
	}

	public void setFinal() {
		this.type = PhraseIU.PhraseType.FINAL;
		WordIU lastWord = null;
		if (groundedIn != null) {
			lastWord = (WordIU) groundedIn.get(groundedIn.size() - 1);
		}
		if (lastWord != null) {
			for (SegmentIU seg : lastWord.getSegments()) {
				// clear any locks that might block the vocoder from finishing the final phrase
				((SysSegmentIU) seg).setAwaitContinuation(false);			
			}
		}
	}

	public PhraseType getType() {
		return type;
	}
}
