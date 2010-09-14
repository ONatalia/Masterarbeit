package org.cocolab.inpro.domains.greifarm;

import java.util.Random;

import javax.swing.JComponent;

import org.apache.log4j.Logger;
import org.cocolab.inpro.gui.greifarm.GreifArmGUI;

/** this encapsulates greifarmController control and only exposes the task-dependent actions */
public class GreifarmController {
	private final Logger logger = Logger.getLogger(GreifarmController.class);
	
	public DropListener dropListener;

	private GreifArmGUI greifarmGUI = new GreifArmGUI(7);
	private Random rand = new Random();
	private double greifarmPosition;
	/** this is set to false by drop() and set to true by the reset command */
	private boolean hasControl;
	
	private GameScore gamescore;
	
	GreifarmController(GameScore gamescore) {
		this.gamescore = gamescore;
		ActionIU.greifarm = this;
	}
	
	public JComponent getVisual() {
		return greifarmGUI;
	}
	
	public void reset() {
		greifarmGUI.reset();
		greifarmPosition = GreifArmGUI.translatePixelToBlock(greifarmGUI.cursorPosition.x);
		hasControl = true;		
	}
	
	public void drop() {
		if (hasControl) { // no need to act if we're already dropping
			hasControl = false;
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					logger.info("dropping at " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
					greifarmGUI.cursorVisible = false;
				//	greifarmGUI.repaint();
					greifarmGUI.emptyHand.setPos(greifarmGUI.cursorPosition);
					greifarmGUI.emptyHand.setVisible(true);
					greifarmGUI.cursorMoveSlowlyToAndWait(greifarmGUI.cursorPosition.x, (GreifArmGUI.RELATIVE_HEIGHT - 1) * GreifArmGUI.SCALE);
					// calculate score: if the ball lands within the bowl, score should be increased, otherwise decreased by a fixed amount
					int distance = Math.abs(greifarmGUI.getBowlPosition() - greifarmGUI.cursorPosition.x);
					int score;
					if (distance < 3) 
						score = 100;
					else if (distance > 25) 
						score = -100;
					else 
						score = 100 - (4 * distance);
					gamescore.increaseScore(score);
					if (dropListener != null) {
						dropListener.notifyDrop(gamescore);
					}					
				}
			}).start();
		}
	}

	public void stop() {
		greifarmGUI.cursorMoveSlowlyTo(greifarmGUI.cursorPosition.x, greifarmGUI.cursorPosition.y);
		greifarmPosition = GreifArmGUI.translatePixelToBlock(greifarmGUI.cursorPosition.x);
		logger.info("stopping at " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
		logger.warn("distance between arm and bowl is: " + getDistanceToGoal());
	}
	
	protected int getDistanceToGoal() {
		return greifarmGUI.cursorPosition.x - greifarmGUI.getBowlPosition();	
	}
	
	public double getCurrentPosition() {
		return greifarmPosition; // this is the same as calling getGoalPosition(0), but nevertheless
	}

	public double getGoalPositionFor(double amount) {
		if (hasControl) {
			double newPosition = greifarmPosition + (rand.nextGaussian() *.1 + 1) * amount;
			newPosition = Math.min(Math.max(newPosition, 0.5), GreifArmGUI.RELATIVE_WIDTH - 0.5);
			return newPosition; 
		}
		return greifarmPosition;
	}
	
	public void moveTo(double position) {
		if (hasControl) {
			greifarmPosition = position;
			// assert that we don't leave the canvas
			greifarmPosition = Math.min(Math.max(greifarmPosition, 0.5), GreifArmGUI.RELATIVE_WIDTH - 0.5); 
			logger.info("new position will be " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
			greifarmGUI.cursorMoveSlowlyToRel(greifarmPosition, 1);
		}
	}

}

