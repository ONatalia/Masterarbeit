package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM Composer - Reads AVPairs.  Attempts composition of
 * new AVMs and subsequent unification of existing ones.
 * @author okko
 */
public class AVMComposer {

	private List<AVM> avmList = new ArrayList<AVM>();
	
	AVMComposer() {
	}
	
	public static void main(String[] args) {
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();		

		// Below is a demonstration of what should happen when tags come in.

		System.out.println("Adding tag 'name:kreuz'.");
		AVPair nameAVPair = new AVPair("name", "kreuz");
		TileAVM tile = new TileAVM(nameAVPair);
		composer.avmList.add(tile);
		composer.printAVMs();

		System.out.println("Adding tag 'col:grün'.");
		AVPair colorAVPair = new AVPair("col", "grün");
		FieldAVM field = new FieldAVM(colorAVPair);
		composer.avmList.add(field);
		tile.unify(new UntypedAVM(colorAVPair));
		composer.printAVMs();
		
		System.out.println("Adding tag 'ord:1'.");
		AVPair ordAVPair = new AVPair("ord", "1");
		RowAVM row = new RowAVM(ordAVPair);
		composer.avmList.add(row);
		ColumnAVM col = new ColumnAVM(ordAVPair);
		composer.avmList.add(col);
		LocationAVM location = new LocationAVM();
		location.unify(row);
		location.unify(col);
		composer.avmList.add(location);
		AVPair locationAVPair = new AVPair("loc", location);
		tile.setAttribute(locationAVPair);
		field.setAttribute(locationAVPair);
		composer.printAVMs();

		System.out.println("Adding tag 'rel:next_to'.");
		AVPair relLocationAVPair1 = new AVPair("rel", "next_to");
		RelativeLocationAVM relLoc1 = new RelativeLocationAVM(relLocationAVPair1);
		composer.avmList.add(relLoc1);
		tile.unify(relLoc1);
		field.unify(relLoc1);
		composer.printAVMs();

		System.out.println("Adding tag 'rel:above'.");
		AVPair relLocationAVPair2 = new AVPair("rel", "above");
		RelativeLocationAVM relLoc2 = new RelativeLocationAVM(relLocationAVPair2);
		composer.avmList.add(relLoc2);
		tile.unify(relLoc2);
		field.unify(relLoc2);
		composer.printAVMs();

	}

	private void printAVMs() {
		System.out.println("AVMs:");
		for (AVM a : this.avmList) {
			System.out.println(a.toString());
		}		
	}
}