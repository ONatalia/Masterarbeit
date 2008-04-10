package org.cocolab.deawu.pentomino;

import java.awt.Graphics;
import java.net.URL;

class Grid {
	
	private static final Object OUTSIDE = "false";
	private static final Object INSIDE = "true";

	private Point pos; // position of the grid in the graphic context
	private int scale; // size of each field in the tray

	Point dim; // dimension of the tray, *NOT pixels*
	Object[][] tray;
	
	void init(int x0, int y0, int scale) {
		this.pos = new Point(x0, y0);
		this.scale = scale;
	}
	
	Grid(int x0, int y0, int scale, String patternFile) {
		URL patternURL = Grid.class.getResource(patternFile);
	}

	Grid(int x0, int y0, int scale, int nj, int ni) {
		// for elephant, overwrite initial values:
		this.pos = new Point(x0, y0);
		this.scale = scale;
		this.dim = new Point(nj, ni); /* treat like (x,y) -- (width, height) */
		this.tray = new Object[nj][ni];

		// added by das:
		for (int i = 0; i < ni; i++)
			for (int j = 0; j < nj; j++) {
				this.tray[j][i] = INSIDE;
			}
		// cut out elements that are not part of the elephant:
		this.tray[0][6] = OUTSIDE;
		this.tray[0][7] = OUTSIDE;
		this.tray[0][8] = OUTSIDE;
		this.tray[1][4] = OUTSIDE;
		this.tray[1][5] = OUTSIDE;
		this.tray[1][6] = OUTSIDE;
		this.tray[1][7] = OUTSIDE;
		this.tray[1][8] = OUTSIDE;
		this.tray[2][4] = OUTSIDE;
		this.tray[2][5] = OUTSIDE;
		this.tray[2][6] = OUTSIDE;
		this.tray[2][7] = OUTSIDE;
		this.tray[2][8] = OUTSIDE;
		this.tray[3][0] = OUTSIDE;
		this.tray[4][0] = OUTSIDE;
		this.tray[4][1] = OUTSIDE;
		this.tray[5][0] = OUTSIDE;
		this.tray[5][1] = OUTSIDE;
		this.tray[5][7] = OUTSIDE;
		this.tray[5][8] = OUTSIDE;
		this.tray[6][0] = OUTSIDE;
		this.tray[6][1] = OUTSIDE;
		this.tray[6][7] = OUTSIDE;
		this.tray[6][8] = OUTSIDE;
		this.tray[7][0] = OUTSIDE;
		this.tray[7][1] = OUTSIDE;
		this.tray[7][7] = OUTSIDE;
		this.tray[7][8] = OUTSIDE;
		this.tray[8][0] = OUTSIDE;
		this.tray[8][1] = OUTSIDE;
		this.tray[8][7] = OUTSIDE;
		this.tray[8][8] = OUTSIDE;
		this.tray[9][0] = OUTSIDE;
		this.tray[9][1] = OUTSIDE;
		this.tray[9][2] = OUTSIDE;
		this.tray[10][0] = OUTSIDE;
		this.tray[10][1] = OUTSIDE;
		this.tray[10][2] = OUTSIDE;
		this.tray[10][3] = OUTSIDE;
	}

	public void draw(Graphics g) {
		for (int i = 0; i < dim.y; i++)
			for (int j = 0; j < dim.x; j++)
				// das:
				if (this.tray[j][i] != OUTSIDE)
					g.drawRect(pos.x + j * scale, pos.y + i * scale, scale,
							scale);
	}

	public Point index(Point p) {
		/* Returns array coordinate (as a Point) that p maps to in the Grid.
		 The upper-left point of each square in the Grid is the actual
		 reference point.  _p_ is "rounded" to the nearest reference point.
		 */
		Point q = new Point(p);
		q.add(this.scale / 2);
		q.sub(this.pos);
		q.div(this.scale);
		if (q.x >= 0 && q.x < dim.x && q.y >= 0 && q.y < dim.y)
			return q;
		else
			return null;
	}

	public Point map(Point p) {
		// Returns a Grid point, given a point in the neighborhood.
		Point q;
		q = index(p);
		if (q == null)
			return null;
		else {
			q.mul(this.scale);
			q.add(this.pos);
			return q;
		}
	}

	public Object getField(Point p) {
		// _p_ is a 2-d array index.
		if (p.x < 0 || p.x >= dim.x || p.y < 0 || p.y >= dim.y)
			return null;
		else
			return tray[p.x][p.y]; // caller must cast
	}

}
