package inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;
import sium.nlu.context.Context;
import sium.nlu.grounding.Grounder;
import sium.nlu.language.LingEvidence;
import sium.nlu.language.mapping.Mapping;
import sium.nlu.language.mapping.MaxEntMapping;
import sium.nlu.stat.Distribution;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SlotIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.utils.IUUtils;

public class ResolutionModuleNgram extends IUModule {
	
	@S4Integer(defaultValue = 3)
	public final static String ORDER = "order";
	
	@S4String(defaultValue = "slot")
	public final static String ID = "id";
	
	private Context<String,String> context;
	private String referent;
	private boolean trainMode;
	private Mapping<String> mapping;
	private Grounder<String, String> grounder;
	private int order;
	private String slotID;
	private SlotIU currentHyp;

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		grounder = new Grounder<String, String>();
		mapping = new MaxEntMapping("temp.txt");
		order = ps.getInt(ORDER);
		slotID = ps.getString(ID);
		currentHyp = new SlotIU();
		currentHyp.setName(slotID);
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
	
		for (EditMessage<? extends IU> edit : edits) {
			IU iu = edit.getIU();
//			WordIU wordIU = IUUtils.getWordIU(iu);
			switch (edit.getType()) {
			case ADD:
				if (isNormalMode()) 
					applyInstance(iu);
				ground(iu);
				setRightBuffer(EditType.ADD, iu);
				
				break;
				
			case COMMIT:
//				when training, only consider committed data
				if (isTrainMode())
					addTrainingInstance(iu);
				setRightBuffer(EditType.COMMIT, iu);
				break;
				
			case REVOKE:
//				this makes the (reasonable) assumption that only the most recent ADD can be revoked
				revoke(iu);
				setRightBuffer(EditType.REVOKE, iu);
				break;
				
			default:
				break;
			}
		}
	}

	protected void ground(IU iu) {
		currentHyp.groundIn(iu);
	}
	
	protected void setRightBuffer(EditType editType, @SuppressWarnings("unused") IU iu) { // the second parameter is used in an inheriting class
		List<EditMessage<SlotIU>> newEdits = new ArrayList<EditMessage<SlotIU>>();
		currentHyp.setDistribution(getGrounder().getPosterior());
		newEdits.add(new EditMessage<SlotIU>(editType, currentHyp));
		rightBuffer.setBuffer(newEdits);
	}

	protected void revoke(IU iu) {
		grounder.undoStep();
		checkIsFirst(IUUtils.getWordIU(iu));
	}

	protected void applyInstance(IU iu) {
//		this is the only place where something depends on the beginning of a new utterance
		checkIsFirst(IUUtils.getWordIU(iu));
		LingEvidence ling = getLingEvidence(iu);
		Distribution<String> currentDist = mapping.applyEvidenceToContext(ling);
		grounder.groundIncrement(context, currentDist);
	}

	protected void checkIsFirst(WordIU wordIU) {
		if (IUUtils.isFirstWordIU(wordIU.getSameLevelLink())) {
			grounder = new Grounder<String, String>();
			currentHyp = new SlotIU();
			currentHyp.setName(slotID);
		}		
	}

	protected void addTrainingInstance(IU iu) {
		mapping.addEvidenceToTrain(getLingEvidence(iu), context.getPropertiesForEntity(getReferent()));
	}

//	get words up to the order size (default is trigram)
	protected LingEvidence getLingEvidence(IU iu) {
		IU wordIU = IUUtils.getWordIU(iu);
		LingEvidence evidence = new LingEvidence();
		for (int i=1; i<=order; i++) {
			if (wordIU == null || IUUtils.isFirstWordIU(wordIU)) {
				evidence.addEvidence("w" + i, "<s>");
				if (wordIU != null) wordIU = wordIU.getSameLevelLink();
				continue;	
			}
			evidence.addEvidence("w" + i, wordIU.toPayLoad());
			wordIU = wordIU.getSameLevelLink();
		}
		return evidence;
	}

	public void setContext(Context<String, String> context) {
		this.context = context;
	}
	
	public void train() {
		mapping.train();
	}
	
	public void clear() {
		mapping.clear();
	}

	public boolean isNormalMode() {
		return !trainMode;
	}

	public boolean isTrainMode() {
		return trainMode;
	}

	public void toggleTrainMode() {
		this.trainMode = true;
	}
	
	public void toggleNormalMode() {
		this.trainMode = false;
	}

	public String getReferent() {
		return referent;
	}

	public void setReferent(String referent) {
		this.referent = referent;
	}
	
	public Grounder<String,String> getGrounder() {
		return this.grounder;
	}

}
