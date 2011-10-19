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
	//private Map<CandidateAnalysis, CandidateAnalysisIU> mapToCapsule = new HashMap<CandidateAnalysis, CandidateAnalysisIU>();
	// die Map geht nicht weil equal nicht für die CAs nicht so gesetzt werden kann wie nötig. stattdessen:
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
		this.states.put(TagIU.FIRST_TAG_IU, p);
	}
	
	// finds all payload-string-equal IUs for a given CandidateAnalysis
	private List<CandidateAnalysisIU> findIU(CandidateAnalysis ca) {
		List<CandidateAnalysisIU> l = new ArrayList<CandidateAnalysisIU>(1);
		Iterator<CandidateAnalysisIU> i = analyses.iterator();
		while(i.hasNext()) {
			CandidateAnalysisIU iu = i.next();
			if(ca.toString().equals(iu.toPayLoad())) {
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
						newState.feed(tag.toPayLoad());
						this.states.put(tag,newState);
						for (CandidateAnalysis ca : newState.getQueue()) {
							CandidateAnalysisIU sll = CandidateAnalysisIU.FIRST_CA_IU;
							
							CandidateAnalysis ante = ca.getAntecedent();
							if (ante != null) {
								//CandidateAnalysisIU anteIU = mapToCapsule.get(ante);
								List<CandidateAnalysisIU> potentialAntecedents = findIU(ante);
								for (CandidateAnalysisIU potentialAnte : potentialAntecedents) {
									if (potentialAnte != null && potentialAnte.groundedIn().contains(previousTag)) {
										sll = potentialAnte;
										break;
									} 
								}
							}
							CandidateAnalysisIU newCAIU = new CandidateAnalysisIU(sll, tag, ca);
							//mapToCapsule.put(ca, newCAIU);
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

}
