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

import java.util.List;

public abstract class IU {
	
	public static final IU FIRST_IU = new IU() {}; 

	private static int IU_idCounter = 0;
	
	final int id;

	IU sameLevelLink;
	
	List<? extends IU> groundedIn;
	
	/**
	 * call this, if you want to provide a sameLevelLink and a groundedIn list
	 * and you want groundedIn to be deeply SLLed to the sameLevelLink's groundedIn-IUs  
	 */
	public IU(IU sll, List<? extends IU> groundedIn, boolean deepSLL) {
		this();
		this.groundedIn = groundedIn;
		if (deepSLL && (sll != null)) {
			connectSLL(sll);
		} else {
			this.sameLevelLink = sll;
		}
		
	}
	
	/**
	 * call this, if you want to provide both a sameLevelLink and a groundedIn list
	 */
	public IU(IU sll, List<? extends IU> groundedIn) {
		this(sll, groundedIn, false);
	}
	
	public IU(List<? extends IU> groundedIn) {
		this((IU) null, groundedIn);
	}
	
	/**
	 * call this, if you want to provide a sameLevelLink 
	 */
	public IU(IU sll) {
		this.id = IU.getNewID();
		this.sameLevelLink = sll;
	}
	
	/**
	 * this constructor must be called in order to acquire an IU with a valid ID. 
	 */
	public IU() {
		this((IU) null);
	}
	
	/**
	 * called to acquire a new ID
	 * @return a process-unique ID.
	 */
	private static synchronized int getNewID() {
		return IU_idCounter++;
	}
	
	public void setSameLevelLink(IU link) {
		if (sameLevelLink != null) {
			throw new RuntimeException("SLL may not be changed");
		} else {
			sameLevelLink = link;
		}
	}
	
	public void connectSLL(IU link) {
		setSameLevelLink(link);
		if (groundedIn != null) {
			IU firstGrounding = groundedIn.get(0);
			IU prevLast;
			if (link.groundedIn != null) {
				prevLast = link.groundedIn.get(link.groundedIn.size());
				if (prevLast.getClass() != firstGrounding.getClass()) {
					throw new RuntimeException("I can only connect IUs of identical types!");
				}
			} else {
				prevLast = FIRST_IU;
			}
			firstGrounding.connectSLL(prevLast);
		}
	}
	

	
	/**
	 * return the start of the timespan this IU covers 
	 * @return NaN if time is unavailable, a time (in seconds) otherwise
	 */
	public double startTime() {
		if ((groundedIn != null) && (groundedIn.size() > 0)) {
			return groundedIn.get(0).startTime();
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * return the end of the timespan this IU covers 
	 * @return NaN if time is unavailable, a time (in seconds) otherwise
	 */
	public double endTime() {
		if ((groundedIn != null) && (groundedIn.size() > 0)) {
			return groundedIn.get(groundedIn.size() - 1).endTime();
		} else {
			return Double.NaN;
		}
	}
		
	public List<? extends IU> groundedIn() {
		return groundedIn;
	}

	public boolean equals(IU iu) {
		return (this.id == iu.id); 
	}
	
	public String toString() {
		return Integer.toString(id);
	}
	
	public String deepToString() {
		StringBuffer sb = new StringBuffer("[IU of type ");
		sb.append(this.getClass());
		sb.append(" with content ");
		sb.append(this.toString());
		sb.append("\n  SLL: ");
		if (sameLevelLink != null) {
			sb.append(sameLevelLink.toString());
		} else {
			sb.append("none");
		}
		sb.append("\n  grounded in:\n  [");
		if (groundedIn != null) {
			for (IU iu : groundedIn) {
				sb.append(iu.deepToString());
				sb.append("  ");
			}
		} else {
			sb.append("none");
		}
		sb.append("]\n]\n");
		return sb.toString();
 	}
	
	public String toOAAString() {
		return null;
	}

	public String toTEDviewXML() {
		return null;
	}
	
}
