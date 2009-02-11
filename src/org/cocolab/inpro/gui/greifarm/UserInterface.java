package org.cocolab.inpro.gui.greifarm;

import java.util.Random;

import javax.swing.JFrame;

import com.sri.oaa2.agentlib.AgentException;
import com.sri.oaa2.agentlib.AgentImpl;
import com.sri.oaa2.icl.IclFloat;
import com.sri.oaa2.icl.IclList;
import com.sri.oaa2.icl.IclTerm;

public class UserInterface extends AgentImpl {

	Random rand;
	
	GreifArm greifarm;
	
	double greifarmPosition;
	
	UserInterface() throws AgentException {
		facilitatorConnect(null);
		start();
		JFrame f = new JFrame("Greifarm");
		greifarm = new GreifArm();
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
			greifarmPosition = GreifArm.translatePixelToBlock(greifarm.cursorPosition.x);
		} else if (str.equals("move")) {
			double direction = ((IclFloat) goal.getTerm(0)).toDouble();
			greifarmPosition += (rand.nextGaussian() + 2) * direction;
			greifarmPosition = Math.min(Math.max(greifarmPosition, 0.5), GreifArm.RELATIVE_WIDTH - 0.5); 
			System.err.println("moving to new position: " + greifarmPosition);
			greifarm.cursorMoveSlowlyToRel(greifarmPosition, 1);
		} else if (str.equals("drop")) {
			greifarm.cursorVisible = false;
			greifarm.repaint();
			greifarm.cursorMoveSlowlyTo(greifarm.cursorPosition.x, (GreifArm.RELATIVE_HEIGHT - 1) * GreifArm.SCALE);

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
