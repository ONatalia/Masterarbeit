package org.cocolab.inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * General AVM Utility for composition and resolution via unification.
 * @author okko
 */
public class AVMUtil {

	/**
	 * Variable for storing AVM structures.
	 */
	private static HashMap<String, HashMap<String, String>> avmStructures;
	
	/**
	 * List of AVMs representing world objects (available to resolution).
	 */
	private static ArrayList<AVM> worldList = new ArrayList<AVM>();
	/**
	 * List of AVMs representing underspecified input (available to composition).
	 */
	private ArrayList<AVM> composeList;
	/**
	 * List of AVMs representing list of currently resolved AVMs (a subset of worldList).
	 */
	private ArrayList<AVM> resolvedList;
	/**
	 * Logger for AVMUtil
	 */
	private Logger logger = Logger.getLogger(AVMUtil.class);
 	
	/**
	 * Creates AVMUtil with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (composeList).
	 */
	public AVMUtil() {
		AVMUtil.avmStructures = AVMStructureUtil.parseStructureFile("res/PentoAVMStructure");
		worldList = AVMWorldUtil.setAVMsFromFile("res/PentoAVMWorldList", avmStructures);
		composeList = getAllAVMs();
		resolvedList = new ArrayList<AVM>();
	}

	/**
	 * Creates AVMUtil with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (composeList).
	 * Defaults to res/PentoAVMWorldList for world file.
	 * @param structureFile with list of AVM structures. 
	 */
	public AVMUtil(String worldFile) {
		AVMUtil.avmStructures = AVMStructureUtil.parseStructureFile(worldFile);
		worldList = AVMWorldUtil.setAVMsFromFile("res/PentoAVMWorldList", avmStructures);
		composeList = getAllAVMs();
		resolvedList = new ArrayList<AVM>(worldList.size());
	}

	/**
	 * Creates AVMUtil with a list of prototypes (avmStructures) of
	 * different typed AVMs and a local list of composed AVMs (composeList).
	 * @param structureFile with list of AVMstucture
	 * @param worldFile with list of AVMs in the world 
	 */
	public AVMUtil(String structureFile, String worldFile) {
		AVMUtil.avmStructures = AVMStructureUtil.parseStructureFile(structureFile);
		worldList = AVMWorldUtil.setAVMsFromFile(worldFile, avmStructures);
		composeList = getAllAVMs();
		resolvedList = new ArrayList<AVM>(worldList.size());
	}

	public AVMUtil(AVMUtil c) {
		this.composeList = new ArrayList<AVM>(c.composeList.size());
		for (AVM avm : c.composeList) {
			this.composeList.add(new AVM(avm));
		}
		resolvedList = new ArrayList<AVM>(c.resolvedList.size());
		for (AVM avm : c.resolvedList) {
			this.resolvedList.add(new AVM(avm));
		}
	}

