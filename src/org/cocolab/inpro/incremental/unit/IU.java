package org.cocolab.inpro.incremental.unit;

public class IU {

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

}
