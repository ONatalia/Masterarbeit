package org.cocolab.inpro.dialogmanagement.composer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
	
	public static void main(String[] args) throws IOException {
/*		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();		

		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		
		avps.add(new AVPair("color", "grün"));
		avps.add(new AVPair("name", "kreuz"));
//		avps.add(new AVPair("ord", "1"));
		avps.add(new AVPair("orient", "top"));
//		avps.add(new AVPair("rel", "next_to"));
//		avps.add(new AVPair("rel", "above"));

		for (AVPair avp : avps) {
			composer.unifyNewAVPair(avp);
			composer.printAVMs();
		}
*/
		interactiveTest();
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

		this.setPrototypeAVMs();
		ArrayList<AVM> thirdOrder = new ArrayList<AVM>();
		for (AVM proto : avmPrototypes) {
			for (AVM avm1 : avmList) {
				AVM reUnified = proto.unify(avm1);
				if (reUnified != null) {
					for (AVM avm2 : avmList) {
						AVM reReUnified = reUnified.unify(avm2);
						if (reReUnified != null) {
							thirdOrder.add(reReUnified);
						}
					}						
				}
			}
		}
		for (AVM a : thirdOrder) {
			if (!avmList.contains(a)) {
				avmList.add(a);
			}
		}

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