package org.cocolab.inpro.incremental.deltifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.incremental.util.ResultUtil;
import org.cocolab.inpro.incremental.util.WordUtil;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


/**
 * an ASRWordDeltifier (and descendants) can be used 
 * to store the ASR's (or one of the ASR's) current hypothesis
 * 
 * you would usually call deltify() (either on the result or a specific token from the n-best-list)
 * and afterwards retrieve the WordIUs and edits via getIUs() and getEdits()
 * 
 * Descendents of this class implement methods described in NAACL-HLT 2009
 * 
 * @author Timo Baumann
 */
public class ASRWordDeltifier implements Configurable, Resetable, ASRResultKeeper, SignalListener {

	final IUList<WordIU> wordIUs = new IUList<WordIU>();
	
	final List<EditMessage<WordIU>> wordEdits = new ArrayList<EditMessage<WordIU>>();
	
	private static final Logger logger = Logger.getLogger(ASRWordDeltifier.class);
	
	int currentFrame = 0;
	int currentOffset = 0; // measured in centiseconds / frames (since when ?)
	long startTime = 0;
	
	/** flag to avoid smoothing or fixed lags on final result */
	protected boolean recoFinal;
	
	/** this base implementation does not accept any parameters */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
	}
	
	/**
	 * adds a bunch of assertions to the output of {@link ResultUtil#getTokenList(Token,boolean,boolean)}
	 * @param finalToken the token to start the backwards trace from
	 * @return a list of tokens, word tokens precede their corresponding segment tokens
	 * TODO?!! currently, no provisions are taken for SILs to be preceded by a <sil> word!
	 * -> it would be really annoying to insert such words in the token list, 
	 *    we hence handle silence-insertion at a later step in the process
	 * -> should we do the opposite and strip silence words, 
	 *    so that we don't have to support them if they ever occur in the token list?  
	 */
	protected synchronized List<Token> getTokens(Token finalToken) {
		List<Token> tokens = ResultUtil.getTokenList(finalToken, true, true);
		assert (tokens != null);
		// pure debug code to 
		// test that each sequence of SIL segments is preceded by a SIL word
		// --> turns out that this is often NOT the case with LexTree
		Token precedingToken = null;
		for (Token token : tokens) {
			if (isSilenceSegment(token)) {
				assert precedingToken != null : "null oops: " + tokens;
				if (!isSilenceWord(precedingToken)) {
				//	System.err.println("no silence word: " + tokens);
				}
			} else {
				precedingToken = token;
			}
		}
		return tokens;
	}
	
	private static boolean isSilenceWord(Token t) {
		SearchState s = t.getSearchState();
		return (s instanceof WordSearchState &&
				((WordSearchState) s).getPronunciation().getWord().isFiller());
	}
	
	private static boolean isSilenceSegment(Token t) {
		SearchState s = t.getSearchState();
		return (s instanceof UnitSearchState &&
				((UnitSearchState) s).getUnit().getName().equals("SIL"));
	}
	
	/**
	 * 
	 * @param finalToken the token in the result to start deltification from
	 */
