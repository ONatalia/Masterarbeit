package org.cocolab.inpro.incremental.unit;

public class AtomicWordIU extends WordIU {

	public static final AtomicWordIU FIRST_ATOMIC_WORD_IU = new AtomicWordIU(""); 
	public int counter;
	
	/* a word that hides any and all grounded-in hierarchy */
	/* and instead bases timing on word count */
	
	public AtomicWordIU(String word, AtomicWordIU sll, int counter) {
		super(word, null, sll, null);
		this.counter = counter;
	}
	
	public AtomicWordIU(String word, AtomicWordIU sll) {
		this(word, sll, (sll != null) ? sll.counter + 1 : 1);
	}
	
	private AtomicWordIU(String word) {
		this(word, null);
	}
	
	public double endTime() {
		return counter;
	}
	
	public double startTime() {
		return (sameLevelLink == null) ? Double.NaN : sameLevelLink.endTime();
	}
	
}
