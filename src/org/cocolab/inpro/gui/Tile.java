package org.cocolab.inpro.gui;

import java.awt.Graphics;

import edu.cmu.sphinx.instrumentation.Resetable;

/**
 * an abstract pentomino tile / puzzle piece / whatever
 * it can be drawn (this is to be implemented by derived classes),
 * selected, then moved around, 
 * and be in a placed or unplaced state, 
 * and a few more things
 * 
 * implementing classes (for now) are ImageTile (used in Greifarm),
 * and PentoTile (used in all things pentomino) 
 * 
 * @see Canvas
 */
public abstract class Tile implements Resetable {
	protected String name;
	protected String label;
	public Point refPoint = new Point(0, 0);
	protected Point defaultRefPoint = new Point(0, 0);   // defaultRefPoint can be used for resetting
	protected boolean isPlaced;
	private boolean isSelected;

	// draw the tile, if l is true, the label should be drawn
	public abstract void draw(Graphics g, boolean l);
	
	public void draw(Graphics g) {
		draw(g, true); // do not print labels if no parameter was given
	}

	abstract public boolean matchesPosition(java.awt.Point p);

	public String getName() {
		return this.name;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String l) {
		this.label = l;
	}
	
	public void setPos(Point p)	{
		refPoint = (Point) p.clone();        /* change refPoint to new coords */
	}
	
	public void reset() {
		setPos(defaultRefPoint);
		unplace();
		unselect();
	}
	
	public void doCommand(String command) {
		throw new RuntimeException("Illegal command for tile manipulation");
	}
	
	public void place() {
		this.isPlaced = true;
	}
	
	abstract public void unplace();

	public boolean isPlaced() {
		return isPlaced;
	}

	public void select() {
		this.isSelected = true;
	}
	
	public void unselect() {
		this.isSelected = false;
	}

	public boolean isSelected() {
		return isSelected;
	}

}
