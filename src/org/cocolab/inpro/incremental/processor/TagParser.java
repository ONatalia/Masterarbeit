package org.cocolab.inpro.incremental.processor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.CandidateAnalysisIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.TagIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.irmrsc.parser.CandidateAnalysis;
import org.cocolab.inpro.irmrsc.parser.SITDBSParser;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class TagParser extends IUModule {

	private Map<TagIU,SITDBSParser> states = new HashMap<TagIU,SITDBSParser>();

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		this.states.put(TagIU.FIRST_TAG_IU, new SITDBSParser(this.states.get(TagIU.FIRST_TAG_IU)));
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
						}
					}
					break;
				case ADD:
					// der allererste muss gesetzt werden und richtig
					TagIU previousTag = (TagIU) tag.getSameLevelLink();
					SITDBSParser newState = new SITDBSParser(this.states.get(previousTag));
					if (newState != null){
						newState.feed(tag.toPayLoad());
						this.states.put(tag,newState);
						for (CandidateAnalysis ca : newState.getQueue()) {
							CandidateAnalysisIU sll = CandidateAnalysisIU.FIRST_CA_IU;
							if (previousTag.grounds().size() > 0) {
								sll = (CandidateAnalysisIU) previousTag.grounds().get(0);
							}
							newEdits.add(new EditMessage<CandidateAnalysisIU>(EditType.ADD, new CandidateAnalysisIU(sll, tag, ca)));
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
