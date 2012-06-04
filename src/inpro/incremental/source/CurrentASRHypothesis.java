package inpro.incremental.source;

import inpro.incremental.FrameAware;
import inpro.incremental.PushBuffer;
import inpro.incremental.basedata.BaseData;
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
 */
public class CurrentASRHypothesis implements Configurable, ResultListener, Monitor {

	@S4Component(type = FrontEnd.class, mandatory = true)
	public final static String PROP_ASR_FRONTEND = "frontend";

	@S4Component(type = ASRWordDeltifier.class, defaultClass = ASRWordDeltifier.class)
	public final static String PROP_ASR_DELTIFIER = "asrFilter";
	ASRWordDeltifier asrDeltifier;
	
	@S4Component(type = BaseData.class, mandatory = false)
	public final static String PROP_BASE_DATA_KEEPER = "baseData";
	BaseData basedata;
	
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	List<PushBuffer> listeners;
	
//	@S4Component(type = TemporalNGramModel.class, mandatory = false)
//	public final static String PROP_TNGM = "tngm";
//	TemporalNGramModel tngm;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		asrDeltifier = (ASRWordDeltifier) ps.getComponent(PROP_ASR_DELTIFIER);
		if (asrDeltifier == null) {
			asrDeltifier = new ASRWordDeltifier();
		}
		System.err.println("deltifier is " + asrDeltifier);
		basedata = (BaseData) ps.getComponent(PROP_BASE_DATA_KEEPER);
		listeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
		
//		tngm = (TemporalNGramModel) ps.getComponent(PROP_TNGM);
	}
	
	public void addListener(PushBuffer pb) {
		if (listeners == null)
			listeners = new ArrayList<PushBuffer>();
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
		List<EditMessage<WordIU>> edits = asrDeltifier.getWordEdits();
		List<WordIU> ius = asrDeltifier.getWordIUs();
		int currentFrame = asrDeltifier.getCurrentFrame();
//		if (tngm != null) tngm.setFrame(currentFrame);
		for (PushBuffer listener : listeners) {
			// update frame count in frame-aware pushbuffers
			if (listener instanceof FrameAware)
				((FrameAware) listener).setCurrentFrame(currentFrame);
			// for TEDview, we additionally notify about segments and syllables
			checkForTEDview(listener);
			// notify
			if (ius != null && edits != null)
				listener.hypChange(ius, edits);
		}
		if (result.isFinal()) {
			commit();
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
// I believe it's not necessary to reset all incremental modules just because ASR reset, is it?
/*		for (PushBuffer listener : iulisteners) {
				listener.reset();
		}
*/ 
	}

	public void commit() {
		// commit all IUs once recognition finishes (returns to ready state)
		List<WordIU> ius = asrDeltifier.getWordIUs();
		List<EditMessage<WordIU>> edits = new ArrayList<EditMessage<WordIU>>(ius.size());
		for (WordIU iu : ius) {
			edits.add(new EditMessage<WordIU>(EditType.COMMIT, iu));
			iu.commit();
//			iu.update(EditType.COMMIT);
		}
		for (PushBuffer listener : listeners) {
			listener.hypChange(ius, edits);
		}
//		basedata.reset();
		this.reset();
	}
	
}