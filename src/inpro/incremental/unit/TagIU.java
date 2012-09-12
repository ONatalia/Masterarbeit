package inpro.incremental.unit;

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

	/**
	 * TagIUs are same if their tags are the same
	 */
	@Override
	public boolean equals(Object iu) {
		//return iu instanceof TagIU && this.tag.equals(((TagIU) iu).tag);
		// TODO: The old version yields Nullpointers for uninitialized TagIUs that are compared by IU.ground(IU). 
		return iu instanceof TagIU && this.toPayloadString().equals(((TagIU) iu).toPayloadString());
	}
	
	public String toPayloadString() {
		return (this.tag != null) ? this.tag.toString() : "null";
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
