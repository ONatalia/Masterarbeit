package inpro.gui.util;

import antlr_oaa.RecognitionException;
import antlr_oaa.TokenStreamException;

import com.sri.oaa2.agentlib.Agent;

@SuppressWarnings("serial")
public class OAADispatchAction extends OAAAction {

	public static String DISPATCH_GOAL_PREFIX = "x";
	
	public OAADispatchAction(Agent oaaAgent, String name, String filename) throws RecognitionException, TokenStreamException {
		super(oaaAgent, name, null, DISPATCH_GOAL_PREFIX + "PlayFile('" + filename + "')");
	}
}
