package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

public class TagIU extends IU {

	final String tag;

	public static final TagIU FIRST_TAG_IU = new TagIU("$begin"); 
	
	@SuppressWarnings("unchecked")
	public TagIU() {
		this(FIRST_TAG_IU, Collections.EMPTY_LIST, null);
	}
	
	public TagIU(TagIU sll, List<WordIU> groundedIn, String tag) {
		super(sll, groundedIn, true);
		this.tag = tag;
	}

	public TagIU(String tag) {
		this.tag = tag;
	}

	public boolean equals(TagIU iu) {
		/**
		 * IUs are same if their tags are the same
		 */
		return this.tag == iu.tag;
	}
	
	@Override
	public String toPayLoad() {
		return tag;
	}
	
	@Override
	public IU getSameLevelLink() {
		return super.getSameLevelLink() == null ? FIRST_TAG_IU : super.getSameLevelLink();
	}

}
