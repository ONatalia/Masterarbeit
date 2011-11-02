package org.cocolab.inpro.incremental.processor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.domains.pentomino.nlu.PentoRMRSResolver;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.CandidateAnalysisIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.FormulaIU;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.TagIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.irmrsc.parser.CandidateAnalysis;
import org.cocolab.inpro.irmrsc.rmrs.Formula;
import org.cocolab.inpro.irmrsc.rmrs.SemanticMacro;
import org.cocolab.inpro.irmrsc.rmrs.Variable;
import org.cocolab.inpro.irmrsc.util.RMRSLoader;
import org.cocolab.inpro.nlu.AVPair;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
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
	
	@S4Double(defaultValue = 0.01)
	public final static String PROP_MALUS_NO_REFERENCE = "malusNoReference";
	private double malusNoReference;
	
	private Map<String,Formula> semanticMacrosLongname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticMacrosShortname = new HashMap<String,Formula>();
	private Map<String,Formula> semanticRules  = new HashMap<String,Formula>();
	private Map<String,Variable.Type> semanticTypesOfTags = new HashMap<String,Variable.Type>();
	private Map<CandidateAnalysisIU,FormulaIU> states = new HashMap<CandidateAnalysisIU,FormulaIU>();

	private static FormulaIU firstUsefulFormulaIU;
	private final static double MALUS_SEMANTIC_MISMATCH = 0.7;
	private static String logPrefix = "[C] ";
	
	@SuppressWarnings("unchecked")
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		malusNoReference = ps.getDouble(PROP_MALUS_NO_REFERENCE);
		PentoRMRSResolver.setLogger(logger);
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
						logger.debug(logPrefix+"-------");
						Formula newForm = new Formula(previousFIU.getFormula());
						for (String rule : lastDerive) {
							logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							// go through all new syntactic rule applications
							if (rule.startsWith("m(")) {
								// parser match
								String tag = rule.substring(2, rule.length()-1);
								if (tag.equals("S!")) {
									// this is the sentence end marker, signaling that nothing more is to come
									// do not change the new formula iu
								} else {
									// the current rule is a lexical one; build lexical formula.						
									Variable.Type type = semanticTypesOfTags.get(tag);
									String lexname = tag.toUpperCase(); // fallback to this if no better information is there
									// find the word/lemma
									TagIU tiu = (TagIU) ca.groundedIn().get(0);
									WordIU wiu = (WordIU) tiu.groundedIn().get(0);
									if (wiu != null) {
										List<String> l = wiu.getValues("lemma");
										if (l.size() > 0) {
											lexname = l.get(0);
										}									
									}
									logger.debug(logPrefix+"P "+wiu.getAVPairs());									
									Formula lexitem = new Formula(lexname, type);
									logger.debug(logPrefix+"+ "+rule);
									logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
									newForm.forwardCombine(lexitem);
								}
							} else if (rule.startsWith("r(") || rule.startsWith("d(")) {
								String tag = rule.substring(2, rule.length()-1);
								Variable.Type type = semanticTypesOfTags.get(tag);
								String lexname = tag.toUpperCase();
								Formula lexitem = new Formula(lexname, type);
								logger.debug(logPrefix+"+ "+rule);
								logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
								newForm.forwardCombine(lexitem);
							} else if (rule.startsWith("i(")) {
								String tag = rule.substring(2, rule.length()-1);
								Variable.Type type = Variable.Type.INDEX;
								String lexname = tag.toUpperCase();
								TagIU tiu = (TagIU) ca.groundedIn().get(0);
								WordIU wiu = (WordIU) tiu.groundedIn().get(0);
								try {
									if (wiu != null) {
										List<String> l = wiu.getValues("lemma");
										if (l.size() > 0) {
											lexname = l.get(0);
										}									
									}
								} catch (NullPointerException e) {}
								logger.debug(logPrefix+"P "+wiu.getAVPairs());									
								Formula lexitem = new Formula(lexname, type);
								logger.debug(logPrefix+"+ "+rule);
								logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
								newForm.simpleAdd(lexitem);
							} else {
								// the current rule is a syntactic one; get rule semantics from map.
								Formula rulesem = semanticRules.get(rule);
								logger.debug(logPrefix+"+ "+rule);
								logger.debug(logPrefix+"+ "+rulesem.toStringOneLine());
								newForm.forwardCombine(rulesem);
							}
							logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							newForm.reduce();
							//newForm.renumber(0);
							logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							logger.debug(logPrefix+"> "+newForm.getNominalAssertions());
						}
						// create the new formula iu and store it
						FormulaIU newFIU = new FormulaIU(previousFIU, ca, newForm);
						newEdits.add(new EditMessage<FormulaIU>(EditType.ADD, newFIU));
						states.put(ca,newFIU);
						// if the analysis is not complete, try to resolve the formula, and degrade in case of failure
						if (!ca.getCandidateAnalysis().isComplete()) {
							resolver.setPerformDomainAction(false); // don't show the resolved items now
							if (!resolver.resolves(newForm)) {
								parser.degradeAnalysis(ca, malusNoReference);
							}
						}
					} else {
						//TODO: can this happen?
					}
					break;
				case COMMIT:
					for (IU sem : ca.grounds()) {
						if (sem instanceof FormulaIU) {
							newEdits.add(new EditMessage<FormulaIU>(EditType.COMMIT, (FormulaIU) sem));
							// print out full statistics for this sem if it is complete.
							if (ca.getCandidateAnalysis().isComplete()) {
								parser.printStatus(ca);
								logger.warn("[Q] SYN "+ca.getCandidateAnalysis().toFinalString());
								logger.warn("[Q] SEM "+((FormulaIU)sem).getFormula().toStringOneLine());
								logger.warn("[Q] MRS "+((FormulaIU)sem).getFormula().getUnscopedPredicateLogic());
								logger.warn("[Q] ALL "+ca.getCandidateAnalysis().toFinalString()+"\t"+((FormulaIU)sem).getFormula().toStringOneLine()+"\t"+((FormulaIU)sem).getFormula().getUnscopedPredicateLogic());
							}
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		// find the highest ranked ca and show its resolves
		FormulaIU best = null;
		double bestProb = 0;
		int bestLexemCount = 0;
		for (EditMessage<FormulaIU> em : newEdits) {
			CandidateAnalysis ca = ((CandidateAnalysisIU) em.getIU().groundedIn().get(0)).getCandidateAnalysis();
			int thisLexemCount = ca.getNumberOfMatches();
			double thisProb = ca.getProbability();			 
			if (thisProb > bestProb) {
				bestProb = thisProb;
				bestLexemCount = thisLexemCount;
				best = em.getIU();			
			} else if (thisProb == bestProb) {
				if (thisLexemCount > bestLexemCount) {
					bestProb = thisProb;
					bestLexemCount = thisLexemCount;
					best = em.getIU();	
				}
			}
		}
		if (best != null) {
			resolver.setPerformDomainAction(true);
			resolver.resolves(best.getFormula());
			resolver.setPerformDomainAction(false);
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
		 * if its relations resolve something in the world.
		 * @param f the formula
		 * @return true if f's predicates all resolve something in the world
		 */
		public boolean resolves(Formula f);

		/**
		 * Called whenever a new Formula is created to determine
		 * what domain concepts it resolves in the world.
		 * <br />
		 * The map returns maps (Integer) predicate arguments to possible world objects.
		 * <br />
		 * This can be used to further validate the Formula.
		 *  
		 * @param f the formula
		 * @return a map of Formula predicate arguments to lists of objects that the argument can stand for
		 */
		public Map<Integer, List<? extends Object>> resolvesObjects(Formula f);

		/**
		 * A setter called to turn off/on domain actions.
		 * @param action on or off
		 */
		public void setPerformDomainAction(boolean action);
	}

}
