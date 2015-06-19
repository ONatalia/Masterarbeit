package inpro.incremental.processor;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.SemanticIU;
import inpro.incremental.unit.SlotIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.utils.IUUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sium.nlu.context.Context;
import sium.nlu.grounding.Grounder;
import sium.nlu.language.LingEvidence;
import sium.nlu.language.mapping.Mapping;
import sium.nlu.language.mapping.MaxEntMapping;
import sium.nlu.stat.Distribution;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class ResolutionModuleRMRS extends ResolutionModuleNgram {
	
	/*
	 * This resolves entities using RMRS. Output is a set of Slots corresponding
	 * to the entities. 
	 * 
	 * Make sure the sium.module.PredicateLogicModule's right buffer is set to sium_rmrs!
	 * 
	 * If there are problems with Verify errors when using MaxEnt, reorder the build path
	 * so the maxent and opennlp-tools jars are near the top.
	 * 
	 * TODO: mapping type should be loaded from the config file, I'm afraid I may need to use a switch statement
	 * since I want to keep the sphinx-specific stuff out of the sium project. Also, it needs to make an instance
	 * for each context (perhaps use a Factory).
	 * 
	 * TODO: the HashMaps should be abstracted into one or two classes
	 *  
	 * TODO: SLLs? 
	 *  
	 */
	
//	Be careful, the String in mappings, contexts, and referents is an entity type (e.g., x), whereas the String in the other three
//	HashMaps is the entity itself (e.g., x14).
	private HashMap<String,Mapping<String>> mappings;
	private HashMap<String,Context<String,String>> contexts;
	private HashMap<String, String> referents;
	
	private HashMap<String,Grounder<String,String>> grounders;
	private HashMap<String,SlotIU> currentHyps;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		init();
	}
	
	private void init() {
		mappings = new HashMap<String, Mapping<String>>();
		grounders = new HashMap<String, Grounder<String, String>>();
		currentHyps = new HashMap<String, SlotIU>();
		contexts = new HashMap<String,Context<String,String>>();
		referents = new HashMap<String, String>();
	}
	
	@Override
	protected void checkIsFirst(WordIU wordIU) {
		if (IUUtils.isFirstWordIU(wordIU.getSameLevelLink())) {
			grounders.clear();
			currentHyps.clear();
		}		
	}
	
	@Override
	protected void addTrainingInstance(IU iu) {
		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
			String eType = siu.getEntityType();
//			System.out.println(siu);
			if (isAllowedEntityType(eType)) {
				getMapping(eType).addEvidenceToTrain(getLingEvidence(iu), getContext(eType).getPropertiesForEntity(getReferent(eType)));
			}
			
			if (siu.getLeft() != null && !siu.getLeft().isEmpty()) {
				eType = siu.getLeftEntityType();
				if (isAllowedEntityType(eType)) 
					getMapping(eType).addEvidenceToTrain(getLingEvidence(iu), getContext(eType).getPropertiesForEntity(getReferent(eType)));
			}
			
			if (siu.getRight() != null && !siu.getRight().isEmpty()) {
				eType = siu.getRightEntityType();
				if (isAllowedEntityType(eType))
					getMapping(eType).addEvidenceToTrain(getLingEvidence(iu), getContext(eType).getPropertiesForEntity(getReferent(eType)));
			}
		}
	}
	
	@Override
	protected void applyInstance(IU iu) {
		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
//			this is the only place where something depends on the beginning of a new utterance
			checkIsFirst(IUUtils.getWordIU(iu));

			if (isAllowedEntityType(siu.getEntityType())) {
				Distribution<String> currentDist = getMapping(siu).applyEvidenceToContext(getLingEvidence(siu));
				getGrounder(siu).groundIncrement(getContext(siu.getEntityType()), currentDist);
			}
//			of course ground the main predicate, but also ground the arguments since they are applicable
			
			if (siu.hasLeft() && isAllowedEntityType(siu.getLeftEntityType())) {
				Distribution<String> currentDist = getMapping(siu.getLeftEntityType()).applyEvidenceToContext(getLingEvidence(siu));
				getGrounder(siu.getLeftID()).groundIncrement(getContext(siu.getLeftEntityType()), currentDist);
			}
			if (siu.hasRight() && isAllowedEntityType(siu.getRightEntityType())) {
				Distribution<String> currentDist = getMapping(siu.getRightEntityType()).applyEvidenceToContext(getLingEvidence(siu));
				getGrounder(siu.getRightID()).groundIncrement(getContext(siu.getRightEntityType()), currentDist);
			}
		}
	}
	

	private Grounder<String, String> getGrounder(String id) {
		initializeEntity(id);
		return grounders.get(id);
	}


	private Mapping<String> getMapping(SemanticIU siu) {
		initializeEntityType(siu);
		return mappings.get(siu.getEntityType());
	}
	
	private Mapping<String> getMapping(String type) {
		initializeEntityType(type);
		return mappings.get(type);
	}
	
	private void initializeEntityType(String type) {

		if (!mappings.containsKey(type)) {
			if (isAllowedEntityType(type)) {
				mappings.put(type, new MaxEntMapping(type + ".txt"));
//				mappings.put(type, new NaiveBayesMapping());
			}
		}
	}

	private void initializeEntity(String key) {
		if (!grounders.containsKey(key)) {
			grounders.put(key, new Grounder<String,String>());
			currentHyps.put(key, new SlotIU());
		}
	}

	private void initializeEntityType(SemanticIU siu) {
		initializeEntityType(siu.getEntityType());
	}

	private boolean isAllowedEntityType(String entity) {
		return contexts.containsKey(entity);
	}
	
	@Override
	public void train() {
		for (String entity : mappings.keySet()){
			mappings.get(entity).train();
		}
	}
	
	@Override
	public void clear() {
		for (String entity : mappings.keySet()){
			mappings.get(entity).clear();
		}
		mappings.clear();
	}
	
	@Override
	protected LingEvidence getLingEvidence(IU iu) {
		LingEvidence ev = new LingEvidence();
		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
			
			ev.addEvidence("pred", siu.getPredicate());
//			ev.addEvidence("word", IUUtils.getWordIU(siu).toPayLoad());
			
//			int i = 0;
//			for (String derivation : IUUtils.getSyntax(siu).getCandidateAnalysis().getLastDerive()) {
//				ev.addEvidence("d" + (i++), derivation);	
//			}
			
			if (siu.hasLeft())
				ev.addEvidence("left", siu.getLeft());
			if (siu.hasRight())
				ev.addEvidence("right", siu.getRight());
			
		}
		return ev;
	}
	
	public void setReferent(String entity, String referent) {
		referents.put(entity, referent);
	}
	
	public String getReferent(String entity) {
		return referents.get(entity);
	}
	
	@Override
	public void setContext(Context<String, String> context) {
		contexts.put(context.getContextID(), context);
	}
	
	@Override
	protected void ground(IU iu) {
		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
			getCurrentHyp(siu).groundIn(iu); 
		}
	}
	
	private SlotIU getCurrentHyp(SemanticIU siu) {
		initializeEntity(siu);
		return currentHyps.get(siu.getSemID());
	}
	
	private void initializeEntity(SemanticIU siu) {
		initializeEntity(siu.getSemID());
	}

	private SlotIU getCurrentHyp(String id) {
		initializeEntity(id);
		return currentHyps.get(id);
	}

	@Override
	protected void removeGround(IU iu) {
//		TODO: throws NPE?
	}

	@Override
	protected void setRightBuffer(EditType editType, IU iu) {

		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
			List<EditMessage<SlotIU>> newEdits = new ArrayList<EditMessage<SlotIU>>();
			
			if (siu.hasLeft()) {
				getCurrentHyp(siu.getLeftID()).setDistribution(getGrounder(siu.getLeftID()).getPosterior());
				getCurrentHyp(siu).setName(siu.getLeftID());
				SlotIU currentHyp =  getCurrentHyp(siu.getLeftID());
				if (!currentHyp.isCommitted())  newEdits.add(new EditMessage<SlotIU>(editType,currentHyp));
			}
			
			if (siu.hasRight()) {
				getCurrentHyp(siu.getRightID()).setDistribution(getGrounder(siu.getRightID()).getPosterior());
				getCurrentHyp(siu).setName(siu.getRightID());
				SlotIU currentHyp =  getCurrentHyp(siu.getRightID());
				if (!currentHyp.isCommitted()) newEdits.add(new EditMessage<SlotIU>(editType,currentHyp));
			}
			getCurrentHyp(siu).setDistribution(getGrounder(siu).getPosterior());
			getCurrentHyp(siu).setName(siu.getSemID());
			getCurrentHyp(siu).setIsHead(siu.isHead());
			
			SlotIU currentHyp = getCurrentHyp(siu);
			if (!currentHyp.isCommitted()) newEdits.add(new EditMessage<SlotIU>(editType, currentHyp));
			rightBuffer.setBuffer(newEdits);
		}
	}
	
	public Context<String,String> getContext(String contextID) {
		return contexts.get(contextID);
	}

	@Override
	protected void revoke(IU iu) {
		if (iu instanceof SemanticIU) {
			SemanticIU siu = (SemanticIU) iu;
			getGrounder(siu).undoStep();
		}
	}

	private Grounder<String, String> getGrounder(SemanticIU siu) {
		initializeEntity(siu);
		return grounders.get(siu.getSemID());
	}
	
	public HashMap<String, Grounder<String,String>> getGrounders() {
		return grounders;
	}
	
}
