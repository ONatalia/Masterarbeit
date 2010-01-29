package org.cocolab.inpro.gui;

import java.awt.Color;
import java.awt.Graphics;

import edu.cmu.sphinx.instrumentation.Resetable;

/**
 * an abstract pentomino tile / puzzle piece / whatever
 * it can be drawn (this is to be implemented by derived classes),
 * moved around, be in a placed or unplaced state, and a few more things
 * 
 * implementing classes (for now) are ImageTile (used in Greifarm),
 * and PentoTile (used in all things pentomino) 
 * 
 * @see Canvas
 */
public abstract class Tile implements Resetable {
	protected String name;
	public String label;
	public Point refPoint = new Point(0, 0);
	protected Point defaultRefPoint = new Point(0, 0);   // defaultRefPoint can be used for resetting
	protected Color color;         // defaultColor    can be used for resetting
	protected Color defaultColor;
	public boolean placed;

	// draw the tile, if l is true, the label should be drawn
	public abstract void draw(Graphics g, boolean l);
	
	public void draw(Graphics g) {
		draw(g, true); // do not print labels if no parameter was given
	}

	abstract public boolean matchesPosition(Point p);

	public void setColor(Color c) {
		this.color = c;
	}
	
	public void setLabel(String l) {
		this.label = l;
	}
	
	public void setPos(Point p)	{
		refPoint = (Point) p.clone();        /* change refPoint to new coords */
	}
	
	public void reset() {
		setPos(defaultRefPoint);
		setColor(defaultColor);
		unplace();
	}
	
	public void doCommand(String command) {
		throw new RuntimeException("Illegal command for tile manipulation");
	}
	
	abstract public void unplace();

}
