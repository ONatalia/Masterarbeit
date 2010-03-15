/* 
 * Copyright 2009, Okko Buss and the Inpro project
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

	public String toTEDviewXML() {
		double startTime = startTime();
		return "<event time='"
				+ Math.round(startTime * 1000.0) 
				+ "' duration='"
				+ Math.round((endTime() - startTime) * 1000.0)
				+ "'> "
				+ tag
				+ " </event>";
	}

	public String toString() {
		return id + "," + tag;
	}

	public String toOAAString() {
		StringBuffer sb = new StringBuffer(Integer.toString(id));
		sb.append(",'");
		sb.append(tag);
		sb.append("'");
		return sb.toString();
	}
	
	public boolean equals(TagIU iu) {
		/**
		 * IUs are same if their tags are the same
		 */
		return this.tag == iu.tag;
	}
	
	public int getID() {
		return this.id;
	}

}
