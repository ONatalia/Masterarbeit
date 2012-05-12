package inpro.incremental.unit;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/** 
 * an InstallmentIU that loosely uses phrases to structure its output.
 * phrases can be added even when the utterance is already being produced
 * (currently, phrases cannot be "revoked" from the installment as that's not necessary in our task)
 * @author timo
 */
public class PhraseBasedInstallmentIU extends IncrSysInstallmentIU {
	/** create a phrase from  */
	public PhraseBasedInstallmentIU(PhraseIU phrase) {
		// todo: think about a new method in PhraseIU that gets us more specifically tuned text for synthesis depending on phraseTypes
		super(phrase.toPayLoad());
		phrase.groundIn(new ArrayList<IU>(groundedIn));
		phrase.containingInstallment = this;
	}

	/** append words for this phrase at the end of the installment */
	public void appendPhrase(PhraseIU phraseIU) {
		IU oldLastWord = getFinalWord(); // everything that follows this word via fSLL belongs to the new phrase
		String fullPhrase = toPayLoad() + phraseIU.toPayLoad();
		fullPhrase = fullPhrase.replaceAll(" <sil>", ""); // it's nasty when there are silences pronounced as "kleiner als sil größer als"
//		addAlternativeVariant(fullPhrase);
		appendContinuation(phraseIU);
		phraseIU.containingInstallment = this;
		List<IU> phraseWords = new ArrayList<IU>(phraseIU.expectedWordCount());
		while (oldLastWord.getNextSameLevelLink() != null) {
			IU w = oldLastWord.getNextSameLevelLink();
			phraseWords.add(w);
			oldLastWord = w;
		}
		phraseIU.groundIn(phraseWords);
		groundedIn.addAll(phraseWords);
	}
	
	/** append a continuation to the ongoing installment <pre>
	// this works as follows: 
	// * we have linguistic preprocessing generate a full IU structure for both the base words and the continuation
	// * we then identify the words which are the continuation part of the full structure: 
	// * we append the continuation part to the last utterance of the IU
	// * we then move backwards in the lists of segments and copy over synthesis information to the old segments
	// we call this last step "back-substitution"
	 </pre>*/
	private void appendContinuation(PhraseIU phrase) {
		WordIU firstNewWord = null;
		if (System.getProperty("proso.cond.connect", "true").equals("true")) {
			String fullPhrase = toPayLoad() + phrase.toPayLoad();
			fullPhrase = fullPhrase.replaceAll(" <sil>", ""); // it's nasty when there are silences pronounced as "kleiner als sil größer als"
			@SuppressWarnings("unchecked")
			List<WordIU> newWords = (List<WordIU>) (new SysInstallmentIU(fullPhrase)).groundedIn();
			assert newWords.size() >= groundedIn.size();
//			assert newWords.size() == groundedIn.size() + phrase.expectedWordCount(); // for some reason, this assertion breaks sometimes
			firstNewWord = newWords.get(groundedIn.size());
		} else {
			firstNewWord = (WordIU) (new SysInstallmentIU(phrase.toPayLoad())).groundedIn().get(0);
		}
		WordIU lastOldWord = getFinalWord();
		//assert lastOldWord.payloadEquals(firstNewWord.getSameLevelLink());
		SysSegmentIU newSeg = (SysSegmentIU) firstNewWord.getFirstSegment().getSameLevelLink();
		firstNewWord.connectSLL(lastOldWord);
		// back substitution just copy over the HTSModel from the new segments to the old segments (and be done with it)
		backsubstituteHTSModels(newSeg, (SysSegmentIU) lastOldWord.getLastSegment());
	}
	
	private void backsubstituteHTSModels(SysSegmentIU newSeg,
			SysSegmentIU oldSeg) {
		if (newSeg != null && oldSeg != null && newSeg.payloadEquals(oldSeg) && oldSeg.isUpcoming()) {
			oldSeg.copySynData(newSeg);
			backsubstituteHTSModels((SysSegmentIU) newSeg.getSameLevelLink(), (SysSegmentIU) oldSeg.getSameLevelLink());
		}
	}

	/** breaks the segment links between words so that synthesis stops after the currently ongoing word */
	public void stopAfterOngoingWord() {
		ListIterator<IU> groundIt = groundedIn.listIterator(groundedIn.size());
		for (; groundIt.hasPrevious(); ) {
			WordIU word = (WordIU) groundIt.previous();
			// break the segmentIU layer
			SegmentIU seg = word.getLastSegment();
			seg.removeAllNextSameLevelLinks();
			// hack for long words:
			if (word.getSegments().size() > 7) {
				word.getSegments().get(5).removeAllNextSameLevelLinks();
			}
			if (seg.isCompleted()) 
				break; // no need to go on, as this the past already
		}
	}
	
}
