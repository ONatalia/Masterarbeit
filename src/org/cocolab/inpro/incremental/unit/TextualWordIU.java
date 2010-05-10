package org.cocolab.inpro.incremental.unit;

public class TextualWordIU extends WordIU {

	public static final TextualWordIU FIRST_ATOMIC_WORD_IU = new TextualWordIU(""); 
	public int counter;
	
	/* a word that hides any and all grounded-in hierarchy */
	/* and instead bases timing on word count */
	
	public TextualWordIU(String word, TextualWordIU sll, int counter) {
		super(word, null, sll, null);
		this.counter = counter;
	}
	
	public TextualWordIU(String word, TextualWordIU sll) {
		this(word, sll, (sll != null) ? sll.counter + 1 : 1);
	}
	
	private TextualWordIU(String word) {
		this(word, null);
	}
	
	@Override
	public double endTime() {
		return counter;
	}
	
	@Override
	public double startTime() {
		return (sameLevelLink == null) ? Double.NaN : sameLevelLink.endTime();
	}
	
	/**
	 * AtomicWordIUs have prosody if they end in either + or -
	 */
	@Override
	public boolean hasProsody() {
		return super.getWord().matches(".*[\\-\\+]$");
	}
	
	public boolean wordEquals(WordIU iu) {
		return (word.equals(iu.word));
	}
	
	/**
	 * <strong>precondition</strong>: only call this if hasProsody()
	 */
	@Override
	public boolean pitchIsRising() {
		assert hasProsody();
		return super.getWord().matches(".*\\+$");
	}
	
	@Override
	public String getWord() {
		return super.getWord().replaceAll("[\\+\\-]$", "");
	}
	
	/** update the end time of this word */
	@Override
	public void updateTimings(WordIU otherWord) {
		assert otherWord instanceof TextualWordIU;
		this.counter = ((TextualWordIU) otherWord).counter;
	}
	
}
