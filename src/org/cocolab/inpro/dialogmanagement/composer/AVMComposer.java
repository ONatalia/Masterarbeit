package org.cocolab.inpro.dialogmanagement.composer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * AVM Composer - Reads AVPairs.  Attempts composition of
 * new AVMs and subsequent unification of existing ones.
 * @author okko
 */
public class AVMComposer {


	private ArrayList<AVM> avmList = new ArrayList<AVM>();
	static private ArrayList<AVM> worldList = new ArrayList<AVM>();
	
	static {
		AVM avm1 = new AVM("tile");
		avm1.setAttribute(new AVPair("name", "x"));
		avm1.setAttribute(new AVPair("label", "x1"));
		avm1.setAttribute(new AVPair("color", "green"));
		avm1.setAttribute(new AVPair("ord", "1"));
		avm1.setAttribute(new AVPair("orient", "top"));
		avm1.setAttribute(new AVPair("row_col", "row"));
		avm1.setAttribute(new AVPair("ord", "2"));
		avm1.setAttribute(new AVPair("orient", "bottom"));
		avm1.setAttribute(new AVPair("row_col", "row"));
		avm1.setAttribute(new AVPair("ord", "1"));
		avm1.setAttribute(new AVPair("orient", "left"));
		avm1.setAttribute(new AVPair("row_col", "col"));
		avm1.setAttribute(new AVPair("ord", "1"));
		avm1.setAttribute(new AVPair("orient", "right"));
		avm1.setAttribute(new AVPair("row_col", "col"));
		avm1.setAttribute(new AVPair("tb", "top"));
		avm1.setAttribute(new AVPair("lr", "left"));
		avm1.setAttribute(new AVPair("desc", "corner"));
		avm1.setAttribute(new AVPair("relation", "next_to"));
		avm1.setAttribute(new AVPair("relation", "above"));
		avm1.setAttribute(new AVPair("relation", "below"));

		AVM avm2 = new AVM("tile");
		avm2.setAttribute(new AVPair("name", "f"));
		avm2.setAttribute(new AVPair("label", "f1"));
		avm2.setAttribute(new AVPair("color", "green"));
		avm2.setAttribute(new AVPair("ord", "2"));
		avm2.setAttribute(new AVPair("orient", "top"));
		avm2.setAttribute(new AVPair("row_col", "row"));
		avm2.setAttribute(new AVPair("ord", "1"));
		avm2.setAttribute(new AVPair("orient", "bottom"));
		avm2.setAttribute(new AVPair("row_col", "row"));
		avm2.setAttribute(new AVPair("ord", "1"));
		avm2.setAttribute(new AVPair("orient", "left"));
		avm2.setAttribute(new AVPair("row_col", "col"));
		avm2.setAttribute(new AVPair("ord", "1"));
		avm2.setAttribute(new AVPair("orient", "right"));
		avm2.setAttribute(new AVPair("row_col", "col"));
		avm2.setAttribute(new AVPair("tb", "top"));
		avm2.setAttribute(new AVPair("lr", "left"));
		avm2.setAttribute(new AVPair("desc", "corner"));
		avm2.setAttribute(new AVPair("relation", "next_to"));
		avm2.setAttribute(new AVPair("relation", "above"));
		avm2.setAttribute(new AVPair("relation", "below"));

		AVM avm3 = new AVM("field");
		avm3.setAttribute(new AVPair("color", "green"));
		avm3.setAttribute(new AVPair("ord", "1"));
		avm3.setAttribute(new AVPair("orient", "top"));
		avm3.setAttribute(new AVPair("row_col", "row"));
		avm3.setAttribute(new AVPair("ord", "2"));
		avm3.setAttribute(new AVPair("orient", "bottom"));
		avm3.setAttribute(new AVPair("row_col", "row"));
		avm3.setAttribute(new AVPair("ord", "1"));
		avm3.setAttribute(new AVPair("orient", "left"));
		avm3.setAttribute(new AVPair("row_col", "col"));
		avm3.setAttribute(new AVPair("ord", "1"));
		avm3.setAttribute(new AVPair("orient", "right"));
		avm3.setAttribute(new AVPair("row_col", "col"));

		AVM avm4 = new AVM("field");
		avm4.setAttribute(new AVPair("color", "brown"));
		avm4.setAttribute(new AVPair("ord", "2"));
		avm4.setAttribute(new AVPair("orient", "top"));
		avm4.setAttribute(new AVPair("row_col", "row"));
		avm4.setAttribute(new AVPair("ord", "1"));
		avm4.setAttribute(new AVPair("orient", "bottom"));
		avm4.setAttribute(new AVPair("row_col", "row"));
		avm4.setAttribute(new AVPair("ord", "1"));
		avm4.setAttribute(new AVPair("orient", "left"));
		avm4.setAttribute(new AVPair("row_col", "col"));
		avm4.setAttribute(new AVPair("ord", "1"));
		avm4.setAttribute(new AVPair("orient", "right"));
		avm4.setAttribute(new AVPair("row_col", "col"));

		AVM avm5 = new AVM("dialog_act");
		avm5.setAttribute(new AVPair("act", "take"));

		AVM avm6 = new AVM("dialog_act");
		avm6.setAttribute(new AVPair("act", "turn"));

		AVM avm7 = new AVM("dialog_act");
		avm6.setAttribute(new AVPair("act", "place"));

		worldList.add(avm1);
		worldList.add(avm2);
		worldList.add(avm3);
		worldList.add(avm4);
		worldList.add(avm5);
		worldList.add(avm6);
		worldList.add(avm7);
	}
	
