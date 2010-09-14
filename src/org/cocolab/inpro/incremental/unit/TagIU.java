package org.cocolab.inpro.incremental.unit;

import java.util.List;

public class TagIU extends IU {

	final String tag;

	public TagIU(String tag, TagIU sll, List<IU> groundedIn) {
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

}
