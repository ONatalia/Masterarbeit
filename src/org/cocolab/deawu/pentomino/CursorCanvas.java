package org.cocolab.deawu.pentomino;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;

/**
 * PentoCursorCanvas, extends Canvas with a hand cursor implementation
 * 
 * a cursor is placed on the board and can be controlled via mouse-like operations:
 * the cursor can be pressed, released or the state can be toggled and it can be
 * moved around, either immediately or following a path from the current position
 * operations are also available using relative block units
 * 
 * actions with the cursor will trigger the corresponding operations of PentoCanvas 
 * to reflect the changes of the hand cursor on the pentomino board
 * also, this class will trigger repaint() when appropriate
 * 
 * subclasses may want to hide the cursor by setting cursorVisible to false
 * 
 * @author timo
 */

@SuppressWarnings("serial")
abstract public class CursorCanvas extends Canvas {

	Image cursorGrab;
	Image cursorFree;
	boolean grabbing;
	
	Point cursorPosition = new Point(0, 0);
	Point imageCenter;
	boolean cursorVisible;
	
	int buttonClickDelay = 400; // in milliseconds

	public CursorCanvas() {
		this(false);
	}
	
	public CursorCanvas(boolean cursorVisible) {
		super();
		try {
			cursorGrab = ImageIO.read(CursorCanvas.class.getResourceAsStream("dragging.png"));
			cursorFree = ImageIO.read(CursorCanvas.class.getResourceAsStream("draggable.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		imageCenter = new Point(cursorGrab.getWidth(null) / 2, cursorGrab.getHeight(null) / 2);
		this.cursorVisible = cursorVisible;
	}
	
	/* is also called during initialization */
	public void reset() {
		grabbing = false;
		cursorPosition = new Point(getWidth() / 2, getHeight() / 2);
		super.reset();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (cursorVisible) {
			Image cursor = grabbing ? cursorGrab : cursorFree;
			Point imagePosition = (Point) cursorPosition.clone();
			imagePosition.sub(imageCenter);
			g.drawImage(cursor, imagePosition.x, imagePosition.y, null);
		}
		/* TODO (zuallerst: diesen Kommentar in die passende Klasse verschieben...
		 * TODO:
		 * Ein Spielscore, der beschreibt, wie gut das Spiel gelöst wird
		 * 
		 * zum einen erlaubt dies, die Dialogeffizienz zu messen, zum anderen
		 * bringt es dem Spieler auch mehr Spaß, wenn er auf ein Ziel hinspielen 
		 * kann.
		 * 
		 * Der Score muss vor Beginn des Spiels erwähnt und erläutert 
		 * ("Es kann für das SDS also günstiger sein, nachzufragen, anstatt 
		 * eine Lösung einfach auszuprobieren" - "Zu schnelles Sprechen
		 * erhöht natürlich die Gefahr von Missverständnissen und Hörfehlern")
		 * 
		 * Der Spielscore kann berechnet werden aus:
		 * - Anzahl der Klicks,
		 * - Mauskilometerzähler (am besten in relativen Units, nicht in Pixeln)
		 * - Zeit
		 * - theoretisch auch aus Anzahl der Rückfragen ("Das habe ich leider nicht verstanden"), 
		 *   der Dialog bleibt ansonsten aber besser vom Scoring ausgenommen
		 *   
		 * Die Darstellung des Scores muss natürlich inkrementell sein und
		 * damit der Score motiviert, muss es auch möglich sein, trotz Rückschlägen
		 * wieder "besser" zu werden, gerade damit keine Panik/Resignation ausbricht.
		 * 
		 * Scoring-Idee (relativ komplex umzusetzen):
		 * 
		 * Spielzwischenstände nach der Ablage jedes Teils mit vorgegebenen
		 * Zielwerten vergleichen und anzeigen ob besser/schlechter.
		 * 
		 * Zielwerte zum Beispiel: Durchschnittsleistung, bestmögliche Leistung,
		 * (100*bestmöglich)/Realwert (steigt für gute, sinkt für schlechte Leistung, niemals null 
		 * 
		 */
		//String str = (Math.random() < 0.5) ? "Score: none" : "Score: great";
		//final int offset = 5; 
		//g.drawString(str, offset, getHeight() - offset);
	}
	
	public void cursorToggle() {
		cursorToggleAt(cursorPosition.x, cursorPosition.y);
	}
	
	public void cursorToggleAtRel(double x, double y) {
		cursorToggleAt(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	public void cursorToggleAt(int x, int y) {
		if (grabbing) {
			cursorReleaseAt(x, y);
		} else {
			cursorPressAt(x, y);
		}
	}
	
	public void cursorPress() {
		cursorPressAt(cursorPosition.x, cursorPosition.y);
	}
	
	public void cursorPressAtRel(double x, double y) {
		cursorPressAt(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	public void cursorPressAt(int x, int y) {
		cursorPosition = new Point(x, y);
		grabbing = tileSelect(x, y);
		repaint();
	}

	public void cursorRelease() {
		cursorReleaseAt(cursorPosition.x, cursorPosition.y);
	}
	
	public void cursorReleaseAtRel(double x, double y) {
		cursorReleaseAt(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	public void cursorReleaseAt(int x, int y) {
		cursorPosition = new Point(x, y);
		grabbing = false;
		tileRelease(x, y);
		repaint();
	}
	
	private void cursorMoved() {
		if (grabbing) {
			tileMove(cursorPosition);
		}
		repaint();
	}
	
	public void cursorMoveToRel(double x, double y) {
		cursorMoveTo(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	public void cursorMoveTo(int x, int y) {
		cursorPosition = new Point(x, y);
		cursorMoved();
	}

	public void cursorMoveSlowlyToRel(double x, double y) {
		cursorMoveSlowlyTo(translateBlockToPixel(x), translateBlockToPixel(y));
	}
	
	public void cursorMoveSlowlyTo(int x, int y) {
		int distX = x - cursorPosition.x;
		int distY = y - cursorPosition.y;
		double dist = Math.sqrt(distX * distX + distY * distY);
		double deltaX = distX / dist;
		double deltaY = distY / dist;
		double currentX = cursorPosition.x;
		double currentY = cursorPosition.y;
		int speed = 2;
		for (int i = 0; i <= dist; i += speed) {
			currentX += deltaX * speed;
			currentY += deltaY * speed;
			cursorMoveTo((int) currentX, (int) currentY);
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		cursorMoveTo(x, y);
	}
	
	boolean tileSelect(int x, int y) {
		boolean selectionSuccessful = false;
		// deal with buttons
		Component selectedComponent = getComponentAt(x, y);
		if (selectedComponent != this) {
			if (selectedComponent instanceof JButton) {
				((JButton) selectedComponent).doClick(buttonClickDelay);
			}
		} else {
			// otherwise deal with tiles
			selectionSuccessful = super.tileSelect(x, y);
		}
		return selectionSuccessful;
	}

	void addButton(String label, String command, int num) {
		JButton b = new JButton(label);
		b.setActionCommand(command);
		b.addActionListener(this);
		b.setFocusPainted(false);
		int xpos = num * 7 + 2;
		int width = scale * 5;
		b.setBounds(xpos * scale, scale, width, scale * 2);
		add(b);
		for (MouseListener ml : b.getMouseListeners()) {
			b.removeMouseListener(ml);
		}	
		for (MouseMotionListener mml :b.getMouseMotionListeners()) {
			b.removeMouseMotionListener(mml);
		}
	}

}
