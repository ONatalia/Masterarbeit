package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AVM Composer - Reads AVPairs.  Attempts composition of
 * new AVMs and subsequent unification of existing ones.
 * @author okko
 */
public class AVMComposer {

	private List<AVM> avmPrototypes = new ArrayList<AVM>();
	private List<AVM> avmList = new ArrayList<AVM>();
	
	/**
	 * Creates AVMComposer with a list of prototypes of
	 * different typed AVMs.
	 * New AVPairs will first be put into an UntypedAVM,
	 * for which unification will be attempted on each
	 * of these.  Non-null ones will be kept.
	 */
	AVMComposer() {
		this.setPrototypeAVMs();
	}
	
	public static void main(String[] args) {
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();		

		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		avps.add(new AVPair("name", "kreuz"));
		avps.add(new AVPair("color", "grün"));
		avps.add(new AVPair("ord", "1"));
		avps.add(new AVPair("orient", "top"));
//		avps.add(new AVPair("rel", "next_to"));
//		avps.add(new AVPair("rel", "above"));

		for (AVPair avp : avps) {
			composer.unifyNewAVPair(avp);
			composer.printAVMs();
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
	private void unifyNewAVPair(AVPair avp) {
		System.out.println("Adding tag AVPair '" + avp.toString() + "'.");
		this.setPrototypeAVMs();
		UntypedAVM untypedAVM = new UntypedAVM(avp);
		ArrayList<AVM> listToAdd = new ArrayList<AVM>();
		for (AVM protoAVM : avmPrototypes) {
			if (protoAVM.unify(untypedAVM) != null) {
				if (!listToAdd.contains(protoAVM)) {
					listToAdd.add(protoAVM);
				}
			}
		}
		ArrayList<AVM> secondListToAdd = new ArrayList<AVM>();
		for (AVM protoAVM : avmPrototypes) {
			for (AVM newAVM : listToAdd) {
				if (protoAVM.unify(newAVM) != null) {
					if (!secondListToAdd.contains(protoAVM)) {
						secondListToAdd.add(protoAVM);
					}
				}
			}
		}
		for (AVM avm : secondListToAdd) {
			if (!listToAdd.contains(avm)) {
				listToAdd.add(avm);
			}
		}
		ArrayList<AVM> thirdListToAdd = new ArrayList<AVM>();
		for (AVM newAVM: listToAdd) {
			for (AVM avm : avmList) {
				if (avm.unify(newAVM) != null) {
					if (!thirdListToAdd.contains(newAVM)) {
						thirdListToAdd.add(newAVM);
					}
				}
				if (newAVM.unify(avm) != null) {
					if (!thirdListToAdd.contains(newAVM)) {
						thirdListToAdd.add(newAVM);
					}
				}
			}
		}
		for (AVM avm : thirdListToAdd) {
			if (!listToAdd.contains(avm)) {
				listToAdd.add(avm);
			}
		}
		for (AVM a : listToAdd) {
			if (!avmList.contains(a)) {
				avmList.add(a);
			}
		}
//		avmList.addAll(secondListToAdd);
//		avmList.addAll(thirdListToAdd);
//		Set<AVM> set = new HashSet<AVM>(avmList);
//		avmList = new ArrayList<AVM>(set);
	}

	/**
	 * Sets prototype AVMs to be used for matching
	 * against UntypedAVMs (e.g. from new AVPairs).
	 */
	private void setPrototypeAVMs() {
		avmPrototypes.clear();
		avmPrototypes.add(new TileAVM());
		avmPrototypes.add(new FieldAVM());
		avmPrototypes.add(new LocationAVM());
		avmPrototypes.add(new RelativeLocationAVM());
		avmPrototypes.add(new RowAVM());
		avmPrototypes.add(new ColumnAVM());
	}

	/**
	 * Prints out all known AVMs.
	 */
	private void printAVMs() {
		System.out.println("AVMs:");
		for (AVM a : this.avmList) {
			System.out.println(a.toString());
		}
	}
}