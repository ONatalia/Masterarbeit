package org.cocolab.inpro.incremental.processor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.TagIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;
import org.cocolab.inpro.nlu.AVPairMappingUtil;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * An incremental processor that produces {@link TagIU}s from {@link WordIU}s.<br />
 * Determines a word's tags from a list of associated {@link AVPair}s and adds one TagIU for each.<br />
 * @author okko, andreas
 *
 */
public class Tagger extends IUModule {

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
							if (tagPair.getAttribute().equals("tag")) {
								TagIU sll = TagIU.FIRST_TAG_IU;
								if (newWord.getSameLevelLink().grounds().size() > 0) {
									sll = (TagIU) newWord.getSameLevelLink().grounds().get(0);
								}
								newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU(sll, Collections.singletonList(newWord), (String) tagPair.getValue())));
							}
						}
					} else {
						// the word did not recieve a tag; be robust
						TagIU sll = TagIU.FIRST_TAG_IU;
						if (newWord.getSameLevelLink().grounds().size() > 0) {
							sll = (TagIU) newWord.getSameLevelLink().grounds().get(0);
						}
						newEdits.add(new EditMessage<TagIU>(EditType.ADD, new TagIU(sll, Collections.singletonList(newWord), NO_TAG_TAG)));
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
