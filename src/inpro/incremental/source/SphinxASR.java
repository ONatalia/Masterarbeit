package inpro.incremental.source;

import inpro.incremental.FrameAware;
import inpro.incremental.PushBuffer;
import inpro.incremental.deltifier.ASRWordDeltifier;
import inpro.incremental.sink.TEDviewNotifier;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.WordIU;

import java.util.ArrayList;
import java.util.List;

//import work.inpro.sphinx.linguist.language.ngram.TemporalNGramModel;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;

/** 
 * this class is the glue between sphinx and InproTK and is the
 * starting point into the IU world.
 * @author timo
 * 
 * TODO: commit words as soon as they are unambiguously recognized 
 *       (i.e., when there are no alternatives left on the beam)
 * TODO: add support for top-down revokes (i.e., remove hypotheses
 *       from the search space when higher-level tells us to)
 *       (I tested before but back then it resulted in horrible overall results)
 */
public class SphinxASR implements Configurable, ResultListener, Monitor {

	@S4Component(type = FrontEnd.class, mandatory = true)
	public final static String PROP_ASR_FRONTEND = "frontend"; // NO_UCD (unused code)

	@S4Component(type = ASRWordDeltifier.class, defaultClass = ASRWordDeltifier.class)
	public final static String PROP_ASR_DELTIFIER = "asrFilter"; // NO_UCD (use private)
	protected ASRWordDeltifier asrDeltifier;
	
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	private final List<PushBuffer> listeners = new ArrayList<PushBuffer>();

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		asrDeltifier = (ASRWordDeltifier) ps.getComponent(PROP_ASR_DELTIFIER);
		if (asrDeltifier == null) {
			asrDeltifier = new ASRWordDeltifier();
		}
		System.err.println("deltifier is " + asrDeltifier);
		listeners.clear();
		listeners.addAll(ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class));
	}
	
	public void addListener(PushBuffer pb) {
		listeners.add(pb);
	}
	
	/** 
	 * programmatically set the deltifier
	 */
	public void setDeltifier(ASRWordDeltifier deltifier) {
		asrDeltifier = deltifier;
		reset();
	}
	
	public ASRWordDeltifier getDeltifier() {
		return asrDeltifier;
	}
	
	public void newResult(Result result) {
		asrDeltifier.deltify(result);
		notifyListeners();
		if (result.isFinal()) {
			commit();
		}
	}
	
	protected void notifyListeners() {
		List<EditMessage<WordIU>> edits = asrDeltifier.getWordEdits();
		List<WordIU> ius = asrDeltifier.getWordIUs();
		int currentFrame = asrDeltifier.getCurrentFrame();
		for (PushBuffer listener : listeners) {
			// update frame count in frame-aware pushbuffers
			if (listener instanceof FrameAware)
				((FrameAware) listener).setCurrentFrame(currentFrame);
			// for TEDview, we additionally notify about segments and syllables
			checkForTEDview(listener);
			// notify
			if (ius != null && edits != null && !edits.isEmpty())
				listener.hypChange(ius, edits);
		}
	}

	/** for TEDview, we additionally notify about segments and syllables */
	private void checkForTEDview(PushBuffer listener) {
		if ((listener instanceof TEDviewNotifier)) {
			List<EditMessage<WordIU>> edits = asrDeltifier.getWordEdits();
			if (!edits.isEmpty()) {
				List<IU> sylIUs = new ArrayList<IU>();
				List<SegmentIU> segIUs = new ArrayList<SegmentIU>();
				for (WordIU wordIU : asrDeltifier.getWordIUs()) {
					segIUs.addAll(wordIU.getSegments());
					sylIUs.addAll(wordIU.groundedIn());
				}
				listener.hypChange(segIUs, edits);
				listener.hypChange(sylIUs, edits);
			}
		}
	}

	public void reset() {
		asrDeltifier.reset();
	}

	public void commit() {
		// commit all IUs once recognition finishes (returns to ready state)
		List<WordIU> ius = asrDeltifier.getWordIUs();
		List<EditMessage<WordIU>> edits = new ArrayList<EditMessage<WordIU>>(ius.size());
		for (WordIU iu : ius) {
			edits.add(new EditMessage<WordIU>(EditType.COMMIT, iu));
			iu.commit();
		}
		for (PushBuffer listener : listeners) {
			listener.hypChange(ius, edits);
		}
		this.reset();
	}
	
}