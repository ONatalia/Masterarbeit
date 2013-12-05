package inpro.incremental.unit;


import inpro.synthesis.MaryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.StringUtils;


/** 
 * an synthesizable InstallmentIU that uses phrases to structure its output.
 * phrases can be added even when the utterance is already being produced
 * (currently, phrases cannot be "revoked" from the installment as that wasn't necessary in our tasks yet)
 * @author timo
 */
public class PhraseBasedInstallmentIU extends SysInstallmentIU {
	/** counts the hesitations in this installment (which need to be accounted for when counting the continuation point of a resynthesis when appending a continuation */
	private int numHesitationsInserted = 0;
	/** keeps the list of phrases that this installment is based on */
	private final IUList<PhraseIU> phrases = new IUList<PhraseIU>();
	
	public PhraseBasedInstallmentIU(HesitationIU hesitation) {
		super("<hes>", new ArrayList<WordIU>(Collections.<WordIU>singletonList((WordIU)hesitation)));
		numHesitationsInserted++;
		phrases.add(hesitation);
	}
	
	/** create a phrase from  */
	public PhraseBasedInstallmentIU(PhraseIU phrase) {
		super(phrase.toPayLoad(), phrase.getWords());
		// if the phrase that we're starting from isn't yet pre-synthesized, we have to build the IU structure
		if (phrase.getWords() == null) {
			groundedIn = MaryAdapter.getInstance().text2IUs(tts);
		} else {
			groundedIn = new ArrayList<IU>(groundedIn); // ensure that the grounded-in lists aren't shared between InstallmentIU and PhraseIU!
		}
		phrase.groundIn(groundedIn);	
		phrases.add(phrase);
	}

	/** append words for this phrase at the end of the installment */
	public void appendPhrase(PhraseIU phrase) {
		WordIU oldLastWord = getFinalWord(); // everything that follows this word via fSLL belongs to the new phrase
		List<IU> phraseWords = new ArrayList<IU>();
		if (phrase instanceof HesitationIU) {
			phrase.shiftBy(oldLastWord.getLastSegment().endTime());
			phraseWords.add(phrase);
			oldLastWord.getLastSegment().addNextSameLevelLink(phrase.getFirstSegment());
			oldLastWord.addNextSameLevelLink(phrase);
			numHesitationsInserted++;
		} else {
			appendContinuation(phrase);
			while (oldLastWord.getNextSameLevelLink() != null) {
				IU w = oldLastWord.getNextSameLevelLink();
				phraseWords.add(w);
				oldLastWord = (WordIU) w;
			}
			phrase.groundIn(phraseWords);
		}
		groundedIn.addAll(phraseWords);
		phrase.setSameLevelLink(phrases.getLast());
		phrases.add(phrase);
	}
	
