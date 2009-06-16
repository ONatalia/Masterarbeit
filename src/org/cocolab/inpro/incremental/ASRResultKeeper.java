package org.cocolab.inpro.incremental;

import java.util.List;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.WordIU;

public interface ASRResultKeeper {

	public List<EditMessage<WordIU>> getEdits();

	public List<WordIU> getIUs();
	
	public int getCurrentFrame();
	
	public double getCurrentTime();

}
