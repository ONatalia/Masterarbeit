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
import org.cocolab.inpro.incremental.unit.TagIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.irmrsc.rmrs.Formula;
import org.cocolab.inpro.irmrsc.rmrs.SemanticMacro;
import org.cocolab.inpro.irmrsc.rmrs.Variable;
import org.cocolab.inpro.irmrsc.util.RMRSLoader;
import org.cocolab.inpro.nlu.AVPair;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;


public class RMRSComposer extends IUModule {

	@S4String()
	public final static String PROP_SEM_MACROS_FILE = "semMacrosFile";
	private String semMacrosFile;

	@S4String()
	public final static String PROP_SEM_RULES_FILE = "semRulesFile";
	private String semRulesFile;

	@S4Component(type = Resolver.class, mandatory = false)
	public final static String PROP_RESOLVER = "resolver";
	private Resolver resolver;
	
	@S4Component(type = TagParser.class)
	public static final String PROP_PARSER = "parser";
	protected TagParser parser;
	
	@S4String()
	public final static String PROP_TAG_LEXICON_FILE = "tagLexiconFile";
	private String tagLexiconFile;
	
	private Map<String,Formula> semanticMacrosLongname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticMacrosShortname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticRules  = new HashMap<String,Formula>();
	private Map<String,Variable.Type> semanticTypesOfTags = new HashMap<String,Variable.Type>();
	private Map<CandidateAnalysisIU,FormulaIU> states = new HashMap<CandidateAnalysisIU,FormulaIU>();

	private static FormulaIU firstUsefulFormulaIU;
	
	private final static double MALUS_SEMANTIC_MISMATCH = 0.7;
	private final static double MALUS_NO_REFERENCE_RESOLUTION = 0.5;
	
	
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
					logger.info("Warning: No shortname given for macro "+m.getLongName()+".");
				}
			}
			logger.info("Successfully loaded "+semanticMacrosLongname.size()+" semantic macros.");

			// load semantic rules
			semRulesFile = ps.getString(PROP_SEM_RULES_FILE);
			// parse all and put the formulas in semanticRules
			Map<String, String> rules;
			rules = RMRSLoader.loadRules(new URL(semRulesFile));
			for (Map.Entry<String, String> e : rules.entrySet()) {
				String rulename = e.getKey();
				String longname = e.getValue();
				if (! semanticMacrosLongname.containsKey(longname)) {
					logger.info("Error: Semantic macro with longname '"+longname+"' cannot be found.");
				} else {
					semanticRules.put(rulename, semanticMacrosLongname.get(longname));
				}					
			}
			logger.info("Successfully loaded "+rules.size()+" semantic rules.");
			
			// load tag lexicon
			tagLexiconFile = ps.getString(PROP_TAG_LEXICON_FILE);
			semanticTypesOfTags = RMRSLoader.loadTagLexicon(new URL(tagLexiconFile));
			logger.info("Successfully loaded "+semanticTypesOfTags.size()+" associations of semantic types and POS tags.");
			
			// initialize first formula iu
			firstUsefulFormulaIU = new FormulaIU(FormulaIU.FIRST_FORMULA_IU, Collections.EMPTY_LIST, semanticMacrosLongname.get("init"));
			this.states.put(CandidateAnalysisIU.FIRST_CA_IU,firstUsefulFormulaIU);
			
			// Set up the resolver
			this.resolver = (Resolver) ps.getComponent(PROP_RESOLVER);
			logger.info("Set up resolver: " + this.resolver.toString());		
			
			// Set up the parser
			this.parser = (TagParser) ps.getComponent(PROP_PARSER);
			
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
						FormulaIU previousFIU = firstUsefulFormulaIU;
						if (previousCa.grounds().size() > 0) {
							previousFIU = (FormulaIU) states.get(previousCa);
						}
						List<String> lastDerive = ca.getCandidateAnalysis().getLastDerive();
						logger.debug("-------");
						Formula newForm = new Formula(previousFIU.getFormula());
						for (String rule : lastDerive) {
							logger.debug("= "+newForm.toRMRSString());
							// go through all new syntactic rule applications
							if (rule.startsWith("m(")) {
								// the current rule is a lexical one; build lexical formula. 
								String tag = rule.substring(2, rule.length()-1);						
								Variable.Type type = semanticTypesOfTags.get(tag);
								String lexname = tag.toUpperCase(); // if no better information is there
								// find the word/lemma
								TagIU tiu = (TagIU) ca.groundedIn().get(0);
								WordIU wiu = (WordIU) tiu.groundedIn().get(0);
								if (wiu != null) {
									List<String> l = wiu.getValues("lemma");
									if (l.size() > 0) {
										lexname = l.get(0);
									}									
								}
								List<AVPair> pairs = wiu.getAVPairs();
								logger.debug("P "+pairs);
								
								Formula lexitem = new Formula(lexname, type);
								logger.debug("+ "+rule+"\n+ "+lexitem.toRMRSString());
								newForm.forwardCombine(lexitem);
							} else {
								// the current rule is a syntactic one; get rule semantics from map.
								Formula rulesem = semanticRules.get(rule);
								logger.debug("+ "+rule+"\n+ "+rulesem.toRMRSString());
								newForm.forwardCombine(rulesem);
							}
							logger.debug("= "+newForm.toRMRSString());
							newForm.reduce();
							//newForm.renumber(0);
							logger.debug("= "+newForm.toRMRSString());
							logger.debug(newForm.getNominalAssertions());
						}
						FormulaIU newFIU = new FormulaIU(previousFIU, ca, newForm);
						newEdits.add(new EditMessage<FormulaIU>(EditType.ADD, newFIU));
						states.put(ca,newFIU);
						if (!resolver.resolves(newForm)) {
							parser.degradeAnalysis(ca, MALUS_NO_REFERENCE_RESOLUTION);
						}
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

	/**
	 * Interface to call whenever a Formula needs to check if its relations
	 * resolve with something in the world.
	 * @author okko
	 *
	 */
	public interface Resolver extends Configurable {
		/**
		 * Called whenever a new FormulaIU is created to determine
		 * if its relations match something in the world.
		 * @param f the formula
		 * @return true of f's relations hold in the world, i.e. in the implementing class
		 */
		public boolean resolves(Formula f);
	}

}
