package demo.inpro.system.greifarm.gui;

import inpro.gui.util.OAAAction;
import inpro.gui.util.OAAResetAction;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.PropertyConfigurator;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.AgentException;
import com.sri.oaa2.agentlib.AgentImpl;

@SuppressWarnings("serial")
public class WozInterface extends JFrame { // NO_UCD (unused code): it has a main method and is in demo. good enough to be kept around

	AgentImpl agent;
	
	WozInterface() throws AgentException, RecognitionException, TokenStreamException {
		agent = new AgentImpl() {
			@Override
			public String getAgentName() {
				return "greifArmWI";
			}
			
		};
		agent.facilitatorConnect(null);
		setTitle("GreifArm Wizard Interface");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		
/*		add(new JTextField());
		add(new JButton(new OAAAction(agent, "nächste VP", "nextVP(name)")));
*/		add(new JButton(new OAAResetAction(agent)));
		
		JPanel directionPanel = new JPanel(new GridLayout(3, 2));		
		directionPanel.add(new JButton(new OAAMoveToAction("←←←", -3.0)));
		directionPanel.add(new JButton(new OAAMoveToAction("→→→", 3.0)));
		directionPanel.add(new JButton(new OAAMoveToAction("←←", -1.0)));
		directionPanel.add(new JButton(new OAAMoveToAction("→→", 1.0)));
		directionPanel.add(new JButton(new OAAMoveToAction("←", -0.33)));
		directionPanel.add(new JButton(new OAAMoveToAction("→", 0.33)));
		add(directionPanel);
		
		add(new JButton(new OAADropAction()));
		pack();
		setResizable(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	class OAAMoveToAction extends OAAAction {
		OAAMoveToAction(String name, double direction) throws RecognitionException, TokenStreamException {
			super(agent, name, "move(" + direction + ")");
		}
	}
	
	class OAADropAction extends OAAAction {
		OAADropAction() throws RecognitionException, TokenStreamException {
			super(agent, "drop", "drop");
		}
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		try {
			new WozInterface();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
