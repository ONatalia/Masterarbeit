package inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import inpro.incremental.IUModule;
import inpro.incremental.unit.CandidateAnalysisIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.FormulaIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IUList;
import inpro.incremental.unit.SemanticIU;
import inpro.incremental.unit.utils.IUUtils;
import inpro.irmrsc.parser.CandidateAnalysis;
import inpro.irmrsc.rmrs.Formula;
import inpro.irmrsc.rmrs.Relation;
import inpro.irmrsc.rmrs.SimpleAssertion;

public class PredicateLogicModule extends IUModule {

	
	/* 
	   This class takes the output from RMRSComposer and breaks it apart into smaller IUs, called Semantic IUs. 
	 
	   For example:
	   nimm den gelben mast
	  
	   would result in the RMRS string:
	   [ [l0:a1:e2]\n{ [l38:a39:x14] [l0:a1:e2] }\nARG1(a1,x8),\nl6:a7:addressee(x8),\nl0:a1:_nehmen(e2),
	   \nARG2(a1,x14),\nBV(a13,x14),\nRSTR(a13,h21),\nBODY(a13,h22),\nl12:a13:_def(),\nARG1(a33,x14),
	   \nl18:a33:_gelb(e34),\nl18:a19:_mast(x14),\nqeq(h21,l18)]
	   
	   That would then be processed in this module to have the following IUs:
		e2:nehmen(x8:addressee,x14:mast) 
		x8:addressee() 
		x14:def(h21:,h22:) 
		x14:mast()
		e34:gelb(x14:mast)
 
 		And IUs are checked, ones that have already been sent (i.e., that correspond to earlier words)
 		are not re-sent. 
 		
 		ADD / REVOKE 
 		
	 */
	
	private IUList<SemanticIU> prevList;
	private IUList<SemanticIU> addedIUs;
	private SemanticIU sll;

	public static Formula topFormula;
	public static CandidateAnalysis topCA;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		prevList = new IUList<SemanticIU>();
		addedIUs = new IUList<SemanticIU>();
		sll = new SemanticIU();
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {

		TreeSet<EditMessage<FormulaIU>> allCaIUs = new TreeSet<EditMessage<FormulaIU>>(new FormulaIUComparator());
		
		for (EditMessage<? extends IU> edit: edits){
			IU iu = edit.getIU();
			if (iu instanceof FormulaIU) {
				EditMessage<FormulaIU> castedEdit = (EditMessage<FormulaIU>) edit;
//				System.out.println(castedEdit.getIU().getFormula().toRMRSString()+ " " + castedEdit.getIU().groundedIn().get(0).toPayLoad());
				allCaIUs.add(castedEdit);
			}
		}
		
		if (allCaIUs.isEmpty()) return;
			
		EditMessage<FormulaIU> topEdit = allCaIUs.first();
		topCA = ((CandidateAnalysisIU) topEdit.getIU().groundedIn().get(0)).getCandidateAnalysis();
//		System.out.println("top:" +topEdit.getIU().getFormula().toRMRSString() );
		if (topEdit.getType().isCommit()) {
			commitPrevList();
			return;
		}
		
//		new utterance
		if (IUUtils.isFirst(topEdit.getIU())) {
			sll = new SemanticIU();
			prevList.clear();
			addedIUs.clear();
		}
		
		Formula formula = topEdit.getIU().getFormula();
		topFormula = formula;

//		Create a mapping for all predicates and their ids (their id is their first argument)
		HashMap<String, String> semMapping = new HashMap<String,String>();
		for (SimpleAssertion assertion : formula.getUnscopedPredicateLogic()) {
			if (assertion.getPredicateName().equals("def")) continue;
			semMapping.put(getVariableString(formula, assertion.getArguments().get(0)), assertion.getPredicateName());
		}
		

//		step through again, replacing argument ids with actual string values
		IUList<SemanticIU> list = new IUList<SemanticIU>();
		
//		this is the way it was before, for the CoLing 2014 paper
//		for (Relation rel : formula.getRelations()) {
////			if (!relationIsAcceptableType(rel)) continue;
//			if (rel.getType().equals(Relation.Type.ARGREL)) {
//			String anchor = findAnchorName(formula, rel);
//			String argument = findArgumentName(formula, rel);
//			SemanticEvidence semEv = new SemanticEvidence(rel.getName(), anchor, argument);
//			semEv.setLeftEntity(formula.getVariableString(rel.getAnchor()));
//			semEv.setRightEntity(formula.getVariableString(rel.getArgument()));
//			boolean isHead = rel.getAnchor() == formula.getMainHook().getAnchor();
//			SemanticIU semIU = new SemanticIU(formula.getVariableString(rel.getArgument()), rel.getName(), argument, formula.getVariableString(rel.getArgument()), 
//					anchor,formula.getVariableString(rel.getAnchor()), isHead);
////			System.out.println(semIU);
//			semIU.groundIn(topEdit.getIU());
//			semIU.setSameLevelLink(sll);
//			
//			list.add(semIU);
//			sll = semIU;
//		}
//	}
		
		for (SimpleAssertion assertion : formula.getUnscopedPredicateLogic()) {
			String left = new String(), right = new String();
			String leftID = null, rightID = null;
			
			if (assertion.getNumberOfArguments() > 1) {
				leftID = getVariableString(formula, assertion.getArguments().get(1));
				left = checkNull(semMapping.get(leftID));
			}
			
			if (assertion.getNumberOfArguments() > 2) {
				rightID = getVariableString(formula, assertion.getArguments().get(2));
				right = checkNull(semMapping.get(rightID));
			}
			
			SemanticIU semIU = new SemanticIU(
					getVariableString(formula, assertion.getArguments().get(0)), assertion.getPredicateName(), 
					left, leftID, right,rightID, false);
			
			semIU.groundIn(topEdit.getIU());
			semIU.setSameLevelLink(sll);
			
			list.add(semIU);
			sll = semIU;
		}
		
//		find differences in IUs between this increment and the previous
		List<EditMessage<SemanticIU>> diffs = prevList.diffByPayload(list);
		prevList = list;
		
//		print(diffs);
		
//		keep track of the IUs that are added in case we need to commit them
		for (EditMessage<SemanticIU> edit : diffs) {
			if (edit.getType().equals(EditType.ADD)) 
				addedIUs.add(edit.getIU());
			else if (edit.getType().equals(EditType.REVOKE))
				addedIUs.remove(edit.getIU());
		}
		
		rightBuffer.setBuffer(diffs);
	}
	
