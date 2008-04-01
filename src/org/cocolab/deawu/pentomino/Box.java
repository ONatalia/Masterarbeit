package org.cocolab.deawu.pentomino;

import java.awt.Graphics;
import java.awt.Color;
import java.lang.Cloneable;

public class Box implements Cloneable {

   Point dim;
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

   public void draw(Graphics g) {
      g.setColor(color);
      g.fillRect(corner.x, corner.y, dim.x, dim.y);
      g.setColor(Color.black);
      g.drawRect(corner.x, corner.y, dim.x, dim.y);
   }

   public boolean selected(Point p) {
      if (p.x >= this.corner.x && p.x <= this.corner.x +this.dim.x-1 &&
            p.y >= this.corner.y && p.y <= this.corner.y +this.dim.y -1) {
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
