package org.cocolab.inpro.gui.pentomino;

import java.awt.Graphics;
import java.awt.Color;
import java.lang.Cloneable;

import org.cocolab.inpro.gui.Point;

/**
 * five of these make a pentomino tile, four for tetris.
 * @author whoever implemented the original pentomino code
 * @author timo: some adaptations/deprovements over the years
 */
public class Box implements Cloneable {

   /** a box's dimensions never change, hence final */
   final Point dim;
   Point corner;
   private Color color;

   Box(Point p, int s, Color c) {
      corner = (Point) p.clone();
      dim = new Point(s, s);
      color = c;
   }
   
   Box(int x, int y, int dx, int dy, Color c) {
      corner = new Point(x, y);
      dim = new Point(dx, dy);
      color = c;
   }
    
	public void drawSelection(Graphics g) {
		g.setColor(Color.ORANGE);
		int borderWidth = 5;
		g.fillRect(corner.x - borderWidth, corner.y - borderWidth, dim.x + 1 + 2 * borderWidth, dim.y + 1 + 2 * borderWidth);
//		g.setColor(Color.yellow);
//		g.drawRect(corner.x - 1, corner.y - 1, dim.x + 2, dim.y + 2);
	}

   public void draw(Graphics g) {
      g.setColor(color.brighter());
      g.fillRect(corner.x, corner.y, dim.x, dim.y);
   }
   
   public void draw(Graphics g, boolean left, boolean right, boolean top, boolean bottom) {
	   draw(g);
	   Color borderColor = Color.black;
	   Color nonBorderColor = color.darker();
	   g.setColor(left ? borderColor : nonBorderColor);
	   if (left) 
		   g.drawLine(corner.x, corner.y, corner.x, corner.y + dim.y);
	   else 
		   g.drawLine(corner.x, corner.y + 1, corner.x, corner.y + dim.y - 1);
	   g.setColor(right ? borderColor : nonBorderColor);
	   if (right)
		   g.drawLine(corner.x + dim.x , corner.y, corner.x + dim.x, corner.y + dim.y);
	   else 
		   g.drawLine(corner.x + dim.x , corner.y + 1, corner.x + dim.x, corner.y + dim.y - 1);
	   g.setColor(top ? borderColor : nonBorderColor);
	   if (top) 
		   g.drawLine(corner.x, corner.y, corner.x + dim.x, corner.y);
	   else
		   g.drawLine(corner.x + 1, corner.y, corner.x + dim.x - 1, corner.y);
	   g.setColor(bottom ? borderColor : nonBorderColor);
	   if (bottom)
		   g.drawLine(corner.x, corner.y + dim.y, corner.x + dim.x, corner.y + dim.y);
	   else
		   g.drawLine(corner.x + 1, corner.y + dim.y, corner.x + dim.x - 1, corner.y + dim.y);
   }
   
   public boolean matchesPosition(java.awt.Point p) {
      if (p.x >= this.corner.x && p.x <= this.corner.x + this.dim.x - 1 &&
          p.y >= this.corner.y && p.y <= this.corner.y + this.dim.y - 1)
      {
         return true;
      } else {
         return false;
      }
   }

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color c) {
      this.color = c;
   }

   public void setPos(Point p) {
      this.corner = (Point) p.clone();
   }

   public void setPos(int x, int y) {
      this.corner.x = x;
      this.corner.y = y;
   }

}
