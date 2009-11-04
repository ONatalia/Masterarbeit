package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.dialogmanagement.composer.AVM;

public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU() {}; 
	
	private ArrayList<AVM> avmList = new ArrayList<AVM>();
	private final AVM resolvingAVM;

	@SuppressWarnings("unchecked") // fuck you
	private SemIU() {
		this(FIRST_SEM_IU, Collections.EMPTY_LIST, null, Collections.EMPTY_LIST);
	}
	
	public SemIU(IU sll, List<? extends IU> groundedIn, AVM resolvingAVM, List<AVM> avmList) {
		super(sll, groundedIn);
		this.avmList = new ArrayList<AVM>(avmList);
		this.resolvingAVM = resolvingAVM; 
	}
	
	public ArrayList<AVM> getAvmList() {
		return avmList;
	}

	public AVM getResolvingAVM() {
		return resolvingAVM;
	}
	
	public String toString() {
		return super.toString() + ", \nresolving AVM: " + resolvingAVM + "\nAVM list: " + avmList.toString() +"\n"; 
	}
	
}
