package org.cocolab.deawu.pentomino;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

class Grid {
	
	static final Object OUTSIDE = "-";
	static final Object INSIDE = "x";

	private Point pos; // position of the grid in the graphic context
	private int scale; // size of each field in the tray

	Point dim; // dimension of the tray, *NOT pixels*
	Object[][] tray;
	
	void init(int x0, int y0, int scale, int nx, int ny) {
		this.pos = new Point(x0, y0);
		this.scale = scale;
		this.dim = new Point(nx, ny); /* treat like (x,y) -- (width, height) */
		this.tray = new Object[nx][ny];
	}
	
	Grid(int x0, int y0, int scale, String patternFile) {
		URL patternURL = Grid.class.getResource(patternFile);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(patternURL.openStream()));
			// the first line (hopefully) contains the dimensions of the tray
			String firstLine = reader.readLine();
			String[] lineTokens = firstLine.split("\\s+");
			int nx = new Integer(lineTokens[0]).intValue();
			int ny = new Integer(lineTokens[1]).intValue();
			// initialize the tray
			init(x0, y0, scale, nx, ny);
			for (int y = 0; y < ny; y++) {
				String line = reader.readLine();
				for (int x = 0; x < nx; x++) {
					if (Character.toString(line.charAt(x)).equals(INSIDE)) {
						tray[x][y] = INSIDE;
					} else {
						tray[x][y] = OUTSIDE;
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// draw an elephant if no pattern file is given
	Grid(int x0, int y0, int scale) {
		int nx = 11;
		int ny = 9;
		init(x0, y0, scale, nx, ny);
		
		// added by das:
		for (int i = 0; i < ny; i++)
			for (int j = 0; j < nx; j++) {
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
		g.setColor(Color.gray);
		for (int i = 0; i < dim.y; i++)
			for (int j = 0; j < dim.x; j++)
				// das:
				if (this.tray[j][i] != OUTSIDE)
					g.drawRect(pos.x + j * scale, pos.y + i * scale, scale, scale);
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
