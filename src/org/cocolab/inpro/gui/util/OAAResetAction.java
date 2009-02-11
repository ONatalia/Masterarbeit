package org.cocolab.inpro.gui.util;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.AgentImpl;

@SuppressWarnings("serial")
public class OAAResetAction extends OAAAction {

	public OAAResetAction(AgentImpl agent)
			throws RecognitionException, TokenStreamException {
		super(agent, "reset all agents", "reset");
	}

}
