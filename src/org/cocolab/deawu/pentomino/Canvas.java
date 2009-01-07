package org.cocolab.deawu.pentomino;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import edu.cmu.sphinx.util.props.Resetable;

@SuppressWarnings("serial")
public abstract class Canvas extends JPanel implements ActionListener, Resetable {

	static final int RELATIVE_WIDTH = 30; // measured in boxes
	static final int RELATIVE_HEIGHT = 20;

	protected Tile[] tiles;
	int scale = 20;
	protected Tile draggingTile;
	protected Tile activeTile;
	Point clickOffset = new Point(0, 0);

	Color selectedColor = Color.green;
	Color normalColor = Color.gray;

	
	boolean paintLabels;
	
	protected abstract Tile[] createTiles(boolean shuffle, int numTiles, Random rnd);

	protected Tile[] createTiles() {
		// do not shuffle tiles by default
		Random rnd = new Random();
		// use setSeed to make reproducible randomness
		rnd.setSeed(1);
		Tile[] tiles = createTiles(false, 12, rnd);
		return tiles;
	}

	public Canvas() {
		tiles = createTiles();
	}
	
	public void reset() {
		tiles = createTiles();
		draggingTile = null;
		activeTile = null;
		repaint();
	}

	public void setLabels(List<String> labels) {
		assert (tiles.length == labels.size());
		Iterator<String> li = labels.iterator();
		for (Tile t : tiles) {
			t.setLabel(li.next());
		}
	}
	
	public void paintTiles(Graphics g) {
		for (Tile tile : tiles) {
			if ((tile != activeTile) && (tile != draggingTile)) {
				tile.draw(g, paintLabels);
			}
		}
	}
	
	public void paintTopTiles(Graphics g) {
		if (activeTile != null) {
			activeTile.draw(g, paintLabels);
		}
		if (draggingTile != null) {
			draggingTile.draw(g, paintLabels);
		} 
	}
	
	protected void superPaint(Graphics g) {
		super.paint(g);
	}
	
	public void paint(Graphics g) {
		superPaint(g);
		paintTiles(g);
		paintTopTiles(g);
	}

	/**
	 * do tile actions for actions performed on buttons
	 */
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (activeTile != null) {
			activeTile.doCommand(command);
		}
	}

	boolean tileSelectRel(double x, double y) {
		return tileSelect(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	/*
	 * given the screen coordinates (x,y), return whether a tile has been selected and store the selected tile in activeTile
	 */
	boolean tileSelect(int x, int y) {
		boolean selectionSuccessful = false;
		// deal with tiles
		Point p = new Point(x, y);
		draggingTile = null;
		// Placed pieces appear to be underneath non-placed ones, 
		// so non-placed pieces get the first chance at being selected.
		for (Tile tile : tiles) {
			if (!tile.placed && tile.matchesPosition(p)) {
				draggingTile = tile;
				break;
			}
		}
		// if no unplaced tile has been selected, try to select a placed tile 
		if (draggingTile == null) {
			for (Tile tile : tiles) {
				if (tile.placed && tile.matchesPosition(p)) {
					draggingTile = tile;
					break;
				}
			}
		}
		// if another piece is still selected, unselect it
		if (activeTile != null) {
			System.out.println("Unselected: " + activeTile.defaultRefPoint
					+ " " + activeTile.name);
			if (!activeTile.placed) {
				activeTile.setColor(normalColor);
			}
			activeTile = null;
		}
		if (draggingTile != null) {
			System.out.println("Selected: " + draggingTile.defaultRefPoint + " "
					+ draggingTile.name);
			if (draggingTile.placed) {
				draggingTile.unplace();
			}
			draggingTile.setColor(selectedColor);
			clickOffset.copy(p);
			clickOffset.sub(draggingTile.refPoint);
			selectionSuccessful = true;
		}
		return selectionSuccessful;
	}
			
	void tileReleaseRel(double x, double y) {
		tileRelease(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	/*
	 * release an active tile at screen coordinates x,y
	 */
	void tileRelease(int x, int y) {
		if (draggingTile != null) {
			activeTile = draggingTile;
			/* next line: don't allow selection for flips or rotates while a piece is in the Grid */
			if (activeTile.placed) {
				activeTile = null;
			}
			draggingTile = null; // nothing to drag anymore
		}
	}
	
	public void tileMoveRel(double x, double y) {
		tileMove(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	void tileMove(int x, int y) {
		if (draggingTile != null) {
			Point p = new Point(x, y);
			p.sub(clickOffset);
			draggingTile.setPos(p);
		}
	}
	
	public void tileMove(Point p) {
		tileMove(p.x, p.y);
	}
	

	
	/**
	 * translate coordinates given in blocks into coordinates given in pixels
	 * @param t
	 * @return
	 */
	int translateBlockToPixel(double t) {
		return (int) (t * scale);
	}

	double translatePixelToBlock(int t) {
		return ((double) t) / ((double) scale);
	}
	

}
