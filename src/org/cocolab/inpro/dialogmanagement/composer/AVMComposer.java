package org.cocolab.inpro.dialogmanagement.composer;

import java.util.ArrayList;
import java.util.List;

public class AVMComposer {

	private List<AVM> avmList = new ArrayList<AVM>();
	
	AVMComposer() {
	}
	
	public static void main(String[] args) {
		AVMComposer composer = new AVMComposer(); 
		
		TileAVM tile = new TileAVM("kreuz");
		composer.avmList.add(tile);
		
		FieldAVM field = new FieldAVM("grün");
		composer.avmList.add(field);
		
		LocationAVM loc = new LocationAVM("ecke");
		composer.avmList.add(loc);
		tile.setLocation(loc);
		field.setLocation(loc);
		
		RelativeLocationAVM rel_loc = new RelativeLocationAVM("next_to", "t1");
		composer.avmList.add(rel_loc);
		field.addRelativeLocation(rel_loc);
		tile.addRelativeLocation(rel_loc);

		RowAVM row = new RowAVM(1);
		composer.avmList.add(row);
		loc.setRow(row);

		ColumnAVM col = new ColumnAVM("right");
		composer.avmList.add(col);
		loc.setColumn(col);

		System.out.println("AVMs:");
		for (AVM a : composer.avmList) {
			System.out.println(a.toString());
		}
	}

}
