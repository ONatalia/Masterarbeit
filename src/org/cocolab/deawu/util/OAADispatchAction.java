package org.cocolab.deawu.util;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.AgentImpl;

@SuppressWarnings("serial")
public class OAADispatchAction extends OAAAction {

	public static String DISPATCH_GOAL_PREFIX = "x";
	
	public OAADispatchAction(AgentImpl agent, String name, String filename) throws RecognitionException, TokenStreamException {
		super(agent, name, null, DISPATCH_GOAL_PREFIX + "PlayFile('" + filename + "')");
	}
}
