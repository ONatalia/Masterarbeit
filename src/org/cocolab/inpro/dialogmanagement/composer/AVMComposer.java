package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.List;

public class AVMComposer {

	private List<AVM> avmList = new ArrayList<AVM>();
	
	AVMComposer() {
	}
	
	public static void main(String[] args) {
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer();		

		// Below is a demonstration of what happens when tags come in.
		AVPair avp1 = new AVPair("name", "kreuz");
		System.out.println("Adding tag 'kreuz'.");		
		TileAVM tile = new TileAVM(avp1);
		composer.avmList.add(tile);

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}

		System.out.println("Adding tag 'grün'.");
		AVPair avp2 = new AVPair("col", "grün");
		FieldAVM field = new FieldAVM(avp2);
		composer.avmList.add(field);
		tile.setColor(avp2.getValue());

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}

		System.out.println("Adding tag 'ecke'.");
		AVPair avp3 = new AVPair("name", "ecke");
		LocationAVM loc = new LocationAVM(avp3);
		composer.avmList.add(loc);
		tile.setLocation(loc);
		field.setLocation(loc);		

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}

		System.out.println("Adding tag 'next_to'.");
		AVPair avp4 = new AVPair("relation", "next_to");
		RelativeLocationAVM rel_loc = new RelativeLocationAVM(avp4);
		composer.avmList.add(rel_loc);
		field.addRelativeLocation(rel_loc);
		tile.addRelativeLocation(rel_loc);

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}

		System.out.println("Adding tag 'right'.");
		AVPair avp5 = new AVPair("orient", "right");
		RowAVM row = new RowAVM(avp5);
		composer.avmList.add(row);
		loc.setRow(row);

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}

		System.out.println("Adding tag '1'.");
		AVPair avp6 = new AVPair("ord", "1");
		ColumnAVM col = new ColumnAVM(avp6);
		composer.avmList.add(col);
		loc.setColumn(col);

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}
	}

}
