package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.cocolab.inpro.incremental.BaseDataKeeper;
import org.cocolab.inpro.incremental.util.ResultUtil;

public abstract class IU implements Comparable<IU> {
	
	public static long startupTime;
	static { startupTime = System.currentTimeMillis(); }
	
	public static final IU FIRST_IU = new IU() {
		@Override
		public String toPayLoad() {
			return "The very first IU";
		}
	}; 
	private static int IU_idCounter = 0;
	private final int id;

	protected IU previousSameLevelLink;
	protected List<IU> nextSameLevelLinks;
	
	protected List<IU> groundedIn;
	protected List<IU> grounds; 
	
	protected static BaseDataKeeper bd = null;
	
	protected long creationTime;
	
	private boolean committed = false;
	private boolean revoked = false;
	
	/** types of temporal progress states the IU may be in */ 
	public enum Progress { UPCOMING, ONGOING, COMPLETED}
	
	/**
	 * call this, if you want to provide a sameLevelLink and a groundedIn list
	 * and you want groundedIn to be deeply SLLed to the sameLevelLink's groundedIn-IUs  
	 */
	@SuppressWarnings("unchecked")
	public IU(IU sll, List<? extends IU> groundedIn, boolean deepSLL) {
		this();
		this.groundedIn = (List<IU>) groundedIn;
		if (groundedIn != null) {
			for (IU grin : this.groundedIn) {
				grin.ground(this);
			}			
		}
		if (deepSLL && (sll != null)) {
			connectSLL(sll);
		} else {
			this.previousSameLevelLink = sll;
			if (sll != null) {
				sll.addNextSameLevelLink(this);
			}
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
		this(sll, null);
	}
		
	/**
	 * this constructor must be called in order to acquire an IU with a valid ID. 
	 */
	public IU() {
		this.id = IU.getNewID();
		this.creationTime = System.currentTimeMillis() - startupTime;
	}
	
	/**
	 * called to acquire a new ID
	 * @return a process-unique ID.
	 */
	private static synchronized int getNewID() {
		return IU_idCounter++;
	}
	
	/**
	 * get the ID assigned to this IU
	 * @return the IU's ID
	 */
	public final int getID() {
		return id;
	}

	public static void setBaseData(BaseDataKeeper bd) {
		assert (IU.bd == null) : "You're trying to re-set basedata. This may be a bug.";
		IU.bd = bd;
	}

	public void setSameLevelLink(IU link) {
		if (previousSameLevelLink != null) {
		//	throw new RuntimeException("SLL may not be changed");
		}
		previousSameLevelLink = link;
		if (link != null) {
			link.addNextSameLevelLink(this);
		}
	}
	
	public void addNextSameLevelLink(IU iu) {
		if (nextSameLevelLinks == null)
			nextSameLevelLinks = new ArrayList<IU>();
		nextSameLevelLinks.add(iu);
	}

	public IU getSameLevelLink() {
		return previousSameLevelLink;
	}

	/** return the (possibly empty) list of next SLLs */
	public List<IU> getNextSameLevelLinks() {
		return nextSameLevelLinks != null ? nextSameLevelLinks : Collections.<IU>emptyList();
	}
	
	public IU getNextSameLevelLink() {
		return (nextSameLevelLinks != null && nextSameLevelLinks.size() > 0) ? nextSameLevelLinks.get(0) : null;
	}
	
	public void setAsTopNextSameLevelLink(final String bestFollowerPayload) {
		reorderNextSameLevelLink(new Comparator<IU>() {
			@Override
			public int compare(IU o1, IU o2) {
				return Math.abs(o1.toPayLoad().compareTo(bestFollowerPayload)) - Math.abs(o2.toPayLoad().compareTo(bestFollowerPayload));
			}
		});
	}
	
	/** get the IU among the nextSLLs with the given payload */
	public IU getAmongNextSameLevelLinks(final String bestFollowerPayload) {
		for (IU iu : nextSameLevelLinks) {
			if (bestFollowerPayload.equals(iu.toPayLoad()))
				return iu;
		}
		return null;
	}
	
	public void reorderNextSameLevelLink(Comparator<IU> order) {
		Collections.sort(nextSameLevelLinks, order);
		if (groundedIn != null && groundedIn.size() > 0) {
			IU lastGrounding = groundedIn.get(groundedIn.size() - 1);
			lastGrounding.newGroundingNextSameLevelLinksOrder(nextSameLevelLinks);
		}
	}
	
	/** this is needed to (recursively) correct nextSameLevelLinks in groundedIn IUs */
	private void newGroundingNextSameLevelLinksOrder(List<IU> aboveNextSameLevelLinks) {
		nextSameLevelLinks.clear();
		for (IU iu : aboveNextSameLevelLinks) {
			nextSameLevelLinks.add(iu.groundedIn.get(0));
		}
		if (groundedIn != null && groundedIn.size() > 0) {
			IU lastGrounding = groundedIn.get(groundedIn.size() - 1);
			lastGrounding.newGroundingNextSameLevelLinksOrder(nextSameLevelLinks);
		}
	}

    public void connectSLL(IU link) {
    	//if (previousSameLevelLink == null) {
    		setSameLevelLink(link);
    		if (link != null && groundedIn != null) {
    			IU firstGrounding = groundedIn.get(0);
    			IU prevLast;
    			if (link.groundedIn != null && link.groundedIn != Collections.EMPTY_LIST) {
    				prevLast = link.groundedIn.get(link.groundedIn.size() - 1);
    				if (prevLast.getClass() != firstGrounding.getClass()) {
    					throw new RuntimeException("I can only connect IUs of identical types!");
    				}
    			} else {
    				prevLast = FIRST_IU;
    			}
    			firstGrounding.connectSLL(prevLast);
    		}
    	//}
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
			double time = 0;
			for (IU iu : this.groundedIn()) {
				if (!Double.isNaN(iu.endTime())) {
					time = Math.max(iu.endTime(), time);
				}
			}
			return time;
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * @return the duration of the IU in seconds, that is, endTime() - startTime()
	 */
	public double duration() {
		return endTime() - startTime();
	}
		
	@SuppressWarnings("unchecked")
	public List<? extends IU> groundedIn() {
		return groundedIn != null ? groundedIn : Collections.EMPTY_LIST;
	}
	
	@SuppressWarnings("unchecked")
	public List<? extends IU> grounds() {
		return grounds != null ? grounds : Collections.EMPTY_LIST;
	}

	/**
	 * two IUs are equal if their IDs are the same
	 */
	public boolean equals(IU iu) {
		return (this.id == iu.id); 
	}
	
	/**
	 * compares IUs based on their payload (i.e., ignoring differing IDs)
	 * equals() implies payloadEquals(), but not the other way around
	 */
	public boolean payloadEquals(IU iu) {
		return (this.toPayLoad().equals(iu.toPayLoad()));
	}
	
	/**
	 * @return true if this IU has been committed
	 */
	public boolean isCommitted() {
		return this.committed;
	}
	
	/**
	 * COMMITs this IU.
	 */
	public void commit() {
		this.committed = true;
	}
	
	/**
	 * @return true if this IU has been revoked
	 */
	public boolean isRevoked() {
		return this.revoked;
	}

	/**
	 * COMMITs this IU.
	 */
	public void revoke() {
		this.revoked = true;
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
		switch (edit) {
			case COMMIT: // commit what you're grounding
				this.commit();
				for (IU iu : groundedIn()) {
					iu.update(edit);
				}
				break;
			case REVOKE: // revoke what is grounded in you
				this.revoke();
				if (grounds != null)
					for (IU iu : grounds) {
						iu.update(edit);
					}
				break;
			case ADD: // nothing to do, whoever adds us should set us up correctly
		}
	}
	
	public void ground(IU iu) {
		if (grounds == null) {
			// we typically ground just 1 IU, so there's no need for an initial capacity of 10
			grounds = new ArrayList<IU>(1);
		}
		if (!grounds.contains(iu)) {
			grounds.add(iu);
		}
		iu.groundIn(this);
	}

	public void removeGrin(List<IU> ius) {
		for (IU iu : ius) {
			this.removeGrin(iu);
		}
	}

	public void removeGrin(IU iu) {
		this.groundedIn.remove(iu);
		if (iu.grounds != null)
			iu.grounds.remove(this);
	}

	public void groundIn(List<IU> ius) {
		for (IU iu : ius) {
			this.groundIn(iu);
		}
	}

	public void groundIn(IU iu) {
		if (this.groundedIn == null) {
			this.groundedIn = new ArrayList<IU>();
		} else {
			this.groundedIn = new ArrayList<IU>(this.groundedIn);
		}
		if (!this.groundedIn.contains(iu)) {
			this.groundedIn.add(iu);
			iu.ground(this);
		}
	}
	
	public boolean isUpcoming()  { return Progress.UPCOMING.equals(getProgress()); }
	public boolean isOngoing()   { return Progress.ONGOING.equals(getProgress()); }
	public boolean isCompleted() { return Progress.COMPLETED.equals(getProgress()); }
	
	/** 
	 * by default, an IU 
	 *  - is complete if the last grounding unit is complete,
	 *  - is upcoming if the first grounding unit is upcoming,
	 *  - is ongoing if inbetween those two states
	 *  - is null if there are no grounding units. 
	 */
	public Progress getProgress() {
		if (groundedIn != null && groundedIn.size() > 0) {
			if (groundedIn.get(0).isUpcoming())
				return Progress.UPCOMING;
			if (groundedIn.get(groundedIn.size() - 1).isCompleted())
				return Progress.COMPLETED;
			return Progress.ONGOING;
		} else {
			return null;
		}
	}
	
	/** 
	 * return the IU that is ongoing among the groundedIn links 
	 * @return the first ongoing grounding IU (or null if none is ongoing)
	 */
	public IU getOngoingGroundedIU() {
		assert isOngoing();
		for (IU iu : groundedIn) {
			if (iu.isOngoing())
				return iu;
		}
		return null;
	}
	
	public abstract String toPayLoad();
	
	public String toLabelLine() {
		return String.format(Locale.US,	"%.2f\t%.2f\t%s", startTime(), endTime(), toPayLoad());
	}
	
	@Override
	public String toString() {
		return getID() + "," + toLabelLine(); // + "\n";
	}
	
	public String deepToString() {
		StringBuilder sb = new StringBuilder("[IU of type ");
		sb.append(this.getClass());
		sb.append(" with content ");
		sb.append(this.toString());
		sb.append("\n  Committed: " + this.isCommitted());
		sb.append("\n  pSLL: ");
		if (previousSameLevelLink != null) {
			sb.append(previousSameLevelLink.id);
		} else {
			sb.append("none");
		}
		if (getNextSameLevelLink() != null) {
			sb.append("\n  nSLL: [");
			for (IU nsll : getNextSameLevelLinks()) {
				sb.append(nsll.id);
				sb.append(", ");
			}
			sb.append("]");
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
	
	public String toTEDviewXML() {
		double startTime = startTime();
		if (Double.isNaN(startTime))
			startTime = 0.0;
		double duration = duration();
		if (Double.isNaN(duration))
			duration = 0.0;
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("' duration='");
		sb.append(Math.round(duration * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("'><iu id='");
		int id = this.getID();
		sb.append(id);
		sb.append("' created='");
		sb.append(getCreationTime());
		sb.append('\'');
		if (this.getSameLevelLink() != null) {
			sb.append(" sll='" + this.getSameLevelLink().getID() + "'");
		}
		if (groundedIn != null && !groundedIn.isEmpty()) {
	        Iterator<IU> grIt = groundedIn.iterator();
			sb.append(" grin='");
	        while (grIt.hasNext()) {
	                sb.append(grIt.next().getID());
	                if (grIt.hasNext())
	                        sb.append(",");
	        }
			sb.append("'");
		}
		sb.append(">");
		sb.append(toPayLoad().replace("<", "&lt;").replace(">", "&gt;"));
		sb.append("</iu></event>");
		return sb.toString();
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	public long getAge() {
		return System.currentTimeMillis() - startupTime - creationTime;
	}
	
	/**
	 * the natural ordering of IUs is based on the IU's ids:
	 * IUs with lower ids come first 
	 */
	@Override
	public int compareTo(IU other) {
		return this.id - other.id;
	}
	
}
