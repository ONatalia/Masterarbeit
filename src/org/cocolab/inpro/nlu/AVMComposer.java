package org.cocolab.inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

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

 	private Logger logger = Logger.getLogger(AVMComposer.class);
 	
	/**
	 * Creates AVMComposer with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (avmList).
	 * @throws MalformedURLException 
	 */
	public AVMComposer() throws MalformedURLException {
		AVMComposer.avmStructures = AVMStructureUtil.parseStructureFile(new URL("res/AVMStructure"));
		worldList = AVMWorldUtil.setAVMsFromFile("res/AVMWorldList", avmStructures);
		avmList = getAllAVMs();
		resolvedList = new ArrayList<AVM>();
		keepList = new ArrayList<AVM>();
	}

	/**
	 * Creates AVMComposer with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (avmList).
	 * Defaults to res/PentoAVMStructures for structure file.
	 * @param worldFile with list of AVMs in the world. 
	 * @throws MalformedURLException 
	 */
	public AVMComposer(String worldFile) throws MalformedURLException {
		AVMComposer.avmStructures = AVMStructureUtil.parseStructureFile(new URL("file:res/PentoAVMStructure"));
		worldList = AVMWorldUtil.setAVMsFromFile(worldFile, avmStructures);
		avmList = getAllAVMs();
		resolvedList = new ArrayList<AVM>(worldList.size());
		keepList = new ArrayList<AVM>();
	}

	/**
	 * Creates AVMComposer with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (avmList).
	 * @param structureFile with list of AVMstucture
	 * @param worldFile with list of AVMs in the world 
	 * @throws MalformedURLException 
	 */
	public AVMComposer(String structureFile, String worldFile) throws MalformedURLException {
		AVMComposer.avmStructures = AVMStructureUtil.parseStructureFile(new URL(structureFile));
		worldList = AVMWorldUtil.setAVMsFromFile(worldFile, avmStructures);
		avmList = getAllAVMs();
		resolvedList = new ArrayList<AVM>(worldList.size());
		keepList = new ArrayList<AVM>();
	}

	public AVMComposer(AVMComposer c) {
		this.avmList = new ArrayList<AVM>(c.avmList.size());
		for (AVM avm : c.avmList) {
			this.avmList.add(new AVM(avm));
		}
		resolvedList = new ArrayList<AVM>(c.resolvedList.size());
		for (AVM avm : c.resolvedList) {
			this.resolvedList.add(new AVM(avm));
		}
		keepList = new ArrayList<AVM>(c.keepList.size());
		for (AVM avm : c.keepList) {
			this.keepList.add(new AVM(avm));
		}
	}

	/**
	 * Method to call when a new AVPair becomes known.
	 * Attempt unification with known AVM prototypes (from avmStructures)
	 * which incrementally grow with more input.
	 * @param avp
	 */
	public ArrayList<AVM> compose(AVPair avp) {
		// Initialize a new list
		ArrayList<AVM> newList = new ArrayList<AVM>();
		// Try setting new AVPair on all known AVMs
		for (AVM avm : this.avmList) {
			if (avm.setAttribute(avp)) {
				// Keep successful ones.
				newList.add(avm);
			}
		}
		if (this.avmList.size() >= newList.size()) {
			this.avmList = newList;
		}
		logger.info("Composed " + this.avmList.size() + ":");
		logger.info(this.avmList.toString());
		return this.avmList;
	}

	/**
	 * Calls compose() for the list of AVPairs
	 * @param avPairs list of AVPairs to be composed
	 */
	public void composeAll(List<AVPair> avPairs) {
		if (avPairs != null) {
			for (AVPair pair : avPairs) {
				this.compose(pair);
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
		// Loop through known and world AVMs, attempt unification
		this.resolvedList.clear();
		for (AVM worldAVM : worldList) {
			for (AVM composedAVM : this.avmList) {
				if (worldAVM.unifies(composedAVM)) {
					worldAVM.unify(composedAVM);
					this.resolvedList.add(worldAVM);
				}
			}
		}
		logger.info("Resolved " + this.resolvedList.size() + ":");
		logger.info(this.resolvedList.toString());
		return this.resolvedList;
	}
	
	/** 
	 * Resolves AVMs, returns only a single AVPair of one AVM if one was resolved (else null).
	 * @return AVPair of AVM that resolved
	 */
	public AVPair uniquelyResolve() {
		this.resolve();
		if (this.resolvedList.size() == 1) {
			this.setAllAVMs();
			return this.resolvedList.get(0).getAVPairs().get(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns list of resolved AVMs (ones with which input AVPairs could
	 * be unified.)
	 * @return list of resolved AVMs
	 */
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
		logger.info("Composed AVMs:");
		if (avmList != null) {
			for (AVM a : this.avmList) {
				logger.info(a.toString());
			}			
		} else {
			logger.info("none");
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

	/**
	 * Main method mostly for local testing.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		//For debug only...
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();

		System.out.println("World contains following objects:");
		for (AVM avm : worldList) {
			System.out.println(avm.toString());
		}

		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		
		avps.add(new AVPair("act", "take"));
		avps.add(new AVPair("yesno", "yes"));
		avps.add(new AVPair("yesno", "no"));

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