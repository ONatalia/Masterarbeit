package org.cocolab.inpro.gui.pentomino;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Rectangle;
import java.lang.String;

import org.cocolab.inpro.domains.pentomino.Posture;
import org.cocolab.inpro.gui.Point;
import org.cocolab.inpro.gui.Tile;

/**
 * implements pentomino tiles.
 * 
 * a tile is represented by 5 @see{Box}es.
 */
public class PentoTile extends Tile {

	public static final String CCW_ROTATE_COMMAND = "ccwRotate";
	public static final String CW_ROTATE_COMMAND = "cwRotate";
	public static final String HORIZONTAL_FLIP_COMMAND = "hFlip";
	public static final String VERTICAL_FLIP_COMMAND = "vFlip";

	final int scale;

	Point  clipOffset;                  // (refPoint - clipCorner)
	Point  clipCorner;
	Point  clipDim;

	protected Color color;         // defaultColor    can be used for resetting
	protected Color defaultColor;

	final static Color selectedColor = Color.green;
	final static Color normalColor = Color.gray;
	
	private static final int BOX_COUNT = 5; // number of boxes making up the piece
	private Box[]   boxes = new Box[BOX_COUNT];
	private int[][] defVectors = new int[2][BOX_COUNT];
	
	private TargetPattern myGrid;
	
	/** the kind of pentomino tile this object represents */
	private char type;
	
	/**
	 * the outermost array contains for each kind of pentomino tile (12)
	 * an array with two entries (X and Y) each of these containing 
	 * five positions. 
	 * FIXME: ordering is obviously wrong: it should be 12 -> 5 -> 2 instead.
	 */
	protected static int[][][] TILE_BITMAPS = {
		{{0, 0, -1, 0, 1}, {0, -1, 0, 1, -1}}, // F
		{{0, 1, 0, 0, 1}, {0, -1, -1, 1, 1}}, // U
		{{0, 0, 0, -1, 1}, {0, -1, 1, 0, 0}}, // X
		{{0, 0, -1, 1, 1}, {0, 1, 1, 0, -1}}, // W
		{{0, 0, 0, 0, 1}, {0, -1, 1, 2, 0}}, // Y
		{{0, 1, 1, 0, 0}, {0, -1, 0, 1, 2}}, // N
		{{0, -1, -1, -1, 0}, {0, -1, 0, 1, 1}}, // P
		{{0, -1, -1, -1, 1}, {0, -2, -1, 0, 0}}, // V
		{{0, 0, -1, 0, 1}, {0, 1, 1, -1, -1}}, // Z
		{{0, 0, 0, -1, 1}, {0, 1, -1, -1, -1}}, // T
		{{0, 0, 0, 0, 0}, {0, -1, -2, 1, 2}}, // I
		{{0, 0, 0, 0, 1}, {0, 1, 2, -1, -1}} // L
	};
	
	public PentoTile(int scale, Color c, int x0, int y0, char type) {
		this(scale, c, x0, y0, type, "", "");
	}
	
	public PentoTile(int scale, Color c, int x0, int y0, char type, String name, String label) {
		if (!isValidTile(type)) 
			throw new RuntimeException("Error in PentoTile: " + Character.toString(type) + " is not a valid type for a tile");
		this.defVectors = getDefVectors(type);
		this.color = c;
		this.refPoint = new Point(x0, y0);
		this.type = type;
		this.name = name;
		this.label = label;
		this.scale = scale;

		this.defaultRefPoint = this.refPoint;
		this.defaultColor = this.color;
		this.clipCorner = new Point(this.refPoint);
		this.clipOffset = new Point(this.refPoint);

		generate();  /* creates indiv. boxes and determines clip rectangle */
	}
	
	public static boolean isValidTile(char c) {
		return (c == 'F' || c == 'U' || c == 'X'
			 || c == 'W' || c == 'Y' || c == 'N'
			 || c == 'P' || c == 'V' || c == 'Z'
			 || c == 'T' || c == 'I' || c == 'L');
	}
	
