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

public abstract class IU {

	private static int IU_idCounter = 0;
	
	int id;

	public IU() {
		this.id = IU.getNewID();
	}
	
	private static synchronized int getNewID() {
		return IU_idCounter++;
	}
	
	public String toString() {
		return Integer.toString(id);
	}
	
	public abstract String toTEDviewXML();
	public abstract String toOAAString();
	
	public boolean equals(IU iu) {
		return (this.id == iu.id); 
	}
}
