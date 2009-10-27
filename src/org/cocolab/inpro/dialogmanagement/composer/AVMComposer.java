package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
		avps.add(new AVPair("rel", "next_to"));
		avps.add(new AVPair("rel", "above"));

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
		UntypedAVM untyped = new UntypedAVM(avp);
		ArrayList<AVM> firstOrder= new ArrayList<AVM>();
		for (AVM proto : avmPrototypes) {
			AVM unified = proto.unify(untyped);
			if (unified != null) {
				if (!firstOrder.contains(unified)) {
					firstOrder.add(unified);
				}
			}
		}

		ArrayList<AVM> secondOrder = new ArrayList<AVM>();
		for (AVM proto : avmPrototypes) {
			for (AVM unified : firstOrder) {
				AVM reUnified = proto.unify(unified);
				if (reUnified != null) {
					if (!secondOrder.contains(reUnified)) {
						secondOrder.add(reUnified);
					}
				}
			}
		}

		for (AVM avm : secondOrder) {
			if (!firstOrder.contains(avm)) {
				firstOrder.add(avm);
			}
		}		

		for (AVM a : firstOrder) {
			if (!avmList.contains(a)) {
				avmList.add(a);
			}
		}

//		this.setPrototypeAVMs();
//		ArrayList<AVM> thirdOrder = new ArrayList<AVM>();
//		for (AVM proto : avmPrototypes) {
//			ArrayList<AVM> sameTypes = new ArrayList<AVM>();
//			for (AVM avm : avmList) {
//				if (proto.getClass().equals(avm.getClass())) {
//					sameTypes.add(avm);
//				}
//				for (AVM sameType : sameTypes) {
//					proto.unify(sameType);
//				}
//				if (proto != null) {
//					thirdOrder.add(proto);					
//				}
//			}
//		}
//		for (AVM a : thirdOrder) {
//			if (!avmList.contains(a)) {
//				avmList.add(a);
//			}
//		}

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