package org.cocolab.inpro.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;


import edu.cmu.sphinx.instrumentation.Resetable;

/**
 * a canvas that contains @see{Tile}s which can be selected, moved around, placed, etc.
 */

@SuppressWarnings("serial")
public abstract class Canvas extends JPanel implements ActionListener, Resetable {

	protected static final int RELATIVE_WIDTH = 30; // measured in boxes
	protected static final int RELATIVE_HEIGHT = 24;

	public static final int SCALE = 20;

	protected static final Point[] defPoss = {
		new Point(2 * SCALE, 7 * SCALE), new Point(6 * SCALE, 7 * SCALE), 
		new Point(10 * SCALE, 7 * SCALE), new Point(14 * SCALE, 7 * SCALE), 
		new Point(19 * SCALE, 6 * SCALE), new Point(25 * SCALE, 6 * SCALE),  
		new Point(2 * SCALE, 13 * SCALE), new Point(6 * SCALE, 14 * SCALE), 
		new Point(10 * SCALE, 13 * SCALE), new Point(14 * SCALE, 13 * SCALE), 
		new Point(19 * SCALE, 13 * SCALE), new Point(25 * SCALE, 12 * SCALE),  
		new Point(2 * SCALE, 18 * SCALE), new Point(6 * SCALE, 18 * SCALE), 
		new Point(10 * SCALE, 18 * SCALE), new Point(14 * SCALE, 18 * SCALE), 
		new Point(19 * SCALE, 18 * SCALE), new Point(25 * SCALE, 18 * SCALE),  
	};
	
	protected Tile[] tiles;
	protected Tile draggingTile;
	protected Tile activeTile;
	Point clickOffset = new Point(0, 0);
	Color selectedColor = Color.green;
	Color normalColor = Color.gray;
	
	private boolean showTiles;
	public boolean labelsVisible;
	
	public Canvas() {
		tiles = createTiles();
		showTiles = true;
	}
	
	protected abstract Tile[] createTiles();
	
	public void showTiles(boolean show) {
		showTiles = show;
		repaint();
	}
	
	public void shuffleTiles() {
		Random rnd = new Random();
		shuffleTiles(rnd);
	}
	
	public void shuffleTiles(Long seed) {
		shuffleTiles(new Random(seed));
	}
	
	public void shuffleTiles(Random rnd) {
		Point[] poss = Arrays.copyOf(defPoss, defPoss.length);
		List<Point> positions = Arrays.asList(poss);
		shuffleTiles(rnd, positions);
	}

	protected void shuffleTiles(Random rnd, List<Point> positions) {
		Collections.shuffle(positions, rnd);
		setPositions(positions);
	}
	
	public Tile[] getTiles() {
		return tiles;
	}
	
	protected void setPositions(List<Point> positions) {
		if (tiles.length > positions.size()) {
			throw new ArrayIndexOutOfBoundsException("I can only shuffle arrays with at most " + positions.size() + "tiles");
		}
		Iterator<Point> posIt = positions.iterator();
		for (Tile tile : tiles) {
			tile.setPos(posIt.next());
			tile.defaultRefPoint = (Point) tile.refPoint.clone(); // dirty hack!!
		}
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
		if (showTiles) {
			for (Tile tile : tiles) {
				if ((tile != activeTile) && (tile != draggingTile)) {
					tile.draw(g, labelsVisible);
				}
			}
		}
	}
	
	public void paintTopTiles(Graphics g) {
		if (showTiles) {
			if (activeTile != null) {
				activeTile.draw(g, labelsVisible);
			}
			if (draggingTile != null) {
				draggingTile.draw(g, labelsVisible);
			}
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

	public boolean tileSelectRel(double x, double y) {
		return tileSelect(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	/*
	 * given the screen coordinates (x,y), return whether a tile has been selected and store the selected tile in activeTile
	 */
	public boolean tileSelect(int x, int y) {
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
			
	public void tileReleaseRel(double x, double y) {
		tileRelease(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	/*
	 * release an active tile at screen coordinates x,y
	 */
	public void tileRelease(int x, int y) {
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
	
	public void tileMove(int x, int y) {
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
	public static int translateBlockToPixel(double t) {
		return (int) (t * SCALE);
	}

	public static double translatePixelToBlock(int t) {
		return ((double) t) / ((double) SCALE);
	}
	

}
