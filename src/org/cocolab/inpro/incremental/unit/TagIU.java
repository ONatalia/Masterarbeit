package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagIU extends IU {

	final String tag;

	public static final TagIU FIRST_TAG_IU = new TagIU("$begin"); 
	
	@SuppressWarnings("unchecked")
	public TagIU() {
		this(FIRST_TAG_IU, Collections.EMPTY_LIST, null);
	}
	
	public TagIU(TagIU sll, List<WordIU> groundedIn, String tag) {
		super(sll, groundedIn, true);
		this.tag = tag;
	}

	public TagIU(String tag) {
		this.tag = tag;
	}

	public boolean equals(TagIU iu) {
		/**
		 * IUs are same if their tags are the same
		 */
		return this.tag == iu.tag;
	}
	
	
	public void setSameLevelLink(IU link) {
		if (previousSameLevelLink != null) {
			throw new RuntimeException("SLL may not be changed");
		} else {
			previousSameLevelLink = link;
			if (link != null) {
				link.addNextSameLevelLink(this);
			}
		}
	}
	
	public void addNextSameLevelLink(IU iu) {
		if (nextSameLevelLinks == null)
			nextSameLevelLinks = new ArrayList<IU>();
		nextSameLevelLinks.add(iu);
	}	
	
	 public void connectSLL(IU link) {
	    	if (previousSameLevelLink == null) {
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
		 * compares IUs based on their payload (i.e., ignoring differing IDs)
		 * equals() implies payloadEquals(), but not the other way around
		 */
		public boolean payloadEquals(IU iu) {
			return (this.toPayLoad().equals(iu.toPayLoad()));
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
				grounds.add((IU) iu);
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

		@Override
		public String toPayLoad() {
			// TODO Auto-generated method stub
			return tag;
		}
		
	
}