	/** append a continuation to the ongoing installment. 
	 * this works as follows: <pre>
	 * we have linguistic preprocessing generate a full IU structure for both the base words and the continuation
	 * we then identify the words which are the continuation part of the full structure: 
	 * we append the continuation part to the last utterance of the IU
	 * we then move backwards in the lists of segments and copy over synthesis information to the old segments
	 * we call this last step "back-substitution"
	 </pre>*/
	private void appendContinuation(PhraseIU phrase) {
		WordIU firstNewWord = null;
		if (System.getProperty("proso.cond.connect", "true").equals("true")) {
			List<String> phraseTexts = new ArrayList<String>();
			// move back to the first phraseIU of the installment
			for (PhraseIU oldPhrase : phrases) {
				phraseTexts.add(oldPhrase.toPayLoad());
			}
			phraseTexts.add(phrase.toPayLoad());
			String fullInstallmentText = StringUtils.join(phraseTexts, " "); 
			//String fullPhrase = toPayLoad() + phrase.toPayLoad();
			logger.debug("querying MaryTTS for: " + fullInstallmentText);
			fullInstallmentText = fullInstallmentText.replaceAll(" <sil>", ""); // it's nasty when there are silences pronounced as "kleiner als sil größer als"
			fullInstallmentText = fullInstallmentText.replaceAll(" *<hes>", ""); // ... or hesitations as "kleiner als hes größer als"
			@SuppressWarnings("unchecked")
			List<WordIU> newWords = (List<WordIU>) (new SysInstallmentIU(fullInstallmentText)).groundedIn();
			logger.debug("received the words" + newWords.toString());
			assert newWords.size() >= groundedIn.size() - numHesitationsInserted;
//			assert newWords.size() == groundedIn.size() + phrase.expectedWordCount(); // for some reason, this assertion breaks sometimes -> nasty stuff going on with pauses
			firstNewWord = newWords.get(groundedIn.size() - numHesitationsInserted);
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
	
	/**
	 * recursively walk backwards on the segment layer, replacing older segments with newer segments.
	 * Segments that have been generated more recently are potentially of better quality and should be used 
	 * to replace old segments (which have been generated with other or less context) wherever possible
	 * (i.e., if their synthesis hasn't started yet). 
	 */
	private void backsubstituteHTSModels(SysSegmentIU newSeg, SysSegmentIU oldSeg) {
		if (newSeg != null && oldSeg != null && newSeg.payloadEquals(oldSeg) && oldSeg.isUpcoming()) {
			oldSeg.copySynData(newSeg);
			backsubstituteHTSModels((SysSegmentIU) newSeg.getSameLevelLink(), (SysSegmentIU) oldSeg.getSameLevelLink());
		}
	}
	
	public void revokePhrase(PhraseIU phrase) {
		assert phrase.isUpcoming();
//		SegmentIU seg = ((WordIU) phrase.groundedIn()).getSegments().get(0);
//		seg.getSameLevelLink().removeAllNextSameLevelLinks();
		if (phrase instanceof HesitationIU) {
			numHesitationsInserted--;
		}
		for (WordIU word : phrase.getWords()) {
			word.setSameLevelLink(null);
			word.removeAllNextSameLevelLinks();
			groundedIn.remove(word);
			//word.revoke();
			for (SegmentIU seg : word.getSegments()) {
				seg.setSameLevelLink(null);
				seg.removeAllNextSameLevelLinks();
			}
		}
		//System.err.println(phrase.deepToString());
		/*
		if (revokedIU.isUpcoming()){
			if (revokedIU instanceof WordIU) {
				SegmentIU seg = ((WordIU)revokedIU).getFirstSegment();
				seg.getSameLevelLink().removeAllNextSameLevelLinks(); //FIXME: this is too eager, it would be much better to only remove seg from nextSLL!
			} else {
				throw new NotImplementedException("and now you're revoking an IU that I cannot handle.");
			}
		} else { // warn if revoke comes too late.
			if (revokedIU.isOngoing()) 
				logger.warn("SynthesisModule: so far, I'm unable to revoke ongoing IUs; sorry about that; check for our next release.");
			else
				logger.warn("SynthesisModule: you asked me to revoke a completed IU. I'm unable to change the past.");
		} */
	}

	/** breaks the segment links between words so that crawling synthesis stops after the currently ongoing word */
	public void stopAfterOngoingWord() {
		ListIterator<IU> groundIt = groundedIn.listIterator(groundedIn.size());
		for (; groundIt.hasPrevious(); ) {
			WordIU word = (WordIU) groundIt.previous();
			if (word.isCompleted()) {
				break;
			}
			// break the segmentIU layer
			SegmentIU seg = word.getLastSegment();
			seg.removeAllNextSameLevelLinks();
			// hack for long words: also stop in the middle of word
			if (word.getSegments().size() > 7) {
				word.getSegments().get(5).removeAllNextSameLevelLinks();
			}
			if (seg.isCompleted()) 
				break; // no need to go on, as this the past already
		}
	}
	
}
