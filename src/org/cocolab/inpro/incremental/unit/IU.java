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

import org.cocolab.inpro.incremental.BaseDataKeeper;

public abstract class IU {
	
	public static final IU FIRST_IU = new IU(null) {}; 
	private static int IU_idCounter = 0;
	protected final int id;

	protected IU sameLevelLink;
	
	protected List<? extends IU> groundedIn;
	
	protected BaseDataKeeper bd;
	
	/**
	 * call this, if you want to provide a sameLevelLink and a groundedIn list
	 * and you want groundedIn to be deeply SLLed to the sameLevelLink's groundedIn-IUs  
	 */
	public IU(IU sll, List<? extends IU> groundedIn, boolean deepSLL, BaseDataKeeper bd) {
		this(bd);
		this.groundedIn = groundedIn;
		if (deepSLL && (sll != null)) {
			connectSLL(sll);
		} else {
			this.sameLevelLink = sll;
		}
		
	}
	
	public IU(IU sll, List<? extends IU> groundedIn, boolean deepSLL) {
		this (sll, groundedIn, deepSLL, (BaseDataKeeper) null);
	}
	
	/**
	 * call this, if you want to provide both a sameLevelLink and a groundedIn list
	 */
	public IU(IU sll, List<? extends IU> groundedIn, BaseDataKeeper bd) {
		this(sll, groundedIn, false, bd);
	}
	
	public IU(List<? extends IU> groundedIn, BaseDataKeeper bd) {
		this((IU) null, groundedIn, bd);
	}
	
	/**
	 * call this, if you want to provide a sameLevelLink 
	 */
	public IU(IU sll, BaseDataKeeper bd) {
		this.id = IU.getNewID();
		this.sameLevelLink = sll;
		this.bd = bd;
	}
	
	/**
	 * this constructor must be called in order to acquire an IU with a valid ID. 
	 */
	public IU(BaseDataKeeper bd) {
		this((IU) null, bd);
	}
	
	public IU() {
		this((BaseDataKeeper) null);
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
	
	/**
	 * this is used to notify an IU that it's status has changed
	 * for example, in the abstract model, an IU might want to notify
	 * the grounded-in IUs, that it is now commited, and hence the
	 * grounded-in IUs have to become commited, too 
	 * 
	 * by convention, the ADD EditType does not result in an update, 
	 * as adding an IU should coincide with the IU's construction
	 * 
	 * also, we don't notify on REVOKE (this is done by Java's Garbage
	 * Collector and can be accessed through the finalize() method), 
	 * leaving (for now) only the COMMIT EditType to actually result
	 * in an update. in future, more edits (such as GROUNDED_IN_UPDATED,
	 * ASSERTnn and so on) will result in calls to this method
	 * 
	 * @param edit
	 */
	public void update(EditType edit) {
		if (edit == EditType.COMMIT) {
			for (IU iu : groundedIn()) {
				iu.update(edit);
			}
		}
	}
	
	public String toString() {
		return Integer.toString(id);
	}
	
	public String deepToString() {
		StringBuilder sb = new StringBuilder("[IU of type ");
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
