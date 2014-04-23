package inpro.incremental.processor;

import inpro.incremental.IUModule;
import inpro.incremental.unit.CandidateAnalysisIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.FormulaIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IUList;
import inpro.incremental.unit.RelationIU;
import inpro.incremental.unit.TagIU;
import inpro.incremental.unit.WordIU;
import inpro.irmrsc.parser.CandidateAnalysis;
import inpro.irmrsc.rmrs.Entity;
import inpro.irmrsc.rmrs.Formula;
import inpro.irmrsc.rmrs.Relation;
import inpro.irmrsc.rmrs.Relation.Type;
import inpro.irmrsc.rmrs.Variable;
import inpro.irmrsc.util.RMRSLoader;
import inpro.irmrsc.util.SemanticMacro;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4String;

//TODO: This class contains still a lot of evaluation specific code. Needs a clean up.

/**
 * An incremental processor that applies to a semantic representation all updates 
 * corresponding to the syntactic operations of the last incremental step. This
 * implements an incremental rule-to-rule syntax-semantic-interface.
 * <p />
 * For each parsing action in the last processing steps of a given syntactic derivation
 * (i.e. a {@link CandidateAnalysisIU}), the associated semantic actions are applied
 * to the corresponding semantic representation (i.e. a {@link FormulaIU}).<br/>
 * The semantic actions, mapping from syntactic rule to semantic action and the basic
 * semantic representation of lexemes are defined in separate xml files.
 * <p />
 * If {@link #resolveReferences} is set accordingly, the composer can call a
 * {@link Resolver} to test whether the semantic representations successfully refer to 
 * objects in the world (specified by a world setting). If {@link #referencePruning},
 * too, is set accordingly, the composer will provide feedback to the parser by requesting
 * him to degrade those syntactic derivations that do not successfully refer. If both are
 * set to false, the processor runs in a pure 'semantic combination only'-mode.
 * <p />
 * (This class contains still a lot of evaluation specific code. Needs a clean up.)
 * @author Andreas Peldszus
 */
public class RMRSComposer extends IUModule {
	
	// resources
	
	@S4String()
	public final static String PROP_SEM_MACROS_FILE = "semMacrosFile";
	private String semMacrosFile;
	/** a register of semantic macros identified by their long name (as defined in {@link #PROP_SEM_MACROS_FILE}) */
	private Map<String,Formula> semanticMacrosLongname = new HashMap<String,Formula>();
	/** a register of semantic macros identified by their short name (as defined in {@link #PROP_SEM_MACROS_FILE}) */
	private Map<String,Formula> semanticMacrosShortname = new HashMap<String,Formula>();
	
	@S4String()
	public final static String PROP_SEM_RULES_FILE = "semRulesFile";
	private String semRulesFile;
	/** maps syntactic rule names to semantic macros (as defined in the {@link #PROP_SEM_RULES_FILE}) */
	private Map<String,Formula> semanticRules  = new HashMap<String,Formula>();
	
	@S4String()
	public final static String PROP_TAG_LEXICON_FILE = "tagLexiconFile";
	private String tagLexiconFile;
	/** maps POS-tags to variable type (as defined in the {@link #PROP_TAG_LEXICON_FILE}) */
	private Map<String,Variable.Type> semanticTypesOfTags = new HashMap<String,Variable.Type>();
	
	
	// reference resolution
	
	@S4Boolean(defaultValue = false)
	public final static String PROP_RESOLVE_REFERENCES = "resolveReferences";
	/** whether or not the composer should use a resolver to resolve references */
	private boolean resolveReferences;
	
	@S4Component(type = Resolver.class, mandatory = false)
	public final static String PROP_RESOLVER = "resolver";
	/** The resolver to be used if {@link #resolveReferences} is true. */
	private Resolver resolver;
	
	
	// reference pruning 
	
	@S4Boolean(defaultValue = false)
	public final static String PROP_REFERENCE_PRUNING = "referencePruning";
	/** whether or not the parser should be requested to degrade non-referring analyses */
	private boolean referencePruning;
	
	@S4Double(defaultValue = 0.01)
	public final static String PROP_MALUS_NO_REFERENCE = "malusNoReference";
	/** the malus that is sent to the parser to degrade non-referring analyses by */
	private double malusNoReference;
	
	@S4Component(type = TagParser.class)
	public static final String PROP_PARSER = "parser";
	/** Reference to the syntactic processor providing the derivations. Required in the
	 *  reference pruning setting to request the degradation of derivations, and also 
	 *  required for evaluation to request parser statistics. */
	protected TagParser parser;
	
	
	// evaluation specific
	
