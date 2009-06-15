package org.cocolab.inpro.incremental.unit;

public class EditMessage<IUType extends IU> {

	EditType edit;
	IUType iu;
	
	public EditMessage(EditType edit, IUType iu) {
		this.edit = edit;
		this.iu = iu;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer(edit.toString());
		sb.append("(");
		sb.append(iu.toString());
		sb.append(")");
		return sb.toString();
	}
	
}