	/**
	 * Creates AVMComposer with a list of prototypes of
	 * different typed AVMs.
	 * New AVPairs will first be put into an UntypedAVM,
	 * for which unification will be attempted on each
	 * of these.  Non-null ones will be kept.
	 */
	public AVMComposer() {
		this.avmList = getAllAVMs();
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();
		
		System.out.println("World contains following objects:");
		for (AVM avm : worldList) {
			System.out.println(avm.toString());
		}


		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		
//		avps.add(new AVPair("act", "turn"));
		avps.add(new AVPair("color", "green"));
		avps.add(new AVPair("name", "f"));
//		avps.add(new AVPair("name", "cross"));
		avps.add(new AVPair("ord", "2"));
		avps.add(new AVPair("orient", "top"));
		avps.add(new AVPair("ord", "1"));
		avps.add(new AVPair("ord", "1"));
		avps.add(new AVPair("ord", "1"));
//		avps.add(new AVPair("ord", "3"));
//		avps.add(new AVPair("ord", "4"));
//		avps.add(new AVPair("orient", "bottom"));
		avps.add(new AVPair("relation", "next_to"));
		avps.add(new AVPair("relation", "above"));
//		avps.add(new AVPair("color", "gelb"));

		for (AVPair avp : avps) {
			System.out.println("Adding tag AVPair '" + avp.toString() + "'.");
			if (composer.avmList != null) {
				composer.compose(avp);
				composer.printAVMs();
			} 
			if (composer.avmList != null) {
				ArrayList<AVM> resolvedList = composer.resolve();
				if (resolvedList.size() > 0) {
					System.out.println("Found these that resolve...");
					for (AVM avm : resolvedList)
					System.out.println(avm.toString());
				} else {
					System.out.println("Nothing resolves...");
				}
			}
		}
		System.out.println("Done!");

//		interactiveTest();
	}
	
	static void interactiveTest() throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String line = stdin.readLine();
		AVMComposer composer = new AVMComposer();
		while (!line.equals("exit")) {
			if (line.equals("new"))  
				composer = new AVMComposer();
			else {
				composer.unifyNewAVPair(new AVPair(line));
			}
			composer.printAVMs();
			line = stdin.readLine();
		}
	}

	/**
	 * Method to call when a new AVPair becomes known.
	 * Creates an UntypedAVM.  Attempt unification with known
	 * prototypes.  For all unified AVMs re-attempts unification
	 * with prototypes (for higher-order AVMs).  Lastly attempts
	 * unification with existing AVMs.
	 * @param avp
	 */
	public void unifyNewAVPair(AVPair avp) {
		ArrayList<AVM> newList = new ArrayList<AVM>();
		boolean placed = false;
		for (AVM avm : this.avmList) {
			if (avm.setAttribute(avp)) {
				newList.add(avm);
				placed = true;
			}
		}
		if (placed) {
			avmList = newList;
		} else {
			avmList.clear();
		}
	}
	
	public ArrayList<AVM> compose(AVPair avp) {
		this.unifyNewAVPair(avp);
		return avmList;
	}

	public ArrayList<AVM> resolve() {
		ArrayList<AVM> resolvedList = new ArrayList<AVM>();
		if (avmList != null) {
			for (AVM avm1 : worldList) {
				for (AVM avm2 : avmList) {
					if (avm1.unifies(avm2)) {
						avm1.unify(avm2);
						if (!resolvedList.contains(avm1)) {
							resolvedList.add(avm1);
						}
					}
				}
			}			
		}
		return resolvedList;
	}
	
	/**
	 * Sets prototype AVMs to be used for matching
	 * against UntypedAVMs (e.g. from new AVPairs).
	 */
	static public ArrayList<AVM> getAllAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("dialog_act"));
		list.add(new AVM("tile"));
		list.add(new AVM("field"));
		return list;
	}

	static public ArrayList<AVM> getObjectAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("tile"));
		list.add(new AVM("field"));
		return list;
	}
	
	static public ArrayList<AVM> getFieldAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("field"));
		return list;
	}

	static public ArrayList<AVM> getTileAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("tile"));
		return list;
	}

	static public ArrayList<AVM> getDialogActAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("dialog_act"));
		return list;
	}

	public void setAllAVMs() {
		this.avmList = getAllAVMs();
	}

	public void setObjectAVMs() {
		this.avmList = getObjectAVMs();
	}

	public void setFieldAVMs() {
		this.avmList = getFieldAVMs();
	}

	public void setTileAVMs() {
		this.avmList = getTileAVMs();
	}

	public void setDialogActAVMs() {
		this.avmList = getDialogActAVMs();
	}

	public void unsetAVMs(String type) {
		ArrayList<AVM> removeList = new ArrayList<AVM>();
		for (AVM avm : this.avmList) {
			if (avm.getType().equals(type)) {
				removeList.add(avm);
			}
		}
		this.avmList.removeAll(removeList);
		System.err.println(this.avmList.toString());
	}

	public void unsetDialogActAVMs() {
		this.avmList.remove(new AVM("dialog_act"));
		System.err.println(this.avmList.toString());
	}

	public void unsetTileAVMs() {
		this.avmList.remove(new AVM("tile"));
		System.err.println(this.avmList.toString());
	}

	public void unsetFieldAVMs() {
		this.avmList.remove(new AVM("field"));
		System.err.println(this.avmList.toString());
	}

	/**
	 * Prints out all known AVMs.
	 */
	private void printAVMs() {
		System.out.println("Composed AVMs:");
		if (avmList != null) {
			for (AVM a : this.avmList) {
				System.out.println(a.toString());
			}			
		} else {
			System.out.println("none");
		}
	}
	
	/**
	 * @return the avmList
	 */
	public ArrayList<AVM> getAvmList() {
		return avmList;
	}

	/**
	 * @param avmList the avmList to set
	 */
	public void setAvmList(ArrayList<AVM> avmList) {
		this.avmList = avmList;
	}

}