/*	protected synchronized void newDeltify(Token finalToken) {
		List<Token> allTokens = getTokens(finalToken);
		// let's take a very easy (but maybe slightly inefficient) approach:
		// first, we create a shiny new IU network from the current tokens
		List<WordIU> newWordIUs = createIUNetworkFromWordAndPhonemeTokens(allTokens);
		// secondly, update the timings of previously output IUs 
		List<EditMessage<WordIU>> newEdits = compareAndUpdateOldNetwork(newWordIUs);
		// finally, update wordEdits to be output:
		wordEdits.clear();
		wordEdits.addAll(newEdits);
	}
	
	private List<WordIU> createIUNetworkFromWordAndPhonemeTokens(List<Token> allTokens) {
		throw new RuntimeException("not implemented");
	}

	private List<EditMessage<WordIU>> compareAndUpdateOldNetwork(List<WordIU> newWordIUs) {
		throw new RuntimeException("not implemented");
	}
*/
	/**
	 * update the stored ASR-representation with information from the ASR result's given Token  
	 * <p>
	 * the actual deltification algorithm works as follows:
	 * <ul><li>
	 * we first extract all the word- and phoneme tokens from the token sequence,
	 * </li><ul><li>
	 * 		each word-token is followed by the corresponding phoneme tokens
	 * </li><li>
	 * 		let's assert that every word-token is followed by the corresponding 
	 * 		phoneme tokens and that every SIL phoneme is preceded by a silence-word token
	 * </li></ul><li>
	 * we then compare the previous and the current hypothesis:
	 * </li><ul><li>
	 * 		the beginning of the previous and this word hypothesis should be equal,
	 * 		we only need to update phoneme timings
	 * </li><li>    
	 * 		eventually, the two hypotheses will differ:
	 * </li><ul><li>
	 * 				we construct remove-edits for the remaining old words
	 * </li><li>
	 * 				we then construct the new words and add edits for them
	 * </li></ul></ul></ul>
	 * 
	 * @param token the token from which the deltification starts
	 */
	protected synchronized void deltify(Token token) {
		List<Token> newTokens = getTokens(token);
		logger.debug("started to deltify; word tokens are: " + newTokens);
		// step over wordIUs and newWords to see which are equal in both
		ListIterator<Token> newIt = newTokens.listIterator();
		ListIterator<WordIU> wordIt = wordIUs.listIterator();
		// TODO: what are these for?
		double segmentStartTime = currentOffset * ResultUtil.FRAME_TO_SECOND_FACTOR;
		double segmentEndTime = 0.0;
		List<SegmentIU> emptyList = Collections.<SegmentIU>emptyList(); // needed to initialize prevSegmentIt with an empty non-null iterator
		Iterator<SegmentIU> currSegmentIt = emptyList.iterator();
		// TODO: what exactly is the meaning of this variable?
		boolean addSilenceWord = false;
		// TODO: implement this loop in new method compareNewTokensToPreviousOuput();
		while (newIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			if (newSearchState instanceof WordSearchState) {
				// ?
				if (!wordIt.hasNext()) {
					newIt.previous();
					break;
				}
				Pronunciation pron = ((WordSearchState) newToken.getSearchState()).getPronunciation();
				WordIU prevIU = wordIt.next();
				// check if the words matches and break once we reach the point where they don't match anymore
				if (!prevIU.wordEquals(pron)) {
					wordIt.previous(); // go back the one word that didn't match anymore
					newIt.previous();
					break;
				}
				// set the segment iterator that labels for this word can be updated from the trailing segment tokens
				currSegmentIt = prevIU.getSegments().iterator();
			} else if (newSearchState instanceof UnitSearchState) {
				segmentEndTime = getTimeFromNewIt(newIt);
				String name = ((UnitSearchState) newSearchState).getUnit().getName();
				// in the FA case, SIL-phonemes are not covered by SIL-words. duh.
				if (currSegmentIt.hasNext()) {
					currSegmentIt.next().updateLabel(new Label(segmentStartTime, segmentEndTime, name));
				} else if (name.equals("SIL")) {
					if (wordIt.hasNext()) {
						if (wordIt.next().isSilence()) {
							wordIt.previous();
							WordIU currWord = wordIt.next(); 
							currWord.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, name)));
							currSegmentIt = currWord.getSegments().iterator();
							addSilenceWord = false; // maybe this was missing? -> nope, doesn't help, but still, it seems sane to be here
						} else {
							wordIt.previous();
							addSilenceWord = true;
							break;
						}
					} else {
						addSilenceWord = true;
						break;
					}
				} else {
					assert false : newSearchState.getClass().toString() + newSearchState.toString();
					// assert false; // hmm, I don't know why, but it works without this assertion. and it doesn't hurt
				}
				segmentStartTime = segmentEndTime;
			} else {
				throw new RuntimeException("weird searchState type in deltify: " + newSearchState);
			}
		}
		// TODO: implement this loop in new method makeRevokes()
		// ok, now:
		// if there are words left in the prev word list, send purge notifications
		// purge notifications have to be sent in reversed order, starting with the very last word
		// therefore we put them in reverse order into a new list
		wordEdits.clear();
		while (wordIt.hasNext()) {
			WordIU prevIU = wordIt.next();
			wordEdits.add(0, new EditMessage<WordIU>(EditType.REVOKE, prevIU));
		}
		// TODO: likely culprit for what's going wrong
		// check if we need to insert a silence in the end (this happens when SIL does not have its own word token) 
		if (addSilenceWord) {
			WordIU newIU = new WordIU(null);
			newIU.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, "SIL")));
			wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
		}
		// TODO: implement this loop in new method makeAdds() ?
		// for the remaining words in the new list, add them to the old list and send add notifications
		while (newIt.hasNext()) {
			Token newToken = newIt.next();
			SearchState newSearchState = newToken.getSearchState();
			/* on WordSearchStates, we build an IU and add it */
			if (newSearchState instanceof WordSearchState) {
				Pronunciation pron = ((WordSearchState) newSearchState).getPronunciation();
				WordIU newIU = WordUtil.wordFromPronunciation(pron);
				currSegmentIt = newIU.getSegments().iterator();
				wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
			} else 
			/* on UnitSearchStates, we find the unit's timing and update the segmentIU */
			if (newSearchState instanceof UnitSearchState) {
				segmentEndTime = getTimeFromNewIt(newIt);

				String name = ((UnitSearchState) newSearchState).getUnit().getName();
				if (currSegmentIt.hasNext()) {
					currSegmentIt.next().updateLabel(new Label(segmentStartTime, segmentEndTime, name));
				} else if (name.equals("SIL")) {
					WordIU newIU = new WordIU(null);
					newIU.updateSegments(Collections.nCopies(1, new Label(segmentStartTime, segmentEndTime, "SIL")));
					wordEdits.add(new EditMessage<WordIU>(EditType.ADD, newIU));
				}
				segmentStartTime = segmentEndTime;
			}
		}
		logger.debug("I believe the following edits should be applied: " + wordEdits);
		logger.debug("to the previous IUs: " + wordIUs);
		try {
			wordIUs.apply(wordEdits);
		} catch (AssertionError ae) {
			logger.fatal("new tokens: " + newTokens);
			logger.fatal(addSilenceWord);
			logger.fatal("value of addSilenceWord was " + (addSilenceWord ? "true" : "false"));
			logger.fatal("");
			throw ae;
		}
		logger.debug("ok, ouput IUs are now: " + wordIUs);
	}
	
	/**
	 * tries to get segmentEndTime from a list iterator; 
	 * the iterator will be at the same position afterwards as it was before
	 */
	private double getTimeFromNewIt(ListIterator<Token> newIt) {
		double segmentEndTime;
		if (newIt.hasNext()) {
			Token t = newIt.next();
			if ((t.getSearchState() instanceof WordSearchState) 
			 && (newIt.hasNext()) 
			) {
				segmentEndTime = (newIt.next().getFrameNumber()+ currentOffset) * ResultUtil.FRAME_TO_SECOND_FACTOR;
				newIt.previous();
			} else {
				segmentEndTime = (t.getFrameNumber() + currentOffset) * ResultUtil.FRAME_TO_SECOND_FACTOR;
			}
			newIt.previous();
		} else
			segmentEndTime = getCurrentTime();
		return segmentEndTime;
	}

	/**
	 * update the stored ASR-representation with the new Result from ASR
	 * <ul>
	 * <li>the current representation can afterwards be queried through getWordIUs()</li>
	 * <li>the difference from the previous state can be queried through getWordEdits()</li>
	 * </ul>
	 * the actual deltification algorithm is described in {@link ASRWordDeltifier#deltify(Token)}
	 * @see ASRWordDeltifier#deltify(Token)
	 * @param result current ASR hypothesis
	 */
	public synchronized void deltify(Result result) {
		currentFrame = result.getFrameNumber();
		if (result.isFinal())
			recoFinal = true;
		Token t = result.getBestToken();
		if (t != null)
			deltify(result.getBestToken());
		else {
			if (currentFrame > 2) { // there never is for the first two frames, so don't print a message
				logger.debug("there was no best token at frame " + currentFrame);
			}
			wordEdits.clear();
		}
	}

	public synchronized List<EditMessage<WordIU>> getWordEdits() {
		return wordEdits;
	}

	public synchronized List<WordIU> getWordIUs() {
		// make sure that words in wordIUs are connected via sll
		wordIUs.connectSLLs();
		return wordIUs;
	}
	
	public synchronized int getCurrentFrame() {
		return currentFrame + currentOffset;
	}
	
	public synchronized double getCurrentTime() {
		return (currentFrame + currentOffset) * ResultUtil.FRAME_TO_SECOND_FACTOR;
	}

	@Override
	public void reset() {
		wordIUs.clear();
		recoFinal = false;
	}

	/**
	 *  there are two alternatives to set the offset:
	 *  - put an OffsetAdapter into the FrontEnd (after VAD)    
	 * 	- use signalOccurred() below; for this you have to call 
	 *    FrontEnd.addSignalListener(deltifier) somewhere (CurrentHypothesis-setup)
	 *    
	 *     here, the offset is given in centiseconds (frames)
	 */
	public void setOffset(int currentOffset) {
		logger.debug("SETTING OFFSET (frames) TO " + currentOffset);
		this.currentOffset = currentOffset;
	}
	
	@Override
	public void signalOccurred(Signal signal) {
		if (signal instanceof DataStartSignal) {
			startTime = signal.getTime();
			logger.debug("Audio start time is " + startTime);
		} else if (signal instanceof SpeechStartSignal) {
		//	setOffset((int) (signal.getTime() - startTime) / 10);
		}
	}

	public void setCollectTime(long collectTime) {
		logger.debug("SETTING COLLECT TIME (ms) TO " + ((collectTime - startTime) / 10));
	}
	
	@Override
	public String toString() {
		return "basic ASRWordDeltifier";
	}
	
	
}
