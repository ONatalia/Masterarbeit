package org.cocolab.inpro.incremental.unit;

import com.sri.oaa2.icl.IclTerm;

public class EditMessage<IUType extends IU> {

	EditType type;
	private IUType iu;
	
	public EditMessage(EditType edit, IUType iu) {
		this.type = edit;
		this.iu = iu;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(type.toString());
		sb.append("(");
		sb.append(iu.toString());
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * equality for EditMessages is defined by the contained IUs being equal
	 * and the EditType being the same
	 */
	public boolean equals(EditMessage<? extends IU> edit) {
		return (this.type == edit.type) && (this.iu.equals(edit.iu));
	}

	public IUType getIU() {
		return iu;
	}
	
	public EditType getType() {
		return type;
	}
	
	public IclTerm toOAAGoal() {
		return toOAAGoal(null);
	}
	
	public IclTerm toOAAGoal(String prefix) {
		StringBuilder sb = new StringBuilder(prefix);
		sb.append(type.oaaGoal());
		sb.append("(");
		sb.append(iu.toOAAString());
		sb.append(")");
		System.err.println(sb.toString());
		return IclTerm.fromString(true, sb.toString());
	}
	
}
