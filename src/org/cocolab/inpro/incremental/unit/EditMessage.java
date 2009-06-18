/* 
 * Copyright 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
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
	
	public IclTerm toOAAGoal() {
		return toOAAGoal(null);
	}
	
	public IclTerm toOAAGoal(String prefix) {
		StringBuffer sb = new StringBuffer(prefix);
		sb.append(type.oaaGoal());
		sb.append("(");
		sb.append(iu.toOAAString());
		sb.append(")");
		System.err.println(sb.toString());
		return IclTerm.fromString(true, sb.toString());
	}
	
}
