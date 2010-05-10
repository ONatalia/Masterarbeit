package org.cocolab.inpro.incremental.deltifier;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.WordIU;

/** allows to query the the current state of ASR input */
public interface ASRResultKeeper {

	/** 
	 * return a list of currently valid edits between 
	 * the last update of internal state and the one preceding that state. 
	 */
	public List<EditMessage<WordIU>> getWordEdits();

	/**
	 * return a list of currently valid WordIUs
	 */
	public List<WordIU> getWordIUs();
	
	/**
	 * return the frame of the {@link edu.cmu.sphinx.result.Result}
	 * that the current state is based on 
	 */
	public int getCurrentFrame();
	
	public double getCurrentTime();

}
