package org.cocolab.deawu;

import java.awt.Graphics;
import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;


public class ImageTile extends Tile {

	Image image;
	Point size;
	
	public ImageTile(String s) throws MalformedURLException {
		this(new URL(s));
	}
	
	public ImageTile(URL url) {
		ImageIcon img = new ImageIcon(url);
		this.image = img.getImage();
		size = new Point(image.getWidth(null), image.getHeight(null));
	}
	
	@Override
	public void draw(Graphics g, boolean l) {
		g.drawImage(image, refPoint.x - size.x / 2, refPoint.y - size.y / 2, null, null);
	}

	@Override
	public boolean matchesPosition(Point p) {
		boolean s;
		s = ((refPoint.x - size.x / 2 < p.x) && (p.x < (refPoint.x + size.x / 2))
		&& (refPoint.y - size.y / 2 < p.y) && (p.y < (refPoint.y + size.y + size.y / 2)));
		return s;
	}

	@Override
	public void unplace() {
		// TODO Auto-generated method stub

	}

}
