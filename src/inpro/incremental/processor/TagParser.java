package inpro.incremental.processor;

import inpro.incremental.IUModule;
import inpro.incremental.unit.CandidateAnalysisIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.TagIU;
import inpro.irmrsc.parser.CandidateAnalysis;
import inpro.irmrsc.parser.SITDBSParser;
import inpro.irmrsc.simplepcfg.Grammar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4String;

/**
 * An IU processor wrapper around the {@link SITDBSParser}. For any incoming {@link TagIU} representing the
 * next element in a string of part-of-speech tags, the processor outputs the possible partial
 * parses in form of {@link CandidateAnalysisIU}s.
 * @author Andreas Peldszus
 */
public class TagParser extends IUModule {

	@S4String()
	public final static String PROP_GRAMMAR = "grammarFile";
	private String grammarFile;
	
	@S4Double(defaultValue = 0.001)
	public final static String PROP_BASE_BEAM_FACTOR = "baseBeamFactor";
	private double baseBeamFactor;
	
	@S4Boolean(defaultValue = true)
	public final static String PROP_BE_ROBUST = "beRobust";
	private boolean beRobust;
	
	/** keeps track of all parsing states for a given TagIU **/
	private Map<TagIU,SITDBSParser> states = new HashMap<TagIU,SITDBSParser>();
	
	/** keeps track of all analyses (in order to be able to find the right IU for a given CA). */
	private LinkedList<CandidateAnalysisIU> analyses = new LinkedList<CandidateAnalysisIU>();
	
	private HashMap<String, CandidateAnalysisIU> indexedAnalyses = new HashMap<String, CandidateAnalysisIU>();
	
	SITDBSParser p;
	Grammar g;
	//private String startSymbol; // was this used anywhere?
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		
		// set Robustness
		beRobust = ps.getBoolean(PROP_BE_ROBUST);
		SITDBSParser.setRobust(beRobust);
		
		// load grammar
		grammarFile = ps.getString(PROP_GRAMMAR);
		g = new Grammar();
		try {
			g.loadXML(new URL(grammarFile));
			//startSymbol = g.getStart().getSymbol();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		
		// initialize first parser
		baseBeamFactor = ps.getDouble(PROP_BASE_BEAM_FACTOR);
		p = new SITDBSParser(g, baseBeamFactor);
		p.setLogger(this.logger);
		this.states.put(TagIU.FIRST_TAG_IU, p);
	}
	
	/** finds all payload-string-equal IUs for a given CandidateAnalysis **/
	private List<CandidateAnalysisIU> findIU(CandidateAnalysis ca) {
		List<CandidateAnalysisIU> l = new ArrayList<CandidateAnalysisIU>(1);
//		Iterator<CandidateAnalysisIU> i = analyses.iterator();
//		while(i.hasNext()) {
//			CandidateAnalysisIU iu = i.next();
			//TODO this is a hack: I compare the fullstring-representation of the CAs. a better alternative would be to
			// properly implement equal-like functions for CA and all subobjects.
//			if(ca.toFullString().equals(iu.getCandidateAnalysis().toFullString())) {
			String fullString = ca.toFullString();
			if (indexedAnalyses.containsKey(fullString))
				l.add(indexedAnalyses.get(fullString));
//			}
//		}
		return l;
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		List<EditMessage<CandidateAnalysisIU>> newEdits = new ArrayList<EditMessage<CandidateAnalysisIU>>();
		for (EditMessage<? extends IU> edit : edits) {
			TagIU tag = (TagIU) edit.getIU();
			switch (edit.getType()) {
				case REVOKE:
					for (IU ca : tag.grounds()) {
						if (ca.toPayLoad().equals(TagIU.FIRST_TAG_IU.toPayLoad())) {
							analyses.clear();
							states.clear();
							indexedAnalyses.clear();
							p = new SITDBSParser(g, baseBeamFactor);
							p.setLogger(this.logger);
							states.put(TagIU.FIRST_TAG_IU, p);							
						}
					}
					break;
					
				case ADD:
					while (analyses.size() >  5) analyses.pop();
					// TODO: find a better root element
					TagIU previousTag = (TagIU) tag.getSameLevelLink();
					assert previousTag != null;
					if (previousTag.toPayLoad().equals(TagIU.FIRST_TAG_IU.toPayLoad())) {
						analyses.clear();
						states.clear();
						indexedAnalyses.clear();
						p = new SITDBSParser(g, baseBeamFactor);
						p.setLogger(this.logger);
						states.put(TagIU.FIRST_TAG_IU, p);
					}
//					System.out.println(tag + " " + previousTag + " " +this.states.get(previousTag));
					if (!states.containsKey(previousTag)) {
						previousTag = (TagIU) previousTag.getSameLevelLink();
					}
					SITDBSParser newState = new SITDBSParser(this.states.get(previousTag));
					newState.feed(tag.toPayLoad());
					this.states.put(tag,newState);
					for (CandidateAnalysis ca : newState.getQueue()) {
						CandidateAnalysisIU sll = CandidateAnalysisIU.FIRST_CA_IU;
						CandidateAnalysis ante = ca.getAntecedent();
						if (ante != null) {
							prepareAntecedents();
							List<CandidateAnalysisIU> potentialAntecedents = findIU(ante);
							for (CandidateAnalysisIU potentialAnte : potentialAntecedents) {
								if (potentialAnte != null && potentialAnte.groundedIn().contains(previousTag)) {
									sll = potentialAnte;
									break;
								} 
							}
						}
						CandidateAnalysisIU newCAIU = new CandidateAnalysisIU(sll, tag, ca);
						analyses.add(newCAIU);
						newEdits.add(new EditMessage<CandidateAnalysisIU>(EditType.ADD, newCAIU));
					}
					break;
					
				case COMMIT:
					for (IU iu : tag.grounds()) {
						if (iu instanceof CandidateAnalysisIU) {
							newEdits.add(new EditMessage<CandidateAnalysisIU>(EditType.COMMIT, (CandidateAnalysisIU) iu));
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		this.rightBuffer.setBuffer(newEdits);
	}

	private void prepareAntecedents() {
		for (CandidateAnalysisIU ciu : analyses) {
			indexedAnalyses.put(ciu.getCandidateAnalysis().toFullString(), ciu);
		}
	}

	/** degrades the analysis encapsuled in the given CandidateAnalysisIU by the given malus **/
	public void degradeAnalysis(CandidateAnalysisIU caiu, double malus) {
		// find the parser state containing this candidate analysis and modify its weight
		TagIU tagiu = (TagIU) caiu.groundedIn().get(0);
		SITDBSParser parserStateToModify = this.states.get(tagiu);
		parserStateToModify.degradeAnalysis(caiu.getCandidateAnalysis(), malus);
	}
	
	/** print the parser status for this CandidateAnalysisIU **/
	public void printStatus(CandidateAnalysisIU caiu) {
		// TODO add checks
		TagIU tiu = (TagIU) caiu.groundedIn().get(0); // a CA should only be grounded in one tag
		states.get(tiu).status();
	}
}
