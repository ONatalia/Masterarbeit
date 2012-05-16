package demo.inpro.system.greifarm.gui;

import inpro.gui.CursorCanvas;
import inpro.gui.ImageTile;
import inpro.gui.Point;
import inpro.gui.Tile;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class GreifArmGUI extends CursorCanvas {

	private static final Logger logger = Logger.getLogger(GreifArmGUI.class);
	
	public static final int RELATIVE_WIDTH = 50;
	public static final int RELATIVE_HEIGHT = 10;
	
	public ImageTile emptyHand;
	
	Random randomSource = new Random();
	
	public GreifArmGUI() {
		this(5);
	}
	
	public GreifArmGUI(double moveSpeed) {
		super();
		MOVE_SPEED = moveSpeed;
		reset();
		cursorVisible = true;
		buttonClickDelay = 100;
		labelsVisible = false;
		setPreferredSize(new Dimension(RELATIVE_WIDTH * SCALE, RELATIVE_HEIGHT * SCALE));
	}
	
	@Override
	protected List<Tile> createTiles() {
		ArrayList<Tile> tiles = new ArrayList<Tile>(3);
		tiles.add(new ImageTile(GreifArmGUI.class.getResource("ball.png")));
		tiles.add(new ImageTile(GreifArmGUI.class.getResource("bowl.png")));
		emptyHand = new ImageTile(CursorCanvas.class.getResource("draggable.png"));
		emptyHand.setVisible(false);
		tiles.add(emptyHand);
		return tiles;
	}
	
	public int getBowlPosition() {
		return tiles.get(1).refPoint.x;
	}
	
	@Override
	public void paint(Graphics g) {
		superPaint(g);
		paintTopTiles(g);
		paintTiles(g);
		paintCursor(g);
	}
	
	public void regeneratePositions() {
		// reset to a new position
		double ballX = randomSource.nextDouble() * (RELATIVE_WIDTH - 2) + 1;
		setBall(ballX);
		double bowlX = randomSource.nextDouble() * (RELATIVE_WIDTH - 2) + 1;
		setBowl(bowlX);
		repaint();		
	}
	
	public void regenerateWithGreatDistance() {
		boolean bowlLeft = randomSource.nextBoolean();
		double minDistance = 20;
		double distLeftAndRight = (RELATIVE_WIDTH - minDistance) / 2;
		double bowlPos = randomSource.nextDouble() * distLeftAndRight + 4;
		double ballPos = RELATIVE_WIDTH - randomSource.nextDouble() * distLeftAndRight - 4;
		if (!bowlLeft) {
			double swap;
			swap = bowlPos;
			bowlPos = ballPos;
			ballPos = swap;
		}
		setBowl(bowlPos);
		setBall(ballPos);
	}
	
	private void setBowl(double pos) {
		tiles.get(1).setPos(new Point((int) (pos * SCALE), (int) ((RELATIVE_HEIGHT - 1.5) * SCALE)));
		logger.info("bowl position is now " + (int) (pos * SCALE));		
	}
	
	private void setBall(double pos) {
		tiles.get(0).setPos(new Point((int) (pos * SCALE), 1 * SCALE));
		cursorPosition = new Point((int) (pos * SCALE), 1 * SCALE);
		grabbing = true;
		cursorVisible = true;
		cursorPressAt((int) (pos * SCALE), 1 * SCALE);
		logger.info("greifarm position is now " + cursorPosition.x);		
	}

	@Override
	public void reset() {
		logger.info("being reset");
		super.reset();
		//regeneratePositions();
		regenerateWithGreatDistance();
	}
	
	public static void main(String[] args) {
		JFrame f = new JFrame("Greifarm");
		GreifArmGUI greifarm = new GreifArmGUI();
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