	static int[][] getDefVectors(char name) {
		int[][] vector;
		assert(isValidTile(name));
		switch (name) {
			case 'F': vector = TILE_BITMAPS[0]; break; 
			case 'U': vector = TILE_BITMAPS[1]; break; 
			case 'X': vector = TILE_BITMAPS[2]; break; 
			case 'W': vector = TILE_BITMAPS[3]; break; 
			case 'Y': vector = TILE_BITMAPS[4]; break; 
			case 'N': vector = TILE_BITMAPS[5]; break; 
			case 'P': vector = TILE_BITMAPS[6]; break; 
			case 'V': vector = TILE_BITMAPS[7]; break; 
			case 'Z': vector = TILE_BITMAPS[8]; break; 
			case 'T': vector = TILE_BITMAPS[9]; break; 
			case 'I': vector = TILE_BITMAPS[10]; break; 
			case 'L': vector = TILE_BITMAPS[11]; break; 
			default: throw new RuntimeException("ahduschaisse");
		}
		int[][] returnVector = new int[2][5];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 5; j++) {
				returnVector[i][j] = vector[i][j];
			}
		}
		return returnVector;
	}

	private void generate() {
		Point p = new Point(0, 0);
		Point clipLRCorner =
			new Point(this.refPoint);
		clipLRCorner.add(scale);

		this.clipCorner.copy(this.refPoint);
		for (int i=0; i<BOX_COUNT; i++) {
			p.copy(this.refPoint);
			p.add(new Point(defVectors[0][i] *scale,
					defVectors[1][i] *scale));
			boxes[i] = new Box(p, scale, color); //generates GARBAGE
			this.clipCorner.min(p);
			p.add(scale);
			clipLRCorner.max(p);
		}
		this.clipDim = (Point) clipLRCorner.clone();
		this.clipDim.sub(clipCorner);

		//upper-left clip corner can vary relative to refPoint due to transformations
		this.clipOffset.copy(this.refPoint);
		this.clipOffset.sub(this.clipCorner);
	}


	@Override
	public void reset() {
		super.reset();
		defVectors = getDefVectors(name.charAt(0));
		generate();
		setColor(defaultColor);
	}

	@Override
	public boolean matchesPosition(java.awt.Point p) {
		boolean s;
		s = (boxes[0]).matchesPosition(p) || 
		(boxes[1]).matchesPosition(p) ||
		(boxes[2]).matchesPosition(p) || 
		(boxes[3]).matchesPosition(p) || 
		(boxes[4]).matchesPosition(p);
		return s;
	}

	@Override
	public void setPos(Point p) {
		Point delta = (Point) p.clone();
		delta.sub(refPoint);                  /* get change in Box coordinates */
//		refPoint = (Point) p.clone();        /* change refPoint to new coords */
		for (int i = 0; i < BOX_COUNT; i++) {
			(boxes[i]).corner.add(delta);
		}
		clipCorner.add(delta);
		super.setPos(p);
	}

	public void setColor(Color c) {
		for (int i=0; i<BOX_COUNT; i++) {
			(boxes[i]).setColor(c);
		}
	}

	/* Takes a boolean as additional parameter
	 * to decide whether to draw the Tile's label.
	 * */
	public void draw(Graphics g, boolean l) {
		int tx = boxes[0].corner.x;
		int ty = boxes[0].corner.y;
		if (isSelected()) {
			for (int i = 0; i < BOX_COUNT; i++) {
				boxes[i].drawSelection(g);
			}
		}
		for (int i = 0; i < BOX_COUNT; i++) {
			Box current = boxes[i];
			Point left = new Point(current.corner.x - current.dim.x, current.corner.y);
			Point right = new Point(current.corner.x + current.dim.x, current.corner.y);
			Point top = new Point(current.corner.x, current.corner.y - current.dim.y);
			Point bottom = new Point(current.corner.x, current.corner.y + current.dim.y);
			boolean drawLeft = true;
			boolean drawRight = true;
			boolean drawTop = true;
			boolean drawBottom = true;
			for (int j = 0; j < BOX_COUNT; j++) {
				if (i != j) {
					Point otherCorner = boxes[j].corner;
					drawLeft = otherCorner.equals(left) ? false : drawLeft;
					drawRight = otherCorner.equals(right) ? false : drawRight;
					drawTop = otherCorner.equals(top) ? false : drawTop;
					drawBottom = otherCorner.equals(bottom) ? false : drawBottom;
				}
			}
			boxes[i].draw(g, drawLeft, drawRight, drawTop, drawBottom);
			if (boxes[i].corner.x <= tx && boxes[i].corner.y <= ty) {
				tx = boxes[i].corner.x;
				ty = boxes[i].corner.y;
			}
		}
		if (l) {
			Rectangle box = getBoundingBox(); 
			g.setColor(Color.BLACK);
			g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
			int textWidth = g.getFontMetrics().stringWidth(this.label);
			int textHeight = g.getFontMetrics().getHeight();
			
			g.drawString(this.label, box.x - (textWidth - box.width) / 2 , 
					box.y + box.height + textHeight);
			
		}
	}
	
	public void setPosture(Posture posture) {
		this.defVectors = getDefVectors(type);
		if (posture.isMirrored()) {
			hFlip();
		}
		for (int i = 0; i < posture.quarterRotations(); i++) {
			cwRotate();
		}
		this.generate();
	}

	public void doCommand(String command) {
		if (command.equals(VERTICAL_FLIP_COMMAND))
			vFlip();
		else if (command.equals(HORIZONTAL_FLIP_COMMAND))
			hFlip();
		else if (command.equals(CW_ROTATE_COMMAND))
			cwRotate();
		else if (command.equals(CCW_ROTATE_COMMAND))
			ccwRotate();
		else 
			super.doCommand(command);
	}

	public void hFlip() {
//		System.out.println("hFlip: " + this.defaultRefPoint);
		for (int i = 1; i<BOX_COUNT; i++) {
			this.defVectors[1][i] = -1 * this.defVectors[1][i];
		}
		this.generate();
	}

	public void vFlip() {
//		System.out.println("vFlip: " + this.defaultRefPoint);
		for (int i = 1; i<BOX_COUNT; i++) {
			this.defVectors[0][i] = -1 * this.defVectors[0][i];
		}
		this.generate();
	}

	public void cwRotate() {
		int temp;
//		System.out.println("cwRotate: " + this.defaultRefPoint);
		for (int i=1; i<BOX_COUNT; i++) {
			temp = this.defVectors[0][i];
			this.defVectors[0][i] = -1 * this.defVectors[1][i];
			this.defVectors[1][i] = temp;
		}
		this.generate();
	}

	public void ccwRotate() {
		int temp;
//		System.out.println("ccwRotate: " + this.defaultRefPoint);
		for (int i=1; i<BOX_COUNT; i++) {
			temp = this.defVectors[0][i];
			this.defVectors[0][i] = this.defVectors[1][i];
			this.defVectors[1][i] = -1 * temp;
		}
		this.generate();
	}

	/*
	 * test whether the object fits into the "tray", 
	 * the internal data recording structure of Grid
	 * (it's ugly, but we'll leave it like that)
	 */
	public boolean test(Object tray[][], Point dim, Point index0) {
		boolean fit = true;
		int mi, mj;
		for (int i=0; i<BOX_COUNT; i++) {
			mi = index0.x +defVectors[0][i];
			mj = index0.y +defVectors[1][i];
			// das:
				if (mi < 0 || mi >= dim.x || mj < 0 || mj >= dim.y ||
						(tray[mi][mj] != TargetPattern.INSIDE && tray[mi][mj] != null))
					fit = false;
		}
		return fit;
	}

	/* 
	 * "place" the tile:
	 * - record the placement in tray (an internal data-structure of Grid)
	 * - change color to c[]
	 * (it's ugly, but we'll leave it like that)
	 */
	public void place(Object tray[][], Point dim, Point index0, Color c[]) {
		boolean[] cOK = new boolean[c.length];
		for(int i=0; i<c.length; i++) cOK[i] = true;
		int mi, mj;
		for (int i=0; i<BOX_COUNT; i++) {
			mi = index0.x +defVectors[0][i];
			mj = index0.y +defVectors[1][i];
			tray[mi][mj] = this;
		}

		for (int i=0; i<BOX_COUNT; i++) {
			// (mi,mj) is the array index corresp. to one Box that makes up piece.
			mi = index0.x +defVectors[0][i];
			mj = index0.y +defVectors[1][i];
			for (int k=-1; k<2; k++) {
				for (int L=-1; L<2; L++) {
					if (k*L==0 && k+L!=0 &&                // diagonal doesn't count
							mi+k>=0 && mi+k<dim.x && mj+L>=0 && mj+L<dim.y &&    // no pieces outside of m[][]
							tray[mi+k][mj+L] != this && tray[mi+k][mj+L] != null) {    // don't count spaces occupied by _this_ or not occupied at all
						for (int z = 0; z<c.length; z++) {
							// das:
							if (tray[mi+k][mj+L] == TargetPattern.INSIDE 
									&& tray[mi+k][mj+L] == TargetPattern.OUTSIDE
									&& ((PentoTile) tray[mi+k][mj+L]).color == c[z])
							{
								cOK[z] = false;
								break;
							}
						}
					}
				}
			}
		}
		for (int z = 0; z<c.length; z++) {
			if (cOK[z]) {
				this.setColor(c[z]);
				break;
			}
		}
		isPlaced = true;
	}

	public void unplace() {
		if (myGrid != null) {
			unplace(myGrid.tray, myGrid.index(refPoint));
		}
	}
	
	/*
	 * remove the tile from the "tray" (internal data structure of "Grid")
	 */
	public void unplace(Object tray[][], Point index0) {
		int mi, mj;
		for (int i=0; i<BOX_COUNT; i++) {
			mi = index0.x +defVectors[0][i];
			mj = index0.y +defVectors[1][i];
			tray[mi][mj] = null;
		}
		isPlaced = false;
	}

	public String toString() {
		return "tile: " + this.name + "\n";
	}

	public void setMyGrid(TargetPattern myGrid) {
		this.myGrid = myGrid;
	}

	public void select() {
		super.select();
//		setColor(selectedColor);
	}
	
	public void unselect() {
		super.unselect();
//		setColor(normalColor);
	}
	
	public Rectangle getBoundingBox() {
		Rectangle r = boxes[0].getBoundingBox();
		for (int i = 1; i < BOX_COUNT; i++) {
			r = r.union(boxes[i].getBoundingBox());
		}
		return r;
	}
	
	
}
