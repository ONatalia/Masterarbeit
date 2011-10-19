package org.cocolab.inpro.incremental.processor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.CandidateAnalysisIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.FormulaIU;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.irmrsc.rmrs.Formula;
import org.cocolab.inpro.irmrsc.rmrs.RMRSLoader;
import org.cocolab.inpro.irmrsc.rmrs.SemanticMacro;
import org.cocolab.inpro.irmrsc.rmrs.Variable;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;


public class RMRSComposer extends IUModule {

	@S4String()
	public final static String PROP_SEM_MACROS_FILE = "semMacrosFile";
	private String semMacrosFile;
	
	@S4String()
	public final static String PROP_SEM_RULES_FILE = "semRulesFile";
	private String semRulesFile;
	
	private Map<String,Formula> semanticMacrosLongname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticMacrosShortname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticRules  = new HashMap<String,Formula>();
	private Map<CandidateAnalysisIU,FormulaIU> states = new HashMap<CandidateAnalysisIU,FormulaIU>();
	
	private static FormulaIU firstUsefulFormulaIU;
	
	@SuppressWarnings("unchecked")
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		try {
			// load semantic macros
			semMacrosFile = ps.getString(PROP_SEM_MACROS_FILE);
			// parse all and put the formulas in semanticMacros
			List<SemanticMacro> l = RMRSLoader.loadMacros(new URL(semMacrosFile));
			for (SemanticMacro m : l) {
				semanticMacrosLongname.put(m.getLongName(), m.getFormula());
				String shortname = m.getShortName();
				if (! (shortname == null)) {
					semanticMacrosShortname.put(shortname, m.getFormula());
				} else {
					System.out.println("Warning: No shortname given for macro "+m.getLongName()+".");
				}
			}
			System.out.println("Successfully loaded "+semanticMacrosLongname.size()+" semantic macros.");
			
			// load semantic rules
			semRulesFile = ps.getString(PROP_SEM_RULES_FILE);
			// parse all and put the formulas in semanticRules
			Map<String, String> rules;
			rules = RMRSLoader.loadRules(new URL(semRulesFile));
			for (Map.Entry<String, String> e : rules.entrySet()) {
				String rulename = e.getKey();
				String longname = e.getValue();
				if (! semanticMacrosLongname.containsKey(longname)) {
					System.out.println("Error: Semantic macro with longname '"+longname+"' cannot be found.");
				} else {
					semanticRules.put(rulename, semanticMacrosLongname.get(longname));
				}					
			}
			System.out.println("Successfully loaded "+rules.size()+" semantic rules.");
			
			//System.out.println(semanticMacrosLongname);
			firstUsefulFormulaIU = new FormulaIU(FormulaIU.FIRST_FORMULA_IU, Collections.EMPTY_LIST, semanticMacrosLongname.get("init"));
			//System.out.println(CandidateAnalysisIU.FIRST_CA_IU);
			//System.out.println(firstUsefulFormulaIU);
			this.states.put(CandidateAnalysisIU.FIRST_CA_IU,firstUsefulFormulaIU);
			//System.out.println(states);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		List<EditMessage<FormulaIU>> newEdits = new ArrayList<EditMessage<FormulaIU>>();
		for (EditMessage<? extends IU> edit : edits) {
			CandidateAnalysisIU ca = (CandidateAnalysisIU) edit.getIU();
			switch (edit.getType()) {
				case REVOKE:
					for (IU formula : ca.grounds()) {
						if (formula instanceof FormulaIU) {
							newEdits.add(new EditMessage<FormulaIU>(EditType.REVOKE, (FormulaIU) formula));
						}
					}
					break;
				case ADD:
					CandidateAnalysisIU previousCa = (CandidateAnalysisIU) ca.getSameLevelLink();
					if (previousCa != null) {
						System.out.println("Ping");
						FormulaIU previousFIU = firstUsefulFormulaIU;
						if (previousCa.grounds().size() > 0) {
							System.out.println("Pong");
							previousFIU = (FormulaIU) states.get(previousCa);//.grounds().get(0);
						}
						List<String> lastDerive = ca.getCandidateAnalysis().getLastDerive();
						System.out.println(firstUsefulFormulaIU);
						
						System.out.println("-------");
						Formula newForm = new Formula(previousFIU.getFormula());
						for (String rule : lastDerive) {
							System.out.println("= "+newForm);
							// go through all new syntactic rule applications
							if (rule.startsWith("m(")) {
								// the current rule is a lexical one; build lexical formula. 
								String tag = rule.substring(2, rule.length()-1);
								Variable.Type type = Variable.Type.INDEX;
								String lexname = tag;
								// map tagnames to Variable.Type.s
								Formula lexitem = new Formula(lexname, type);
								System.out.println("+ "+rule+"\n+ "+lexitem);
								newForm.forwardCombine(lexitem);
							} else {
								// the current rule is a syntactic one; get rule semantics from map.
								Formula rulesem = semanticRules.get(rule);
								System.out.println("+ "+rule+"\n+ "+rulesem);
								newForm.forwardCombine(rulesem);
							}
							System.out.println("= "+newForm);
							newForm.reduce();
							//newForm.renumber(0);
						}
						FormulaIU newFIU = new FormulaIU(previousFIU, ca, newForm);
						newEdits.add(new EditMessage<FormulaIU>(EditType.ADD, newFIU));
						states.put(ca,newFIU);						
					} else {
						//TODO: can this happen?
					}
					break;
				case COMMIT:
					for (IU sem : ca.grounds()) {
						if (sem instanceof FormulaIU) {
							newEdits.add(new EditMessage<FormulaIU>(EditType.COMMIT, (FormulaIU) sem));
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		this.rightBuffer.setBuffer(newEdits);
	}

}
