package inpro.incremental.processor;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.TagIU;
import inpro.incremental.unit.WordIU;
import inpro.nlu.AVPair;
import inpro.nlu.AVPairMappingUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * An incremental processor that produces {@link TagIU}s from {@link WordIU}s.<br />
 * Determines a word's tags from a list of associated {@link AVPair}s and adds one TagIU for each.<br />
 * @author okko, Andreas Peldszus
 */
public class Tagger extends IUModule {

	/** the tag that is given to words that have no associated tag defined */
	public final static String NO_TAG_TAG = "unknown";
	
	@S4String(defaultValue = "res/PentoAVMapping")
	public final static String PROP_WORD_SEMANTICS = "lookupTags";

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String mappingPath = ps.getString(PROP_WORD_SEMANTICS);
		try {
			WordIU.setAVPairs(AVPairMappingUtil.readAVPairs(new URL(mappingPath)));
		} catch (IOException e) {
			e.printStackTrace();
			logger.fatal("Could not set WordIU's AVPairs from file " + mappingPath);
		}
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		List<EditMessage<TagIU>> newEdits = new ArrayList<EditMessage<TagIU>>();
		for (EditMessage<? extends IU> edit : edits) {
			WordIU newWord = (WordIU) edit.getIU();
			switch (edit.getType()) {
				case REVOKE:
					for (IU tag : newWord.grounds()) {
						if (tag instanceof TagIU) {
							newEdits.add(new EditMessage<TagIU>(EditType.REVOKE, (TagIU) tag));
						}
					}
					break;
					
				case ADD:
					if (newWord.getAVPairs() != null) {
						// TODO: bundle tag value + other tag-relevant AVPs.
						for (AVPair tagPair : newWord.getAVPairs()) {
							// a word can receive more than one tag
							if (tagPair.getAttribute().equals("tag")) {
								// the word did receive some tag, all good.
								List<? extends IU> antecedents = newWord.getSameLevelLink().grounds();
								if (antecedents.size() < 1) {
									// this seems to be the first word: link its tag to initial tag iu
									newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU(TagIU.FIRST_TAG_IU, Collections.singletonList(newWord), (String) tagPair.getValue())));
								} else {
									// this is a follow up: add a new TagIU for each antecedent
									for (IU a : antecedents) {
										newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU((TagIU) a, Collections.singletonList(newWord), (String) tagPair.getValue())));
									}
								}
							}
						}
					} else {
						// the word did not receive a tag; be robust
						List<? extends IU> antecedents = newWord.getSameLevelLink().grounds();
						if (antecedents.size() < 1) {
							// this seems to be the first word: link its tag to initial tag iu
							newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU(TagIU.FIRST_TAG_IU, Collections.singletonList(newWord), NO_TAG_TAG)));
						} else {
							// this is a follow up: add a new TagIU for each antecedent
							for (IU a : antecedents) {
								newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU((TagIU) a, Collections.singletonList(newWord), NO_TAG_TAG)));
							}
						}
//						/* unknown words can equally occur after ambiguous words, which was not covered by the old code. */ 
//						TagIU sll = TagIU.FIRST_TAG_IU;
//						if (newWord.getSameLevelLink().grounds().size() > 0) {
//							sll = (TagIU) newWord.getSameLevelLink().grounds().get(0);
//						}
//						newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU(sll, Collections.singletonList(newWord), NO_TAG_TAG)));
					}
					break;
					
				case COMMIT:
					for (IU sem : newWord.grounds()) {
						if (sem instanceof TagIU) {
							newEdits.add(new EditMessage<TagIU>(EditType.COMMIT, (TagIU) sem));
						}
					}
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
		this.rightBuffer.setBuffer(newEdits);
	}

}
