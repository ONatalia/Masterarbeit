package org.cocolab.inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * AVM Composer - Reads AVPairs.  Attempts composition of
 * new AVMs and unification of existing ones.
 * @author okko
 */
public class AVMComposer {

	private static HashMap<String, HashMap<String, String>> avmStructures;
	
	// TODO: think about whether this can remain static in the future or not
	private static ArrayList<AVM> worldList = new ArrayList<AVM>();
	private ArrayList<AVM> avmList;
	private ArrayList<AVM> resolvedList;
	private ArrayList<AVM> keepList;

	/**
	 * Creates AVMComposer with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (avmList).
	 * TODO: should take filename of AVM structure file and AVM world list
	 */
	public AVMComposer() {
		AVMComposer.avmStructures = AVMStructureUtil.parseStructureFile("res/AVMStructure");
		worldList = AVMWorldUtil.setAVMsFromFile("res/AVMWorldList", avmStructures);
		avmList = getAllAVMs();
		resolvedList = new ArrayList<AVM>();
		keepList = new ArrayList<AVM>();
	}
	
	public AVMComposer(AVMComposer c) {
		avmList = new ArrayList<AVM>();
		for (AVM avm : c.avmList) {
			avmList.add(new AVM(avm));
		}
		resolvedList = new ArrayList<AVM>();
		for (AVM avm : c.resolvedList) {
			resolvedList.add(new AVM(avm));
		}
		keepList = new ArrayList<AVM>();
		for (AVM avm : c.keepList) {
			keepList.add(new AVM(avm));
		}
//		System.err.println("Copied AVMComposer:");
//		System.err.println("avmList: " + avmList.toString());
//		System.err.println("resolvedList: " + resolvedList.toString());
//		System.err.println("keepList: " + keepList.toString());
	}

	/**
	 * Method to call when a new AVPair becomes known.
	 * Attempt unification with known AVM prototypes (from avmStructures).
	 * @param avp
	 */
	public ArrayList<AVM> compose(AVPair avp) {
		// Initialize a new list
		ArrayList<AVM> newList = new ArrayList<AVM>();
		// Assume new AVPair can't be set as attribute.
		boolean placed = false;
		// Try setting new AVPair on all known AVMs
		for (AVM avm : this.avmList) {
			if (avm.setAttribute(avp)) {
				// Keep successful ones.
				newList.add(avm);
				placed = true;
			}
		}
		if (placed) {
			// If successful, set new avmList and return
			avmList = newList;
		} else {
			// Else clear avmList and return
			avmList.clear();
		}
		return avmList;
	}

	/**
	 * Calls compose for the list of AVPairs
	 * @param avPairs list of AVPairs to be composed
	 */
	public void composeAll(List<AVPair> avPairs) {
		if (avPairs != null) {
			for (AVPair pair : avPairs) {
				compose(pair);
			}	
		}		
	}

	/**
	 * Method to resolve composed AVMs in avmList with
	 * known AVMs in worldList. Loops through both and attempts
	 * unification.
	 * 
	 * @return resolveList the new list of resolved AVMs
	 */
	public ArrayList<AVM> resolve() {
		ArrayList<AVM> newlyResolved = new ArrayList<AVM>();
		// Loop through known and world AVMs, attempt unification
//		System.err.println("composed: "+ this.avmList.toString());
		for (AVM avm1 : worldList) {
			for (AVM avm2 : this.avmList) {
				if (avm1.unifies(avm2)) {
					avm1.unify(avm2);
					if (!newlyResolved.contains(avm1)) {
						newlyResolved.add(avm1);
					}
				}
			}
		}
//		System.err.println("resolved: "+ newlyResolved.toString());
		// If we resolved to a single, hitherto unknown AVM, remember it
		if (newlyResolved.size() == 1) {
			for (AVM avm : newlyResolved) {
				if (!this.keepList.contains(avm)) {
					this.keepList.add(avm);
				}
			}
//			System.err.println("keeping: "+ this.keepList.toString());
			// Reset avmList and remove those already found
			this.setAllAVMs();
			for (AVM avm : this.keepList) {
				this.unsetAVMs(avm.getType());
			}
//			System.err.println("reset: "+ this.avmList.toString());
		// If nothing was resolved input was out-of-domain and nothing returns
		} else if (newlyResolved.size() == 0) {
			this.resolvedList.clear();
			this.keepList.clear();
		}
		// Return unique list of kept and resolved AVMs
		this.resolvedList = new ArrayList<AVM>(newlyResolved);
//		System.err.println("kept: "+ this.keepList.toString());
		for (AVM avm : keepList) {
			if (!this.resolvedList.contains(avm)) {
				this.resolvedList.add(avm);
			}
		}
//		System.err.println("returned: "+ this.resolvedList.toString());
		return this.resolvedList;
	}
	
	public ArrayList<AVM> getResolvedList() {
		return this.resolvedList;
	}
	
	/**
	 * Returns list of AVMs known from AVM structures.
	 */
	public ArrayList<AVM> getAllAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		for (String type : avmStructures.keySet()) {
			list.add(new AVM(type, avmStructures));
		}
		return list;
	}

	/**
	 * Sets prototype AVMs to be used for matching
	 * against (e.g. from new AVPairs).
	 * getAllAVMs() sets these from known structures.
	 */
	public void setAllAVMs() {
		this.avmList = getAllAVMs();
	}

	/**
	 * Sets only one type of AVM by creating a compose list of only that type.
	 * @param type
	 */
	public void setAVMs(String type) {
		this.avmList = new ArrayList<AVM>();
		this.avmList.add(new AVM(type, avmStructures));
	}

	/**
	 * Unsets AVM of a given type by removing them from avmList list.
	 * @param type
	 */
	public void unsetAVMs(String type) {
		ArrayList<AVM> removeList = new ArrayList<AVM>();
		for (AVM avm : this.avmList) {
			if (avm.getType().equals(type)) {
				removeList.add(avm);
			}
		}
		this.avmList.removeAll(removeList);
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
	
	static void interactiveTest() throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String line = stdin.readLine();
		AVMComposer composer = new AVMComposer();
		while (!line.equals("exit")) {
			if (line.equals("new"))  
				composer = new AVMComposer();
			else {
				composer.compose(new AVPair(line));
			}
			composer.printAVMs();
			line = stdin.readLine();
		}
		System.exit(0);
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
		
		avps.add(new AVPair("act", "take"));


		// These should compose and resolve
//		avps.add(new AVPair("color", "green"));
//		avps.add(new AVPair("name", "f"));
//		avps.add(new AVPair("ord", "2"));
//		avps.add(new AVPair("orient", "top"));
//		avps.add(new AVPair("ord", "1"));
//		avps.add(new AVPair("orient", "bottom"));
//		avps.add(new AVPair("ord", "1"));
//		avps.add(new AVPair("relation", "next_to"));
//		avps.add(new AVPair("relation", "above"));
//		avps.add(new AVPair("relation", "below"));
		
		// These should compose but not resolve 
//		avps.add(new AVPair("ord", "4"));
//		avps.add(new AVPair("ord", "1"));
//		avps.add(new AVPair("relation", "below"));
		
		// These should compose and resolve nothing
//		avps.add(new AVPair("name", "cross"));
//		avps.add(new AVPair("ord", "1"));
//		avps.add(new AVPair("ord", "4"));
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
		System.out.println();
		System.out.println("Done! Continue composing by entering any AVPair (e.g. ord:1). Enter 'exit' to stop or 'new' to restart'");
		composer = new AVMComposer();
		interactiveTest();
	}
	
}