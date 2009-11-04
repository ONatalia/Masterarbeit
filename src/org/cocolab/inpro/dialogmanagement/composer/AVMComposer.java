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
		avm1.setAttribute(new AVPair("name", "cross"));
		avm1.setAttribute(new AVPair("color", "green"));;
		AVM avm2 = new AVM("tile");
		avm2.setAttribute(new AVPair("name", "gun"));
		avm2.setAttribute(new AVPair("color", "green"));
		AVM avm3 = new AVM("field");
		avm3.setAttribute(new AVPair("color", "green"));
		AVM avm4 = new AVM("field");
		avm4.setAttribute(new AVPair("color", "brown"));
		worldList.add(avm1);
		worldList.add(avm2);
		worldList.add(avm3);
		worldList.add(avm4);
	}
	
	/**
	 * Creates AVMComposer with a list of prototypes of
	 * different typed AVMs.
	 * New AVPairs will first be put into an UntypedAVM,
	 * for which unification will be attempted on each
	 * of these.  Non-null ones will be kept.
	 */
	public AVMComposer() {
		this.avmList = getObjectAVMs();
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();
		
		System.out.println("World contains following objects:");
		System.out.println(worldList.toString());

		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		
//		avps.add(new AVPair("relation", "above"));
//		avps.add(new AVPair("ord", "1"));
		avps.add(new AVPair("color", "grün"));
		avps.add(new AVPair("name", "kreuz"));
//		avps.add(new AVPair("ord", "2"));
//		avps.add(new AVPair("ord", "3"));
//		avps.add(new AVPair("orient", "top"));
//		avps.add(new AVPair("relation", "next_to"));
//		avps.add(new AVPair("color", "gelb"));  // this should break

		for (AVPair avp : avps) {
			if (composer.avmList != null) {
				composer.unifyNewAVPair(avp);
				composer.printAVMs();
			}
			if (composer.avmList != null) {
				AVM avm = composer.resolve();
				if (avm != null) {
					System.out.println("Found one that resolves...");
					System.out.println(avm.toString());
					break;
				}				
			} else {
				System.out.println("Stopping unification - last tag didn't unify..");
				break;
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
		System.out.println("Adding tag AVPair '" + avp.toString() + "'.");
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
			avmList = null;
		}
	}

	public AVM resolve() {
		ArrayList<AVM> resolvedList = new ArrayList<AVM>();
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
		if (resolvedList.size() == 1) {
			return resolvedList.get(0);
		} else {
			System.out.println("found " + resolvedList.size());
			return null;
		}
	}
	
	/**
	 * Sets prototype AVMs to be used for matching
	 * against UntypedAVMs (e.g. from new AVPairs).
	 */
	static public ArrayList<AVM> getObjectAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		list.add(new AVM("tile"));
		list.add(new AVM("field"));
		return list;
	}

	/**
	 * Prints out all known AVMs.
	 */
	private void printAVMs() {
		System.out.println("AVMs:");
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