package org.cocolab.inpro.greifarm;

import javax.swing.JLabel;

/** 
 * a nice little highscore that counts down during game play
 * and increments when waste is dropped into the bowl 
 */
public class GameScore implements Runnable {

	int time = 0; // spent time over the course of the game
	int score = 0; // earned points over the course of the game
	int feelGoodAddition = 110; // avoids running into negative numbers right in the beginning
	JLabel scoreLabel = new JLabel("Punktestand: ");
	
	private synchronized void redrawLabel() {
		scoreLabel.setText("Punktestand: " + getCombinedScore());
		scoreLabel.repaint();
	}
	
	public int getCombinedScore() {
		return score - time + feelGoodAddition;
	}
	
	public JLabel getScoreLabel() {
		return scoreLabel;
	}
	
	public void reset() {
		score = 0;
		time = 0;
	}
	
	public void increaseScore(int amount) {
		score += amount;
		redrawLabel();
	}
	
	@Override
	public void run() {
		while (true) {
			time++;
			redrawLabel();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
