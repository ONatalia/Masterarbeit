package org.cocolab.inpro.incremental.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.nlu.AVM;

public class SemIU extends IU {

	public static final SemIU FIRST_SEM_IU = new SemIU() {}; 
	
	private ArrayList<AVM> dialogActList = new ArrayList<AVM>();
	private ArrayList<AVM> tileList = new ArrayList<AVM>();
	private ArrayList<AVM> fieldList = new ArrayList<AVM>();
	private ArrayList<AVM> booleanList = new ArrayList<AVM>();

	@SuppressWarnings("unchecked")
	public SemIU() {
		this(FIRST_SEM_IU, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	public SemIU(IU sll, List<IU> groundedIn, List<AVM> avms) {
		super(sll, groundedIn);
		for (AVM avm : avms) {
			if (avm.getType().equals("tile")) {
				this.tileList.add(avm);
			} else if (avm.getType().equals("field")) {
				this.fieldList.add(avm);
			} else if (avm.getType().equals("dialog_act")) {
				this.dialogActList.add(avm);
			} else if (avm.getType().equals("boolean")) {
				this.booleanList.add(avm);
			}
		}
	}

	public ArrayList<AVM> getAvmList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.tileList);
		list.addAll(this.fieldList);
		list.addAll(this.dialogActList);
		list.addAll(this.booleanList);
		return list;
	}

	public ArrayList<AVM> getFieldList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.fieldList);
		return list;
	}

	public ArrayList<AVM> getTileList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.tileList);
		return list;
	}

	public ArrayList<AVM> getDialogActList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.dialogActList);
		return list;
	}

	public ArrayList<AVM> getBooleanList() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.addAll(this.booleanList);
		return list;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("< ");
		for (AVM avm : this.dialogActList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.tileList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.fieldList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.booleanList) {
			sb.append(avm.toShortString() + " ");
		}
		sb.append(" >");
		return super.toString() + " " + sb.toString();
	}

	public String toTEDviewXML() {
		double startTime = startTime();
		StringBuilder sb = new StringBuilder("<event time='");
		sb.append(Math.round(startTime * 1000.0));
		sb.append("' duration='");
		sb.append(Math.round((endTime() - startTime) * 1000.0));
		sb.append("'> ");
		for (AVM avm : this.dialogActList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.tileList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.fieldList) {
			sb.append(avm.toShortString() + " ");
		}
		for (AVM avm : this.booleanList) {
			sb.append(avm.toShortString() + " ");
		}
		sb.append(" </event>");
		return sb.toString();
	}
	
	/**
	 * Compares payload of two SemIUs.
	 * Note: I'm comparing string representations of lists, because .equals() of contents of lists seems to differ.
	 * @param siu the SemIU to compare against
	 * @return true if each SemIUs string representations of their payload (three array lists) are the same.
	 */
	public boolean samePayload(SemIU siu) {
		return (this.dialogActList.toString().equals(siu.dialogActList.toString()) &&
				this.fieldList.toString().equals(siu.fieldList.toString()) &&
				this.tileList.toString().equals(siu.tileList.toString())) &&
				this.booleanList.toString().equals(siu.booleanList.toString());
	}

}
