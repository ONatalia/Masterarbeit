package inpro.gui.util;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.Agent;
import com.sri.oaa2.agentlib.AgentException;
import com.sri.oaa2.agentlib.AgentImpl;
import com.sri.oaa2.icl.IclList;
import com.sri.oaa2.icl.IclStr;
import com.sri.oaa2.icl.IclStruct;
import com.sri.oaa2.icl.IclTerm;

@SuppressWarnings("serial")
public class OAAAction extends AbstractAction {

	private static final Logger logger = Logger.getLogger(OAAAction.class);
	
	// globally set params for sending solve-requests  
	public static IclTerm params = new IclList(new IclStruct("reply", new IclStr("none")));

	Agent agent;
	protected List<IclTerm> calls;
	
	public OAAAction(Agent agent, String name, String goal) throws RecognitionException, TokenStreamException {
		this(agent, name, null, goal);
	}
	
	public OAAAction(Agent agent, String name, Icon icon, String goal) throws RecognitionException, TokenStreamException {
		super(name, icon);
		this.agent = agent;
		calls = new ArrayList<IclTerm>();
		calls.add(IclTerm.fromString(goal));
	}
	
	public void actionPerformed(ActionEvent arg0) {
		try {
			for (IclTerm call : calls) {
				if (agent != null) {
					logger.info("solving call " + call.toString());
					agent.solve(call, params);
				} else {
					logger.warn("not solving " + call.toString() + ": (no agent)");
				}
			}
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] ARGV) {
		try {
			PropertyConfigurator.configure("log4j.properties");
			OAAAction a = new OAAAction(null, "example", "dont(log)");
			a.actionPerformed(null);
			AgentImpl ag = new AgentImpl() {
				@Override
				public String getAgentName() {
					return "example";
				}
			};
			ag.facilitatorConnect(null);
			a = new OAAAction(ag, "example2", "do(log)");
			a.actionPerformed(null);
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}
}