	@S4Boolean(defaultValue = false)
	public final static String PROP_EVALUATE = "evaluate";
	/** whether or not the composer should evaluate, i.e. compare its (resolution) output with a gold representation */
	private boolean evaluate;
	
	@S4String(defaultValue = "")
	public final static String PROP_GOLD = "gold";
	/** Gold representation, i.e. a string representation of a domain object that a composer/resolver should arrive at */
	private String gold;
	/** Gold representation, i.e. a string representation of a domain action that a composer/resolver should arrive at */
	private String goldaction;
	/** The history of compare-to-gold values of each incremental step with 1best evaluation */
	private static List<Integer> compareToGoldValueHistory1Best;
	/** The history of compare-to-gold values of each incremental step with 5best evaluation */
	private static List<Integer> compareToGoldValueHistory5Best;

	
	// ... and most importantly
	
	/** The composers internal storage: maps syntactic derivations to semantic representations. */
	private Map<CandidateAnalysisIU,FormulaIU> states = new HashMap<CandidateAnalysisIU,FormulaIU>();

	/** the semantic representation to start with */
	private static FormulaIU firstUsefulFormulaIU;	
	
	IUList<RelationIU> prevList;
	TreeSet<String> acceptedEntityTypes;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {		
		super.newProperties(ps);
		
		prevList = new IUList<RelationIU>(); // start off with an empty word list so the diff is all the new stuff
		acceptedEntityTypes  = new TreeSet<String>(); 
		acceptedEntityTypes.add("e");
		acceptedEntityTypes.add("x");
		acceptedEntityTypes.add("i");
		acceptedEntityTypes.add("#");
		
		// TODO: this is weird 
		//PentoRMRSResolver.setLogger(logger);
		
		// set up whether to resolve references
		resolveReferences = ps.getBoolean(PROP_RESOLVE_REFERENCES);
		logger.info("Setting resolveReferences to "+resolveReferences);
		
		// set up the resolver
		this.resolver = (Resolver) ps.getComponent(PROP_RESOLVER);
		if (this.resolver == null) {
			if (resolveReferences) {
				logger.warn("Not using a resolver. Changing resolveReferences to false.");
				resolveReferences = false;
			} else {
				logger.info("Not using a resolver.");
			}	
		} else {
			logger.info("Set up resolver: " + this.resolver.toString());
		}
		
		// set up whether to do reference pruning
		referencePruning = ps.getBoolean(PROP_REFERENCE_PRUNING);
		if (referencePruning && (! resolveReferences)) {
			logger.warn("Setting referencePruning to "+referencePruning+" because resolveReferences is false.");
			referencePruning = false;
		} else {
			logger.info("Setting referencePruning to "+referencePruning);
		}
		
		// set up the reference pruning malus
		malusNoReference = ps.getDouble(PROP_MALUS_NO_REFERENCE);
		logger.info("Setting malusNoReference to "+malusNoReference);
		
		// set up whether to evaluate
		evaluate = ps.getBoolean(PROP_EVALUATE);
		if (evaluate && (! resolveReferences)) {
			logger.warn("Setting evaluate to "+evaluate+" because resolveReferences is false.");
			evaluate = false;
		} else {
			logger.info("Setting evaluate to "+evaluate);
		}
		
		// set up evaluation gold string and extract gold action and gold tile
		String tmp = ps.getString(PROP_GOLD);
		if (tmp.equals("")) {
			gold=null;
			goldaction=null;
		} else {
			String[] tmp2 = tmp.split(" ");
			goldaction = tmp2[0];
			gold = tmp2[1];
		}
		logger.info("Setting gold semantics to action="+goldaction+" tile="+gold);
		
		// initialize resolve value history for evaluation
		compareToGoldValueHistory1Best = new ArrayList<Integer>();
		compareToGoldValueHistory5Best = new ArrayList<Integer>();
		
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
				
			// set up the parser
			this.parser = (TagParser) ps.getComponent(PROP_PARSER);
			
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		List<EditMessage<FormulaIU>> newEdits = new ArrayList<EditMessage<FormulaIU>>();
		boolean iGotAdds = false;
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
					//******************************************************************
					// A new candidate analysis has been added. Now, the following steps
					// are taken to construct the corresponding semantic outcome:
					//  1. Find the preceding syntactic IU and its semantic IU.
					//  2. Get the latest derivational steps from the current syntactic IU
					//     and iterate over them. For each step, produce a corresponding
					//     semantic increment and add it to the current semantics. 
					//     Derivational steps are [see CandidateAnalysis]:
					//       a) normal match (add lexical semantics of the token)
					//       b) robust match: repairs / deletions (add the token's default semantics)
					//       c) robust match: insertions (add the token's default semantics without consuming a slot)
					//       d) rule expansion (add rule semantics)
					//  3. Add the output formula to the outgoing newEdits.
					//  4. ReferencePruning (optional): resolve the output formula and
					//     degrade the analysis of non-resolving formulas
					//******************************************************************
					
					iGotAdds = true;
					
					// ## 1. find antecedent IUs
					CandidateAnalysisIU previousCa = (CandidateAnalysisIU) ca.getSameLevelLink();
					if (previousCa != null) {
						FormulaIU previousFIU = firstUsefulFormulaIU;
						if (previousCa.grounds().size() > 0) {
							previousFIU = states.get(previousCa);
						}
						List<String> lastDerive = ca.getCandidateAnalysis().getLastDerive();
						//logger.debug(logPrefix+"-------");
						//System.err.println("-------: "+lastDerive.toString());
						Formula newForm = new Formula(previousFIU.getFormula());
						
						boolean missingSemanticRule = false;
						
						// ## 2. go through all new syntactic rule applications
						for (String rule : lastDerive) {
							missingSemanticRule = false;
							
							//logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							//System.err.println("[C] = "+newForm.toStringOneLine());
							
							// ## a) normal match (add lexical semantics of the token)
							if (rule.startsWith("m(")) {
								String tag = rule.substring(2, rule.length()-1);
								if (tag.equals("S!")) {
									// this is the sentence end marker, signaling that nothing more is to come
									// do not change the new formula iu
								} else {
									// the current rule is a lexical one; build lexical formula.						
									Variable.Type type = semanticTypesOfTags.get(tag);
									if (! semanticTypesOfTags.containsKey(tag)) {
										logger.debug("The POS-tag '"+tag+"' has no specified semantic type. Using underspecified index type.");
										type = Variable.Type.INDEX;
									}
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
									//logger.debug(logPrefix+"P "+wiu.getAVPairs());									
									Formula lexitem = new Formula(lexname, type);
									//logger.debug(logPrefix+"+ "+rule);
									//logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
									newForm.forwardCombine(lexitem);
								}
								
							// ## b) robust match: repairs / deletions (add the token's default semantics)
							// TODO: why should repaired tokens only be shown by the repaired POStag and not by their lexical name? better discriminate between repairs and deletions
							} else if (rule.startsWith("r(") || rule.startsWith("d(")) {
								String tag = rule.substring(2, rule.length()-1);
								Variable.Type type = semanticTypesOfTags.get(tag);
								if (! semanticTypesOfTags.containsKey(tag)) {
									logger.debug("The POS-tag '"+tag+"' has no specified semantic type. Using underspecified index type.");
									type = Variable.Type.INDEX;
								}
								String lexname = tag.toUpperCase();
								Formula lexitem = new Formula(lexname, type);
								//logger.debug(logPrefix+"+ "+rule);
								//logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
								newForm.forwardCombine(lexitem);
																
							// ## c) robust match: insertions (add the token's default semantics without consuming a slot)
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
								//logger.debug(logPrefix+"P "+wiu.getAVPairs());									
								Formula lexitem = new Formula(lexname, type);
								//logger.debug(logPrefix+"+ "+rule);
								//logger.debug(logPrefix+"+ "+lexitem.toStringOneLine());
								newForm.simpleAdd(lexitem);
								
							// ## d) rule expansion (add rule semantics)	
							} else {
								Formula rulesem = semanticRules.get(rule);
								//logger.debug(logPrefix+"+ "+rule);
								//logger.debug(logPrefix+"+ "+rulesem.toStringOneLine());
								// TODO: check if rulesem is given!
								if (rulesem == null) {
									logger.fatal("No semantic rule specified for the syntactic rule '"+rule+"'! Skipping this reading.");
									missingSemanticRule = true;
									break;
								} else {
									newForm.forwardCombine(rulesem);
								}
							}

							// reduce formula
							//logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							newForm.reduce();
							//newForm.renumber(0);
							//logger.debug(logPrefix+"= "+newForm.toStringOneLine());
							//logger.debug(logPrefix+"> "+newForm.getNominalAssertions());
							
						}
						
						if (missingSemanticRule) {
							// do not output anything for this problematic reading, just skip to the next edit message.
							// TODO: this somehow produces strange IU networks. not yet clear to me why...
							continue;
						}
						
						// ## 3. Add the output formula to the outgoing newEdits.
						//System.err.println("[C] = "+newForm.toStringOneLine());
						FormulaIU newFIU = new FormulaIU(previousFIU, ca, newForm);
						newEdits.add(new EditMessage<FormulaIU>(EditType.ADD, newFIU));
						states.put(ca,newFIU);
						
						// ## 4. ReferencePruning (optional): resolve the output formula and
						//       degrade the analysis of non-resolving formulas

						if (referencePruning && resolveReferences) {
							if (!ca.getCandidateAnalysis().isComplete()) {
								resolver.setPerformDomainAction(false); // don't show the resolved items now
								int resolve = resolver.resolves(newForm);
								switch (resolve) {
								case -1: 
									//System.err.println("DEGRADE");
									parser.degradeAnalysis(ca, malusNoReference);
									break;
								case 0:
									//System.err.println("IGNORE");
									break;
								case 1:
									//System.err.println("ENCOURAGE");
									break;
								default :
									break;
								}
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
								if (resolveReferences && evaluate) {
									int resolution = 2;
									if (gold != null) {
										resolution = resolver.resolvesObject(((FormulaIU)sem).getFormula(), gold);
									}
									String syn = ca.getCandidateAnalysis().toFinalString();
									String rmrs = ((FormulaIU)sem).getFormula().toStringOneLine();
									String mrs = ((FormulaIU)sem).getFormula().getUnscopedPredicateLogic().toString();
									double iscore1 = calculateIncrementalScore(compareToGoldValueHistory1Best);
									double iscore5 = calculateIncrementalScore(compareToGoldValueHistory5Best);
									logger.warn("[Q] SYN "+syn);
									logger.warn("[Q] SEM "+rmrs);
									logger.warn("[Q] MRS "+mrs);
									logger.warn("[Q] RES "+resolution);
									logger.warn("[Q] HS1 "+compareToGoldValueHistory1Best);
									logger.warn("[Q] IS1 "+iscore1);
									logger.warn("[Q] HS5 "+compareToGoldValueHistory5Best);
									logger.warn("[Q] IS5 "+iscore5);
									logger.warn("[Q] ALL "+syn+"\t"+rmrs+"\t"+mrs+"\t"+resolution+"\t"+compareToGoldValueHistory1Best+"\t"+iscore1+"\t"+resolution+"\t"+compareToGoldValueHistory5Best+"\t"+iscore5);
								} else {
									parser.printStatus(ca);
								}
							}
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		
		if (resolveReferences) {
			// find the highest ranked analysis
			FormulaIU bestFoIU = null;
			double bestProb = 0;
			int bestLexemCount = 0;
			for (EditMessage<FormulaIU> em : newEdits) {
				CandidateAnalysis ca = ((CandidateAnalysisIU) em.getIU().groundedIn().get(0)).getCandidateAnalysis();
				int thisLexemCount = ca.getNumberOfMatches();
				double thisProb = ca.getProbability();			 
				if (thisProb > bestProb) {
					bestProb = thisProb;
					bestLexemCount = thisLexemCount;
					bestFoIU = em.getIU();			
				} else if (thisProb == bestProb) {
					if (thisLexemCount > bestLexemCount) {
						bestProb = thisProb;
						bestLexemCount = thisLexemCount;
						bestFoIU = em.getIU();	
					}
				}
			}
			// show the resolving objects of the highest ranked analysis in the gui
			if (bestFoIU != null) {
				resolver.setPerformDomainAction(true);
				resolver.resolves(bestFoIU.getFormula());
				resolver.setPerformDomainAction(false);
			}
			
			// if we got ADDs, evaluate the new result according to one of our two evaluation methods.
			if (iGotAdds && evaluate && (gold != null)) {
				// we compare the gold tile with the resolves set of ..
				
				// ## 1) the syntactic 1best candidate analysis
				int cmpToGoldValue = -2; // default if no best ca is there.
				if (bestFoIU != null) // we already have that from above
					cmpToGoldValue = resolver.resolvesObject(bestFoIU.getFormula(), gold);
				// add to history
				compareToGoldValueHistory1Best.add(cmpToGoldValue);
				System.err.println("[E1] cmp2gold history:"+compareToGoldValueHistory1Best);
					
				// ## 2) the best resolving analysis of the 5best CAs
				// get all candidate analyses
				PriorityQueue<CandidateAnalysisIU> allCaIUs = new PriorityQueue<CandidateAnalysisIU>(5, new CandidateAnalysisIUProbabilityComparator());
				for (EditMessage<FormulaIU> em : newEdits) {
					allCaIUs.add((CandidateAnalysisIU) em.getIU().groundedIn().get(0)); 
				}
				int cnt = 0;
				
				// get the five best and find the best resolving in it
				bestFoIU = null;
				while (cnt < 5 && (! allCaIUs.isEmpty())) {
					CandidateAnalysisIU next = allCaIUs.poll();
					//System.err.println("# consider 5best p="+next.getCandidateAnalysis().getProbability());
					cnt++;
					// resolves this formula:
					FormulaIU nextFoIU = (FormulaIU) next.grounds().get(0);
					int resolve = resolver.resolves(nextFoIU.getFormula());
					if (resolve == 1) {
						// we found a unique resolve: eval this
						bestFoIU = nextFoIU;
						break;
					} else if (resolve == 0) {
						// we found a set resolve: store this one, if it is the first one
						if (bestFoIU == null) {
							bestFoIU = nextFoIU;
						}
					} else {
						// we found a none resolve: store this one, if it is the first one
						if (bestFoIU == null) {
							bestFoIU = nextFoIU;
						}
					}
				}
				// compare the best resolving one to gold
				if (bestFoIU != null) {
					cmpToGoldValue = resolver.resolvesObject(bestFoIU.getFormula(), gold);
					compareToGoldValueHistory5Best.add(cmpToGoldValue);
					System.err.println("[E5] cmp2gold history:"+compareToGoldValueHistory5Best);
				} else {
					System.err.println("Fatal evaluation error: Couldn't find the best formula IU!");
				}
			}
		}
		
		// finish
		
		
		IUList<RelationIU> list = new IUList<RelationIU>();
		
		for (EditMessage<FormulaIU> formulaEdit : newEdits) {
			System.out.println(formulaEdit.getIU().getFormula().toRMRSString());
			
			Formula formula = formulaEdit.getIU().getFormula();
			RelationIU prev = RelationIU.FIRST_RELATION_IU;
		
			for (Relation relation : formula.getRelations()) {
	
				RelationIU riu = new RelationIU(relation, formula);
				riu.groundIn(formulaEdit.getIU().grounds());
				riu.setSameLevelLink(prev);
				list.add(riu);
				
				prev = riu;
				
			}
			break;
		
		}
		
		List<EditMessage<RelationIU>> diffs = prevList.diffByPayload(list);
		prevList = list;
		
		
		for (EditMessage<RelationIU> iu : diffs) {
			System.out.println(iu);
		}
		System.out.println();
		
		this.rightBuffer.setBuffer(newEdits);
	}

	private double calculateIncrementalScore(List<Integer> history) {
		double iscore = 0;
		int m = history.size();
		if (m != 0) {
			for (int n=1; n<=m; n++) {
				iscore += (1.0 * history.get(n-1) * n / m);
			}
		} else {
			// empty histories
			iscore = -999;
		}
		return iscore;
	}
	
	/**
	 * Sets the gold string rep in case one wants to evaluate if a composer/resolver did the right thing.
	 * @param gold the new gold
	 */
	public void setGold(String gold) {
		this.gold = gold;
	}

	/**
	 * Interface to call whenever a Formula needs to check if its relations
	 * resolve with something in the world.
	 * @author okko
	 *
	 */
	public interface Resolver extends Configurable {

		/**
		 * A resolve method called whenever a new FormulaIU is created to determine
		 * if its relations resolve something in the world.
		 * @param f the formula
		 * @return -1 if nothing resolved for any predicate argument, 0 if many world objects resolve for all args, 1 for at least one arg only one world object resolves.
		 */
		public int resolves(Formula f);

		/**
		 * A resolve method called whenever a new Formula is created to determine
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
		 * A resolve method called to determine if a particular object, identified by a string argument, was among the domain objects that resolved (or indeed the only one that did) <br/>
		 * Implementers need to make sure that comparison of the string representation and domain objects takes place.
		 * @param f the formula
		 * @param id a string representation of the object 
		 * @return -1 if nothing resolved, 0 if id was among the resolved objects, 1 if only id was among the resolved objects
		 */
		public int resolvesObject(Formula f, String id);

		/**
		 * A setter called to turn off/on domain actions.
		 * @param action on or off
		 */
		public void setPerformDomainAction(boolean action);

	}

	private class CandidateAnalysisIUProbabilityComparator implements Comparator<CandidateAnalysisIU> {
	    @Override
	    public int compare(CandidateAnalysisIU x, CandidateAnalysisIU y) {
	    	double px = x.getCandidateAnalysis().getProbability();
	    	double py = y.getCandidateAnalysis().getProbability();
	        return Double.compare(py, px);
	    }
	}
	
	public Resolver getResolver() {
		
		return this.resolver;
		
	}	

}