	/**
	 * Method to call when a new AVPair becomes known.
	 * Attempt unification with known AVM prototypes (from avmStructures)
	 * which incrementally grow with more input into composeList.
	 * @param avp the AVPair to compose with
	 * @return composeList the updated list of AVMs available for composition.
	 */
	public ArrayList<AVM> compose(AVPair avp) {
		// Try setting new AVPair on all known AVMs
		for (AVM avm : this.composeList) {
			avm.setAttribute(avp);
		}
		return this.composeList;
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
	 * Method to resolve composed AVMs in composeList with
	 * known AVMs in worldList. Loops through both and attempts
	 * unification.
	 * 
	 * @return resolveList the new list of resolved AVMs
	 */
	public ArrayList<AVM> resolve() {
		// Loop through composed and world AVMs, attempt unification
		this.resolvedList.clear();
		for (AVM worldAVM : worldList) {
			for (AVM composedAVM : this.composeList) {
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
	 * Resolves AVMs, returns a list of AVPairs for all AVMs
	 * that resolved as the only ones of their type.
	 * @return AVPair of AVM that resolved
	 */
	public ArrayList<AVPair> uniquelyResolve() {
		this.resolve();
		ArrayList<AVPair> uniqueTypes = new ArrayList<AVPair>();
		for (AVM avm : resolvedList) {
			boolean resolved = true;
			for (AVM avm2 : resolvedList) {
				if (avm.getType().equals(avm2.getType()) && avm != avm2) {
					resolved = false;
				}
			}
			if (resolved) {
				uniqueTypes.add(avm.getAVPairs().get(0));
			}
		}
		this.setAllAVMs();
		return uniqueTypes;
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
	 * getAllAVMs() sets these from previously known structures.
	 */
	public void setAllAVMs() {
		this.composeList = getAllAVMs();
	}

	/**
	 * Sets only one type of AVM by creating a compose list of only that type.
	 * @param type
	 */
	public void setAVMs(String type) {
		this.composeList = new ArrayList<AVM>();
		this.composeList.add(new AVM(type, avmStructures));
	}

	/**
	 * Unsets AVM of a given type by removing them from composeList list.
	 * @param type
	 */
	public void unsetAVMs(String type) {
		ArrayList<AVM> removeList = new ArrayList<AVM>();
		for (AVM avm : this.composeList) {
			if (avm.getType().equals(type)) {
				removeList.add(avm);
			}
		}
		this.composeList.removeAll(removeList);
	}

	/**
	 * Sets the list of AVMs available for composition.
	 * @param avmList the avmList to set
	 */
	public void setAVMList(ArrayList<AVM> avmList) {
		this.composeList = avmList;
	}

	/**
	 * Prints out all known AVM prototypes.
	 */
	private void printAVMs() {
		logger.info("Composed AVMs:");
		if (composeList != null) {
			for (AVM a : this.composeList) {
				logger.info(a.toString());
			}			
		} else {
			logger.info("none");
		}
	}
	
	/**
	 * Returns a list of composed AVMs.
	 * @return the composeList
	 */
	public ArrayList<AVM> getAVMList() {
		return this.composeList;
	}

	/**
	 * Returns a list of composed AVMs that are not empty
	 * (i.e. that have already been composed into).
	 * @return the list of non-empty AVMs
	 */
	public ArrayList<AVM> getNonEmptyAVMList() {
		ArrayList<AVM> nonEmpties = new ArrayList<AVM>();
		for (AVM avm : this.composeList) {
			if (!avm.isEmpty()) {
				nonEmpties.add(avm);
			}
		}
		return nonEmpties;
	}
	
	/**
	 * Checks another AVMUtil's composed AVM list.
	 * Returns a list of (non-empty) AVMs that differ
	 * or are not in the old AVMUtil's list.
	 * @param lastAVMUtil
	 * @return
	 */
	public ArrayList<AVM> diffComposedAVMLists(AVMUtil au) {
		ArrayList<AVM> list = this.diffComposedAVMLists(au.getNonEmptyAVMList());
		return list;
	}

	/**
	 * Compares this AVMUtil's composed AVM list with another AVM list.
	 * Returns a list of (non-empty) AVMs in this AVMUtils list that differ
	 * or are not in the other list.
	 * @param lastAVMUtil
	 * @return
	 */
	public ArrayList<AVM> diffComposedAVMLists(ArrayList<AVM> oldList) {
//		System.err.println("Diffing new list\n" + this.getNonEmptyAVMList().toString() + "\nAnd old list:\n" + oldList.toString());
		if (oldList.size() == 0) {
			return this.getNonEmptyAVMList();
		}
		ArrayList<AVM> newList = new ArrayList<AVM>();
		for (AVM avm : this.getNonEmptyAVMList()) {
			boolean isNew = true;
			for (AVM avm2 : oldList) {
//				System.err.println("Diffing " + avm.toPrettyString() + " and " + avm2.toPrettyString() + ".");
				if (avm.equals(avm2) || avm == avm2) {
//					System.err.println("  Same: new AVM " + avm.toPrettyString() + " and " + avm2.toPrettyString() + ".");
					isNew = false;
				} else {
//					System.err.println("Differ: new AVM " + avm.toPrettyString() + " and " + avm2.toPrettyString() + ".");
				}
			}
			if (isNew) {
//				System.err.println("New AVM found " + avm.toPrettyString() + ".");
				newList.add(avm);
			}
		}				
		return newList;
	}

	/**
	 * Builds a string representation of this AVMUtil ()
	 * @return string representation of this AVMUtil.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Composed AVMs:\n");
		if (this.composeList.size() == 0) {
			sb.append("  Not yet set/Not currently in composition mode.\n");
		} else {
			for (AVM avm : this.composeList) {
				sb.append("   " + avm.toPrettyString() + "\n");
			}			
		}
		sb.append("Resolved AVMs:\n");
		if (this.resolvedList.size() == 0) {
			sb.append("  Not yet set/Not currently in resolution mode.\n");
		} else {
			for (AVM avm : this.resolvedList) {
				sb.append("   " + avm.toPrettyString() + "\n");
			}
		}		
		return sb.toString();
	}

	static void interactiveTest() throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String line = stdin.readLine();
		AVMUtil composer = new AVMUtil();
		while (!line.equals("exit")) {
			if (line.equals("new"))  
				composer = new AVMUtil();
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
		AVMUtil composer = new AVMUtil();

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
			if (composer.composeList != null) {
				composer.compose(avp);
				composer.printAVMs();
			} 
			if (composer.composeList != null) {
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
		composer = new AVMUtil();
		interactiveTest();
	}

}