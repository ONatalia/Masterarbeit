package inpro.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import edu.cmu.sphinx.instrumentation.Resetable;

/**
 * a canvas that contains @see{Tile}s which can be selected, moved around, placed, etc.
 * 
 * tiles within the canvas live in a logical coordinate system, which is mapped
 * to screen pixels using the final SCALE variable.
 * the canvas has a size of RELATIVE_WIDTH * RELATIVE_HEIGHT
 * 
 * a tile in a canvas can be selected and this tile can then be moved around  
 * 
 * @see Tile
 */

@SuppressWarnings("serial")
public abstract class Canvas extends JPanel implements ActionListener, Resetable {

	/** width of the canvas */
	protected static final int RELATIVE_WIDTH = 30; // measured in boxes
	/** height of the canvas */
	protected static final int RELATIVE_HEIGHT = 24;

	/** scales the logical coordinate system of the canvas to pixel coordinates */
	public static final int SCALE = 19;

	protected List<Tile> tiles;
	protected Tile draggingTile;
	protected Tile activeTile;
	Point clickOffset = new Point(0, 0);
	
	private boolean showTiles;
	public boolean labelsVisible;
	
	public Canvas() {
		this(new Dimension(800, 600));
	}
	
	public Canvas(Dimension preferredSize) {
		tiles = createTiles();
		setPreferredSize(preferredSize);
		showTiles = true;
	}
	
	protected abstract List<Tile> createTiles();
	
	public void showTiles(boolean show) {
		showTiles = show;
		repaint();
	}
/*	
	protected void shuffleTiles(Random rnd, List<Point> positions) {
		Collections.shuffle(positions, rnd);
		setPositions(positions);
	}
*/	
	public List<Tile> getTiles() {
		return tiles;
	}
	
	protected void setPositions(List<Point> positions) {
		if (tiles.size() > positions.size()) {
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
		assert (tiles.size() == labels.size());
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
	
	/**
	 * unlike tileSelect, this returns the tiles at a point 
	 * regardless of their selection/dragging/placement state
	 * @param p
	 * @return a list of tiles (possibly empty but never null) at point t
	 */
	public List<Tile> getTilesAt(java.awt.Point p) {
		List<Tile> returnList = new ArrayList<Tile>(1); // there will usually be just one tile to return
		for (Tile t : tiles) {
			if (t.matchesPosition(p)) {
				returnList.add(t);
			}
		}
		return returnList;
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
			if (!tile.isPlaced() && tile.matchesPosition(p)) {
				draggingTile = tile;
				draggingTile.select();
				break;
			}
		}
		// if no unplaced tile has been selected, try to select a placed tile 
		if (draggingTile == null) {
			for (Tile tile : tiles) {
				if (tile.isPlaced() && tile.matchesPosition(p)) {
					draggingTile = tile;
					draggingTile.select();
					break;
				}
			}
		}
		// if another piece is still selected, unselect it
		if (activeTile != null) {
			System.out.println("Unselected: " + activeTile.defaultRefPoint
					+ " " + activeTile.name);
			if (!activeTile.isPlaced()) {
				//activeTile.setColor(normalColor);
				activeTile.unselect();
			}
			activeTile = null;
		}
		if (draggingTile != null) {
			System.out.println("Selected: " + draggingTile.defaultRefPoint + " "
					+ draggingTile.name);
			if (draggingTile.isPlaced()) {
				draggingTile.unplace();
			}
			draggingTile.select();
//			draggingTile.setColor(selectedColor);
			clickOffset.copy(p);
			clickOffset.sub(draggingTile.refPoint);
			selectionSuccessful = true;
		}
		return selectionSuccessful;
	}
	
	public void tileUnselect() {
		if (activeTile != null) {
			activeTile.unselect();
			activeTile = null;
		}
	}
			
	public void tileReleaseRel(double x, double y) {
		tileRelease(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	/*
	 * release an active tile at screen coordinates x,y
	 */
	@SuppressWarnings("unused")
	public void tileRelease(int x, int y) {
		if (draggingTile != null) {
			activeTile = draggingTile;
			/* next line: don't allow selection for flips or rotates while a piece is in the Grid */
			if (activeTile.isPlaced()) {
				activeTile.unselect();
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
	 * @param t coordinate in block-domain
	 * @return coordinate in pixel-domain
	 */
	public static int translateBlockToPixel(double t) {
		return (int) (t * SCALE);
	}

	public static double translatePixelToBlock(int t) {
		return ((double) t) / ((double) SCALE);
	}
	

}
