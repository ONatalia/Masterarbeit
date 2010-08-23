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
	 * List of AVMs representing list of previously resolved AVMs.
	 */
	private ArrayList<AVM> resolvedList;
//	/**
//	 * List of AVMs representing 
//	 */
//	private ArrayList<AVM> keepList;

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
//		keepList = new ArrayList<AVM>();
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
//		keepList = new ArrayList<AVM>();
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
//		keepList = new ArrayList<AVM>();
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
//		keepList = new ArrayList<AVM>(c.keepList.size());
//		for (AVM avm : c.keepList) {
//			this.keepList.add(new AVM(avm));
//		}
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
	 * Returns a list of (non-empty) AVMs that differ.
	 * @param lastAVMUtil
	 * @return
	 */
	public ArrayList<AVM> diffAVMLists(AVMUtil au) {
		if (au.getNonEmptyAVMList().size() == 0) {
			return this.getNonEmptyAVMList();
		}
		ArrayList<AVM> list = new  ArrayList<AVM>();
		for (AVM newAVM : this.getNonEmptyAVMList()) {
			for (AVM oldAVM : au.getNonEmptyAVMList()) {
				if (newAVM.getType().equals(oldAVM.getType())) {
					if (!newAVM.equals(oldAVM)) {
						list.add(newAVM);
					}
				}
			}
		}
		return list;
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