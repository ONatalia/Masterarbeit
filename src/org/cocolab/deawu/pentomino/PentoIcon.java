package org.cocolab.deawu.pentomino;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.cocolab.deawu.Point;

/*
 * encapsulate PentoTile so that it can be used as the icon on a button
 */

public class PentoIcon extends PentoTile implements Icon {

	public PentoIcon(int s, char name) {
		super(s, Color.RED, 0, 0, name);
	}
	
	public PentoIcon(int s, Color c, char name) {
		super(s, c, 0, 0, name);
	}

	public int getIconHeight() {
		return scale * 5;
	}

	public int getIconWidth() {
		// TODO Auto-generated method stub
		return scale * 5;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		setPos(new Point(x + 2 * scale, y + 2 * scale));
		draw(g, false);
	}
	
	public static JButton newButtonFor(char c) {
		PentoIcon pi = new PentoIcon(10, c);
		return new JButton(Character.toString(c), pi);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f = new JFrame("PentoIcon Test");

		f.setLayout(new GridLayout(2, 6));
		
		f.add(newButtonFor('F'));
		f.add(newButtonFor('U'));
		f.add(newButtonFor('X'));
		f.add(newButtonFor('W'));
		f.add(newButtonFor('Y'));
		f.add(newButtonFor('N'));
		f.add(newButtonFor('P'));
		f.add(newButtonFor('V'));
		f.add(newButtonFor('Z'));
		f.add(newButtonFor('T'));
		f.add(newButtonFor('I'));
		f.add(newButtonFor('L'));
		
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);

	}

}
