package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.cocolab.inpro.incremental.BaseDataKeeper;
import org.cocolab.inpro.incremental.util.ResultUtil;

public abstract class IU {
	
	public static final long startupTime = System.currentTimeMillis();
	
	public static final IU FIRST_IU = new IU() {
		@Override
		public String toPayLoad() {
			return "The very first IU";
		}
	}; 
	private static int IU_idCounter = 0;
	protected final int id;

	protected IU sameLevelLink;
	
	protected List<IU> groundedIn;
	protected List<IU> grounds; 
	
	protected static BaseDataKeeper bd = null;
	
	protected long creationTime;
	
	/**
	 * call this, if you want to provide a sameLevelLink and a groundedIn list
	 * and you want groundedIn to be deeply SLLed to the sameLevelLink's groundedIn-IUs  
	 */
	public IU(IU sll, List<IU> groundedIn, boolean deepSLL) {
		this.creationTime = System.currentTimeMillis();
		this.id = IU.getNewID();
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
	public IU(IU sll, List<IU> groundedIn) {
		this(sll, groundedIn, false);
	}
	
	public IU(List<IU> groundedIn) {
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
	}
	
	/**
	 * called to acquire a new ID
	 * @return a process-unique ID.
	 */
	private static synchronized int getNewID() {
		return IU_idCounter++;
	}
	
	public static void setBaseData(BaseDataKeeper bd) {
		assert (IU.bd == null) : "You're trying to re-set basedata. This may be a bug.";
		IU.bd = bd;
	}

	public void setSameLevelLink(IU link) {
		if (sameLevelLink != null) {
			throw new RuntimeException("SLL may not be changed");
		} else {
			sameLevelLink = link;
		}
	}
	
	public IU getSameLevelLink() {
		return sameLevelLink;
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
	
	/**
	 * @return the duration of the IU, that is, endTime() - startTime()
	 */
	public double duration() {
		return endTime() - startTime();
	}
		
	@SuppressWarnings("unchecked")
	public List<? extends IU> groundedIn() {
		return groundedIn != null ? groundedIn : Collections.EMPTY_LIST;
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
		switch (edit) {
			case COMMIT: // commit what you're grounding
				for (IU iu : groundedIn()) {
					iu.update(edit);
				}
				break;
			case REVOKE: // revoke what is grounded in you
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
		if (!grounds.contains(iu)) 
			grounds.add((IU) iu);
		if (!iu.groundedIn.contains(this))
			iu.groundedIn.add(this);
	}
	
	public abstract String toPayLoad();
	
	public String toLabelLine() {
		return String.format(Locale.US,	"%.2f\t%.2f\t%s", startTime(), endTime(), toPayLoad());
	}
	
	public String toString() {
		return id + "," + toLabelLine(); // + "\n";
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
	
	public String toTEDviewXML() {
		double startTime = startTime();
		if (Double.isNaN(startTime))
			startTime = 0.0;
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * ResultUtil.SECOND_TO_MILLISECOND_FACTOR));
		double duration = duration();
		if (Double.isNaN(duration))
			duration = 0.0;
		sb.append("' duration='");
		sb.append(duration * ResultUtil.SECOND_TO_MILLISECOND_FACTOR);
		sb.append("'>");
		sb.append("<iu iu_id=\"" + this.id + "\"");
		if (this.getSameLevelLink() == null) {
			sb.append(" sll=\"top\"");
		} else {
			sb.append(" sll=\"" + this.getSameLevelLink().id + "\"");
		}
		if (this.groundedIn != null && !groundedIn.isEmpty()) {
			Iterator<IU> grIt = groundedIn.iterator();
			sb.append(" gil=\"");
			while (grIt.hasNext()) {
				sb.append(grIt.next().id);
				if (grIt.hasNext())
					sb.append(",");
			}
			sb.append("\"");
		}
		sb.append(">");
		sb.append(toPayLoad().replace("<", "&lt;").replace(">", "&gt;"));
		sb.append("</iu>");
		sb.append("</event>");
//		System.err.println(sb.toString());
		return sb.toString();
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
}
