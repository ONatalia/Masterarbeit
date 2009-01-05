package org.cocolab.deawu.pentomino;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.cmu.sphinx.util.props.Resetable;

public abstract class Canvas extends JPanel implements ActionListener, Resetable {

	protected Tile[] tiles;
	int scale = 20;
	protected Tile draggingTile;
	protected Tile activeTile;
	Point clickOffset = new Point(0, 0);

	boolean paintLabels;
	
	protected abstract Tile[] createTiles();

	public Canvas() {
		setSize(10 * scale, 10 * scale);
		setMinimumSize(new Dimension(scale, scale));
		tiles = createTiles();
	}
	
	public void reset() {
		tiles = createTiles();
		draggingTile = null;
		activeTile = null;
		repaint();
	}

	public void paintTiles(Graphics g) {
		for (Tile tile : tiles) {
			tile.draw(g, paintLabels);
		}
		if (activeTile != null) {
			activeTile.draw(g, paintLabels);
		}
		if (draggingTile != null) {
			draggingTile.draw(g, paintLabels);
		} 
	}
	
	protected void superPaint(Graphics g) {
		super.paint(g);
	}
	
	public void paint(Graphics g) {
		superPaint(g);
		paintTiles(g);
	}

	/**
	 * do tile actions for actions performed on buttons
	 */
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (activeTile != null) {
			activeTile.doCommand(command);
		}
	}

	/**
	 * translate coordinates given in blocks into coordinates given in pixels
	 * @param t
	 * @return
	 */
	int translateBlockToPixel(double t) {
		return (int) (t * scale);
	}

	double translatePixelToBlock(int t) {
		return ((double) t) / ((double) scale);
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
