package org.cocolab.deawu.greifarm;

import java.awt.Dimension;
import java.awt.Graphics;
import java.net.MalformedURLException;

import javax.swing.JFrame;

import org.cocolab.deawu.CursorCanvas;
import org.cocolab.deawu.ImageTile;
import org.cocolab.deawu.Point;
import org.cocolab.deawu.Tile;

@SuppressWarnings("serial")
public class GreifArm extends CursorCanvas {

	public GreifArm() {
		super();
		cursorVisible = false;
		buttonClickDelay = 100;
		labelsVisible = false;
		setPreferredSize(new Dimension(30 * SCALE, 10 * SCALE));
		cursorPosition = new Point(4 * SCALE, 1 * SCALE);
	}
	
	@Override
	protected Tile[] createTiles() {
		Tile[] tiles;
		tiles = new Tile[2];
		try {
			tiles[0] = new ImageTile("file:///home/timo/inpro/inpro/res/ball.png");
			tiles[1] = new ImageTile("file:///home/timo/inpro/inpro/res/bowl.png");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		tiles[0].setPos(new Point(4 * SCALE, 1 * SCALE));
		tiles[1].setPos(new Point(14 * SCALE, 8 * SCALE));
		return tiles;
	}
	
	@Override
	public void paint(Graphics g) {
		superPaint(g);
		paintTopTiles(g);
		paintTiles(g);
		paintCursor(g);
	}
	
	public static void main(String[] args) {
		JFrame f = new JFrame("Greifarm");
		GreifArm greifarm = new GreifArm();
		greifarm.cursorVisible = true;
		f.add(greifarm);
		f.pack();
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		try {
		Thread.sleep(100);
		greifarm.cursorPress();
		Thread.sleep(100);
		greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x + 10 * SCALE, greifarm.cursorPosition.y);
		Thread.sleep(800);
		greifarm.cursorVisible = false;
		greifarm.repaint();
		Thread.sleep(200);
		greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x, greifarm.cursorPosition.y + 7 * SCALE - 3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}
