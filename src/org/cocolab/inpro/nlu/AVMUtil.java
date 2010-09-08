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
	 * List of fully specified AVMs representing world objects (available to resolution).
	 */
	private static ArrayList<AVM> worldList = new ArrayList<AVM>();
	/**
	 * List of underspecified AVMs representing possible input (available to composition).
	 */
	private ArrayList<AVM> composeList;
	/**
	 * List of AVMs that resolved (a subset of worldList).
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
	 * @return delta List of AVM that changed during update.
	 */
	public ArrayList<AVM> compose(AVPair avp) {
		ArrayList<AVM> delta = new ArrayList<AVM>();
		for (AVM avm : this.composeList) {
			if (avm.setAttribute(avp)) {
				delta.add(avm);
			}
		}
		return delta;
	}

	/**
	 * Calls compose() for the list of AVPairs.
	 * @param avPairs list of AVPairs to be composed
	 * @return delta List of AVMs that have changed during update.
	 */
	public ArrayList<AVM> composeAll(List<AVPair> avPairs) {
		ArrayList<AVM> delta = new ArrayList<AVM>();
		if (avPairs != null) {
			for (AVPair pair : avPairs) {
				delta.addAll(this.compose(pair));
			}
		}
		return delta;
	}

	/**
	 * Tries to unify composed AVMs in composeList with
	 * known AVMs in worldList. Loops through both lists and attempts
	 * unification. Returns successful cases.
	 * 
	 * @return resolveList the new list of resolved AVMs
	 */
	public ArrayList<AVM> resolve() {
		this.resolvedList.clear();
		for (AVM worldAVM : worldList) {
			for (AVM composedAVM : this.composeList) {
				if (worldAVM.unifies(composedAVM)) {
					worldAVM.unify(composedAVM);
					this.resolvedList.add(worldAVM);
				}
			}
		}
		return this.resolvedList;
	}
	
	/** 
	 * Resolves AVMs, returns a list of AVMs that resolved uniquely
	 * (i.e. there were no others of the same type that resolved).
	 * @return uniques List of AVPairs of AVM that resolved
	 */
	public ArrayList<AVM> uniquelyResolve() {
		this.resolve();
		ArrayList<AVM> uniques = new ArrayList<AVM>();
		for (AVM avm : resolvedList) {
			boolean resolved = true;
			for (AVM avm2 : resolvedList) {
				if (avm.getType().equals(avm2.getType()) && avm != avm2) {
					resolved = false;
				}
			}
			if (resolved) {
				uniques.add(avm);
			}
		}
		return uniques;
	}

	/**
	 * Returns list of known AVMs.
	 */
	public ArrayList<AVM> getAllAVMs() {
		ArrayList<AVM> list = new ArrayList<AVM>();
		for (String type : avmStructures.keySet()) {
			list.add(new AVM(type, avmStructures));
		}
		return list;
	}

	/**
	 * Resets AVMs used during composition.
	 */
	public void resetAVMs() {
		this.composeList = getAllAVMs();
	}

	/**
	 * Allows only one type of AVM during composition.
	 * Disallows all others.
	 * @param type The type of AVM to allow
	 */
	public void setAVMs(String type) {
		this.composeList = new ArrayList<AVM>();
		this.composeList.add(new AVM(type, avmStructures));
	}

	/**
	 * Disallows AVM of a given type during composition.
	 * Allows all others.
	 * @param type The type of AVM to disallow
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
	public void setComposeList(ArrayList<AVM> avmList) {
		this.composeList = avmList;
	}

	/**
	 * Prints out all known AVM prototypes.
	 */
	private void printComposeList() {
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
	 * Builds and returns a string representation of this AVMUtil.
	 * @return string representation of this AVMUtil.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!this.composeList.isEmpty()) {
			sb.append("Composed AVMs:\n");
			for (AVM avm : this.composeList) {
				if (!avm.isEmpty())
					sb.append(avm.toPrettyString() + "\n");
			}			
		}
		if (!this.resolvedList.isEmpty()) {
			sb.append("Resolved AVMs:\n");
			for (AVM avm : this.resolvedList) {
				if (!avm.isEmpty())
					sb.append(avm.toPrettyString() + "\n");
			}			
		}
		if (sb.length() == 0) {
			sb.append("Empty");
		}
		return sb.toString();
	}

	/**
	 * Command line test utility.
	 * Type in AVPairs "attribute:value" to test composition.
	 * Type 'exit' to quit and 'new' to restart.
	 * @throws IOException
	 */
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
			composer.printComposeList();
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
		System.out.println("Starting AVM Composer.");
		interactiveTest();
	}

}