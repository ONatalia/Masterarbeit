package org.cocolab.deawu.pentomino;

import java.awt.Graphics;
import java.awt.Color;
import java.lang.String;

class PentoTile {
	
	static final String CCW_ROTATE_COMMAND = "ccwRotate";
	static final String CW_ROTATE_COMMAND = "cwRotate";
	static final String HORIZONTAL_FLIP_COMMAND = "hFlip";
	static final String VERTICAL_FLIP_COMMAND = "vFlip";
	

   private static final int BOX_COUNT = 5; // number of boxes making up the piece
   private Box[]   boxes = new Box[BOX_COUNT];
   private int[][] defVectors = new int[2][BOX_COUNT];
   private int     scale;
      Point  refPoint, defaultRefPoint;   // defaultRefPoint can be used for resetting
      Color   color, defaultColor;         // defaultColor    can be used for resetting
      Point  clipCorner;
      Point  clipDim;
      Point  clipOffset;                  // (refPoint - clipCorner)
      boolean placed;
      String name;

   PentoTile(int s, Color c, int x0, int y0,
         int i1, int j1, int i2, int j2, int i3, int j3, int i4, int j4, String name) {
      
      this.defVectors[0][0] = 0;
      this.defVectors[1][0] = 0;
      this.defVectors[0][1] = i1;
      this.defVectors[1][1] = j1;
      this.defVectors[0][2] = i2;
      this.defVectors[1][2] = j2;
      this.defVectors[0][3] = i3;
      this.defVectors[1][3] = j3;
      this.defVectors[0][4] = i4;
      this.defVectors[1][4] = j4;
      this.color = c;
      this.refPoint = new Point(x0, y0);
      this.scale = s;
      this.name = name;
      
      this.placed = false;
      this.defaultRefPoint = this.refPoint;
      this.defaultColor = this.color;
      this.clipCorner = new Point(this.refPoint);
      this.clipOffset = new Point(this.refPoint);

      generate();  /* creates indiv. boxes and determines clip rectangle */
   }
   

   private void generate() {
      Point p = new Point(0, 0);
      Point clipLRCorner =
            new Point(this.refPoint);
      clipLRCorner.add(this.scale);

      this.clipCorner.copy(this.refPoint);
      for (int i=0; i<BOX_COUNT; i++) {
         p.copy(this.refPoint);
         p.add(new Point(defVectors[0][i] *this.scale,
                                              defVectors[1][i] *this.scale));
         boxes[i] = new Box(p, this.scale, color); //generates GARBAGE
         this.clipCorner.min(p);
         p.add(this.scale);
         clipLRCorner.max(p);
      }
      this.clipDim = (Point) clipLRCorner.clone();
      this.clipDim.sub(clipCorner);
      
      //upper-left clip corner can vary relative to refPoint due to transformations
      this.clipOffset.copy(this.refPoint);
      this.clipOffset.sub(this.clipCorner);
   }

   
   public boolean matchesPosition(Point p) {
      boolean s;
      s = (boxes[0]).matchesPosition(p) || 
          (boxes[1]).matchesPosition(p) ||
          (boxes[2]).matchesPosition(p) || 
          (boxes[3]).matchesPosition(p) || 
          (boxes[4]).matchesPosition(p);
      return s;
   }
   
   public void setPos(Point p) {
      Point delta = (Point) p.clone();
      delta.sub(refPoint);                  /* get change in Box coordinates */
      refPoint = (Point) p.clone();        /* change refPoint to new coords */
      for (int i = 0; i<BOX_COUNT; i++) {
         (boxes[i]).corner.add(delta);
      }
      clipCorner.add(delta);
   }

   public Color getColor() {
      return (boxes[0]).getColor();
   }

   public void setColor(Color c) {
      this.color = c;
      for (int i=0; i<BOX_COUNT; i++) {
         (boxes[i]).setColor(c);
      }
   }

   public void draw(Graphics g) {
	  draw(g, false); // do not print labels if no parameter was given
   }

   /* Alternative draw method.  Takes a boolean as additional parameter
    * to decide whether to draw the Tile's label.
    * */
   public void draw(Graphics g, boolean l) {
	  int tx = boxes[0].corner.x;
	  int ty = boxes[0].corner.y;
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
          g.drawString(this.name, tx + 5, ty + 15);    	  
      }
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
			throw new RuntimeException("Illegal command for tile manipulation");

   }

   public void hFlip() {
	  System.out.println("hFlip: " + this.defaultRefPoint);
      for (int i = 1; i<BOX_COUNT; i++) {
         this.defVectors[1][i] = -1 * this.defVectors[1][i];
      }
      this.generate();
   }

   public void vFlip() {
	  System.out.println("vFlip: " + this.defaultRefPoint);
      for (int i = 1; i<BOX_COUNT; i++) {
         this.defVectors[0][i] = -1 * this.defVectors[0][i];
      }
      this.generate();
   }

   public void cwRotate() {
      int temp;
      System.out.println("cwRotate: " + this.defaultRefPoint);
      for (int i=1; i<BOX_COUNT; i++) {
         temp = this.defVectors[0][i];
         this.defVectors[0][i] = -1 * this.defVectors[1][i];
         this.defVectors[1][i] = temp;
      }
      this.generate();
   }

   public void ccwRotate() {
      int temp;
      System.out.println("ccwRotate: " + this.defaultRefPoint);
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
               (tray[mi][mj] != Grid.INSIDE && tray[mi][mj] != null))
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
                     if (tray[mi+k][mj+L] == Grid.INSIDE 
                     && tray[mi+k][mj+L] == Grid.OUTSIDE
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
      placed = true;
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
      placed = false;
   }
   
   public String toString() {
	   return "tile: " + this.name + "\n";
   }

}
