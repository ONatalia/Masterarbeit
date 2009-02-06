package org.cocolab.deawu.util;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.AgentException;
import com.sri.oaa2.agentlib.AgentImpl;
import com.sri.oaa2.icl.IclList;
import com.sri.oaa2.icl.IclStr;
import com.sri.oaa2.icl.IclStruct;
import com.sri.oaa2.icl.IclTerm;

@SuppressWarnings("serial")
public class OAAAction extends AbstractAction {

	// globally set params for sending solve-requests  
	public static IclTerm params = new IclList(new IclStruct("reply", new IclStr("none")));

	AgentImpl agent;
	protected List<IclTerm> calls;
	
	public OAAAction(AgentImpl agent, String name, String goal) throws RecognitionException, TokenStreamException {
		super(name, null);
		this.agent = agent;
		calls = new LinkedList<IclTerm>();
		calls.add(IclTerm.fromString(goal));
	}
	
	public OAAAction(AgentImpl agent, String name, Icon icon, String goal) throws RecognitionException, TokenStreamException {
		super(name, icon);
		this.agent = agent;
		calls = new LinkedList<IclTerm>();
		calls.add(IclTerm.fromString(goal));
	}
	
	public OAAAction(AgentImpl agent, String name, Icon icon, String goal1, String goal2) throws RecognitionException, TokenStreamException {
		this(agent, name, icon, goal1);
		calls.add(IclTerm.fromString(goal2));
	}
	
	public void actionPerformed(ActionEvent arg0) {
		try {
			for (IclTerm call : calls) {
				if (agent != null) {
					System.err.println("solving call " + call.toString());
					agent.solve(call, params);
				} else {
					System.err.println("not solving " + call.toString() + " (no agent)");
				}
			}
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}
}
