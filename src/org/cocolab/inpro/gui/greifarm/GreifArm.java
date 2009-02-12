package org.cocolab.inpro.gui.greifarm;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;

import org.cocolab.inpro.gui.CursorCanvas;
import org.cocolab.inpro.gui.ImageTile;
import org.cocolab.inpro.gui.Point;
import org.cocolab.inpro.gui.Tile;

@SuppressWarnings("serial")
public class GreifArm extends CursorCanvas {

	public static final int RELATIVE_WIDTH = 40;
	public static final int RELATIVE_HEIGHT = 10;
	
	ImageTile emptyHand;
	
	public GreifArm() {
		super();
		reset();
		cursorVisible = true;
		buttonClickDelay = 100;
		labelsVisible = false;
		setPreferredSize(new Dimension(RELATIVE_WIDTH * SCALE, RELATIVE_HEIGHT * SCALE));
	}
	
	@Override
	protected Tile[] createTiles() {
		Tile[] tiles;
		tiles = new Tile[3];
		tiles[0] = new ImageTile(GreifArm.class.getResource("ball.png"));
		tiles[1] = new ImageTile(GreifArm.class.getResource("bowl.png"));
		emptyHand = new ImageTile(CursorCanvas.class.getResource("draggable.png"));
		emptyHand.setVisible(false);
		tiles[2] = emptyHand;
		return tiles;
	}
	
	@Override
	public void paint(Graphics g) {
		superPaint(g);
		paintTopTiles(g);
		paintTiles(g);
		paintCursor(g);
	}
	
	@Override
	public void reset() {
		super.reset();
		// reset to a new position
		double ballX = Math.random() * (RELATIVE_WIDTH - 2) + 1;
		tiles[0].setPos(new Point((int) (ballX * SCALE), 1 * SCALE));
		cursorPosition = new Point((int) (ballX * SCALE), 1 * SCALE);
		grabbing = true;
		cursorVisible = true;
		cursorPressAt((int) (ballX * SCALE), 1 * SCALE);
		double bowlX = Math.random() * (RELATIVE_WIDTH - 2) + 1;
		tiles[1].setPos(new Point((int) (bowlX * SCALE), (int) ((RELATIVE_HEIGHT - 1.5) * SCALE)));
	}
	
	public static void main(String[] args) {
		JFrame f = new JFrame("Greifarm");
		GreifArm greifarm = new GreifArm();
		f.add(greifarm);
		f.pack();
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		try {
//		Thread.sleep(100);
//		greifarm.cursorPress();
		Thread.sleep(400);
		greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x + 10 * SCALE, greifarm.cursorPosition.y);
		Thread.sleep(800);
		greifarm.cursorVisible = false;
		greifarm.repaint();
		Thread.sleep(200);
		greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x, (RELATIVE_HEIGHT - 1) * SCALE - 3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