	private String findAnchorName(Formula formula, Relation relation) {
		String name = null;
		for (Relation rel : formula.getRelations()) {
//			if (!relationIsAcceptableType(rel)) continue;
			if (rel.getType().equals(Relation.Type.ARGREL)) continue;
			if (rel == relation) continue;
			if (rel.getAnchor() == relation.getAnchor())
				name = rel.getName();
		}
		return name;
	}
	
	private String findArgumentName(Formula formula, Relation relation) {
		String name = null;
		for (Relation rel : formula.getRelations()) {
//			if (!relationIsAcceptableType(rel)) continue;
			if (rel.getType().equals(Relation.Type.ARGREL)) continue;
			if (rel == relation) continue;
			if (rel.getArgument() == relation.getArgument())
				name = rel.getName();
		}
		return name;
	}
	
	private void print(List<EditMessage<SemanticIU>> diffs) {
		for (EditMessage<SemanticIU> iu : diffs) {
			System.out.println(iu);
		}
		System.out.println();		
	}

	private void commitPrevList() {
		ArrayList<EditMessage<SemanticIU>> newEdits = new ArrayList<EditMessage<SemanticIU>>();
		for (SemanticIU iu : addedIUs) {
			newEdits.add(new EditMessage<SemanticIU>(EditType.COMMIT, iu));
		}
		rightBuffer.setBuffer(newEdits);
		
	}

	private String checkNull(String string) {
		if (string == null)
			return new String();
		return string;
	}

	public String getVariableString(Formula formula, int id) {
		return formula.getVariableString(id);
	}
	
	double weight = 0.9;
	private class FormulaIUComparator implements Comparator<EditMessage<FormulaIU>> {
	    @Override
	    public int compare(EditMessage<FormulaIU> x, EditMessage<FormulaIU> y) {
	    	CandidateAnalysis cax = ((CandidateAnalysisIU) x.getIU().groundedIn().get(0)).getCandidateAnalysis();
	    	CandidateAnalysis cay = ((CandidateAnalysisIU) y.getIU().groundedIn().get(0)).getCandidateAnalysis();
	    	double px = cax.getProbability() * weight + (1.0-weight)*cax.getNumberOfMatches();
	    	double py = cay.getProbability() * weight + (1.0-weight)*cay.getNumberOfMatches();
	        return Double.compare(py, px);
	    }
	}
	

}
