package org.cocolab.inpro.incremental.processor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.CandidateAnalysisIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.TagIU;
import org.cocolab.inpro.irmrsc.parser.CandidateAnalysis;
import org.cocolab.inpro.irmrsc.parser.SITDBSParser;
import org.cocolab.inpro.irmrsc.simplepcfg.Grammar;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4String;

public class TagParser extends IUModule {

	@S4String()
	public final static String PROP_GRAMMAR = "grammarFile";
	private String grammarFile;
	
	@S4Double()
	public final static String PROP_BASE_BEAM_FACTOR = "baseBeamFactor";
	private double baseBeamFactor;
	
	private Map<TagIU,SITDBSParser> states = new HashMap<TagIU,SITDBSParser>();
	
	/** keep track of all analyses (in order to be able to find the right IU for a given CA. */
	private List<CandidateAnalysisIU> analyses = new ArrayList<CandidateAnalysisIU>();
	
	public static String startSymbol;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		// load grammar
		grammarFile = ps.getString(PROP_GRAMMAR);
		Grammar g = new Grammar();
		try {
			g.loadXML(new URL(grammarFile));
		startSymbol = g.getStart().getSymbol();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		// initialize first parser
		baseBeamFactor = ps.getDouble(PROP_BASE_BEAM_FACTOR);
		SITDBSParser p = new SITDBSParser(g, baseBeamFactor);
		p.setLogger(this.logger);
		this.states.put(TagIU.FIRST_TAG_IU, p);
	}
	
	// finds all payload-string-equal IUs for a given CandidateAnalysis
	private List<CandidateAnalysisIU> findIU(CandidateAnalysis ca) {
		List<CandidateAnalysisIU> l = new ArrayList<CandidateAnalysisIU>(1);
		Iterator<CandidateAnalysisIU> i = analyses.iterator();
		while(i.hasNext()) {
			CandidateAnalysisIU iu = i.next();
			//TODO this is a hack: we compare the fullstring-representation of the CAs. a better alternative would be to
			// properly implement equal-like functions for CA and all subobjects.
			if(ca.toFullString().equals(iu.getCandidateAnalysis().toFullString())) {
				l.add(iu);
			}
		}
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
						if (ca instanceof CandidateAnalysisIU) {
							newEdits.add(new EditMessage<CandidateAnalysisIU>(EditType.REVOKE, (CandidateAnalysisIU) ca));
							analyses.remove(ca);
						}
					}
					break;
				case ADD:
					// TODO: ein besseres erstes element setzen
					TagIU previousTag = (TagIU) tag.getSameLevelLink();
					SITDBSParser newState = new SITDBSParser(this.states.get(previousTag));
					if (newState != null){
						int remainingAnalyses = newState.feed(tag.toPayLoad());
						this.states.put(tag,newState);
						for (CandidateAnalysis ca : newState.getQueue()) {
							CandidateAnalysisIU sll = CandidateAnalysisIU.FIRST_CA_IU;
							CandidateAnalysis ante = ca.getAntecedent();
							if (ante != null) {
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
					}
					break;
				case COMMIT:
					for (IU sem : tag.grounds()) {
						if (sem instanceof CandidateAnalysisIU) {
							newEdits.add(new EditMessage<CandidateAnalysisIU>(EditType.COMMIT, (CandidateAnalysisIU) sem));
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		this.rightBuffer.setBuffer(newEdits);
	}

	public void degradeAnalysis(CandidateAnalysisIU caiu, double malus) {
		logger.debug("! Degrade ca="+caiu.getCandidateAnalysis().toString()+" by "+malus+".");

		double originalWeight = caiu.getCandidateAnalysis().getProbability();
		double targetWeight = originalWeight * malus;

		// find the parser state containing this candidate analysis and modify its weight
		TagIU tagiu = (TagIU) caiu.groundedIn().get(0);
		SITDBSParser parserStateToModify = this.states.get(tagiu);
		CandidateAnalysis degradedCA = parserStateToModify.degradeAnalysis(caiu.getCandidateAnalysis(), malus);
		
		if(degradedCA != null) {
			// the ca was found in the parser queue and degraded successfully.
			
			// now modify the CA referenced in the CAIU.
			if (caiu.getCandidateAnalysis().getProbability() == targetWeight) {
				//System.out.println(" The CAIUs weight is already degraded.");
			} else {
				// it seems, this is never the case.
				caiu.getCandidateAnalysis().degradeProbability(malus);
				//System.out.println(" The CAIUs weight has been degraded.");
				
			}
			
			// now modify the CA referenced in the list.
			// it likewise seems, this is not needed: the objects are all referenced
			//System.out.println(analyses);
			
		} else {
			logger.debug(" The CA could not be degraded.");
		}
	}
	
}
