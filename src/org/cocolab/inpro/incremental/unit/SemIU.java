package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.dialogmanagement.composer.AVM;

public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU() {}; 
	
	private ArrayList<AVM> dialogActList = new ArrayList<AVM>();
	private ArrayList<AVM> tileList = new ArrayList<AVM>();
	private ArrayList<AVM> fieldList = new ArrayList<AVM>();

	@SuppressWarnings("unchecked") // fuck you
	private SemIU() {
		this(FIRST_SEM_IU, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}
	
	public SemIU(IU sll, List<? extends IU> groundedIn, List<AVM> avms) {
		super(sll, groundedIn);
		for (AVM avm : avms) {
			if (avm.getType().equals("tile")) {
				this.tileList.add(avm);
			} else if (avm.getType().equals("field")) {
				this.fieldList.add(avm);
			} else if (avm.getType().equals("dialog_act")) {
				this.dialogActList.add(avm);
			}
		}
	}

	public ArrayList<AVM> getAvmList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.tileList);
		list.addAll(this.fieldList);
		list.addAll(this.dialogActList);
		return list;
	}

	public String toString() {
		return super.toString() + ", \n tiles: " + this.tileList.toString() + "\n fields: " + this.fieldList.toString() + "\n dialog acts: " + this.dialogActList.toString() + "\n"; 
	}

	public String toTEDviewXML() {
		double startTime = startTime();
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * 1000.0));
		sb.append("' duration='");
		sb.append(Math.round((endTime() - startTime) * 1000.0));
		sb.append("'> ");
		for (AVM avm : dialogActList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : tileList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : fieldList) {
			sb.append(avm.toShortString() + " ");
		}
		sb.append(" </event>");
		return sb.toString();
	}

}
