package inpro.gui.greifarm;

import java.util.Random;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import com.sri.oaa2.agentlib.AgentException;
import com.sri.oaa2.agentlib.AgentImpl;
import com.sri.oaa2.icl.IclFloat;
import com.sri.oaa2.icl.IclList;
import com.sri.oaa2.icl.IclTerm;

public class UserInterface extends AgentImpl {

	private static final Logger logger = Logger.getLogger(UserInterface.class);
		
	Random rand;
	GreifArmGUI greifarm;
	double greifarmPosition;
	
	UserInterface() throws AgentException {
		facilitatorConnect(null);
		start();
		JFrame f = new JFrame("Greifarm");
		greifarm = new GreifArmGUI();
		greifarmPosition = GreifArmGUI.translatePixelToBlock(greifarm.cursorPosition.x);
		f.add(greifarm);
		f.pack();
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		rand = new Random();
	}
	
	@Override
	public String getAgentName() {
		return "greifArmUI";
	}

	@Override
	public String getAgentCapabilities() {
		return "[move(X),drop,reset]";
		// X : double
	}

	@Override
	public boolean oaaDoEventCallback(IclTerm goal, IclList params, IclList answers) {
		boolean result = false;
		String str = goal.toIdentifyingString();
		if (str.equals("reset")) {
			greifarm.reset();
			greifarmPosition = GreifArmGUI.translatePixelToBlock(greifarm.cursorPosition.x);
		} else if (str.equals("move")) {
			if (greifarm.cursorVisible) {
				double direction = ((IclFloat) goal.getTerm(0)).toDouble();
				greifarmPosition += (rand.nextGaussian() + 2) * direction;
				greifarmPosition = Math.min(Math.max(greifarmPosition, 0.5), GreifArmGUI.RELATIVE_WIDTH - 0.5); 
				logger.info("moving. requested direction is " + direction);
				logger.info("new position will be " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
//				System.err.println("moving to new position: " + greifarmPosition);
				greifarm.cursorMoveSlowlyToRel(greifarmPosition, 1);
			}
		} else if (str.equals("drop")) {
			logger.info("dropping at " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
			greifarm.cursorVisible = false;
			greifarm.repaint();
			greifarm.emptyHand.setPos(greifarm.cursorPosition);
			greifarm.emptyHand.setVisible(true);
			greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x, (GreifArmGUI.RELATIVE_HEIGHT - 1) * GreifArmGUI.SCALE);
		} else if (str.equals("stop")) {
			logger.info("stopping at " + GreifArmGUI.translateBlockToPixel(greifarmPosition));
			greifarm.cursorMoveTo(greifarm.cursorPosition.x, greifarm.cursorPosition.y);
		}
		return result;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new UserInterface();
		} catch (AgentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

}
