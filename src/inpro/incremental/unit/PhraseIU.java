package inpro.incremental.unit;


import inpro.synthesis.MaryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 
 * an IU that binds several WordIUs together to form a phrase.
 * phrases are a convenient level for synthesis output if they contain words in 
 * units that roughly correspond to prosodic phrases
 * @author timo
 */
public class PhraseIU extends WordIU {

	final String phrase;
	private PhraseType type;
	/** the state of delivery that this unit is in */
	Progress progress = null;
	
	public enum PhraseType {
	    NONFINAL, // we're still awaiting more content in this utterance
	    FINAL, // last phrase of the utterance <-- this is not exclusive with e.g. continuation,initial,repair,
	    	   // unlike the other types this describes the end of the phrase, not the beginning 
	    UNDEFINED // dunno
	}
	
	public PhraseIU(WordIU word) {
		this(word.toPayLoad());
		word.groundIn(this);
		this.setFinal();
	}
	
	public PhraseIU(String phrase) {
		this(phrase, PhraseType.UNDEFINED);
	}
	
	public PhraseIU(String phrase, PhraseType type) {
		super(phrase, null, null);
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
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) // the untyped list in the call to Collections.checkedList
	public List<WordIU> getWords() {
		if (groundedIn != null)
			return Collections.checkedList((List) groundedIn, WordIU.class);
		else
			return null;
	}
	
	/** grounds in the list of wordIUs, potentially replacing previously grounding words */
	@Override
	public void groundIn(List<IU> ius) {
		if (groundedIn != null)
			groundedIn.clear();
		else 
			groundedIn = new ArrayList<IU>();
		groundedIn.addAll(ius);
	}

	public void setProgress(Progress p) {
		if (p != this.progress) { 
			this.progress = p;
			notifyListeners();
		}
	}
	
	public void preSynthesize() {
		assert previousSameLevelLink == null : "You shouldn't pre-synthesize something that is already connected";
		assert groundedIn == null : "You shouldn't pre-synthesize something that is already connected";
		groundedIn = MaryAdapter.getInstance().text2IUs(this.phrase);
	}
	
	@Override
	public Progress getProgress() {
		return progress != null ? progress : super.getProgress();
	}

	public void setFinal() {
		this.type = PhraseIU.PhraseType.FINAL;
		for (SegmentIU seg : getSegments()) {
			((SysSegmentIU) seg).setAwaitContinuation(false);			
		}
//		WordIU lastWord = null;
//		if (groundedIn != null) {
//			lastWord = (WordIU) groundedIn.get(groundedIn.size() - 1);
//		}
//		if (lastWord != null) {
//			for (SegmentIU seg : lastWord.getSegments()) {
//				// clear any locks that might block the vocoder from finishing the final phrase
//				((SysSegmentIU) seg).setAwaitContinuation(false);			
//			}
//		}
	}

	public PhraseType getType() {
		return type;
	}
}
