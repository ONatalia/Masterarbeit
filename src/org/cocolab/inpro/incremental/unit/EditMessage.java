package org.cocolab.inpro.incremental.unit;

public class EditMessage<IUType extends IU> {

	EditType type;
	private IUType iu;
	
	public EditMessage(EditType edit, IUType iu) {
		this.type = edit;
		this.iu = iu;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer(type.toString());
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
	
}
