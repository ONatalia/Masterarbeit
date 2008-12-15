package org.cocolab.deawu.pentomino;

import java.awt.Color;
import java.awt.Graphics;

public abstract class Tile {
	String name;
	String label;
	Point  refPoint = new Point(0, 0);
	Point defaultRefPoint = new Point(0, 0);   // defaultRefPoint can be used for resetting
	Color   color, defaultColor;         // defaultColor    can be used for resetting
	int     scale;

	// draw the tile, if l is true, the label should be drawn
	public abstract void draw(Graphics g, boolean l);
	
	public void draw(Graphics g) {
		draw(g, true); // do not print labels if no parameter was given
	}

	public boolean matchesPosition(Point p) {
		// by default, nothing matches
		return false;
	}

	public void setColor(Color c) {
		this.color = c;
	}
	
	public void setLabel(String l) {
		this.label = l;
	}
	
	public void setPos(Point p)	{
		refPoint = (Point) p.clone();        /* change refPoint to new coords */
	}
 
	public void doCommand(String command) {
		throw new RuntimeException("Illegal command for tile manipulation");
	}

}
