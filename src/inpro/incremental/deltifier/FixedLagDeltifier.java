package inpro.incremental.deltifier;

import inpro.sphinx.ResultUtil;

import java.util.List;
import java.util.ListIterator;


import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

/**
 * fixed lag deltifier only passes along words that have not 
 * started with a given fixed lag
 */

public class FixedLagDeltifier extends ASRWordDeltifier {

    @S4Integer(defaultValue = 0)
	public final static String PROP_FIXED_LAG = "fixedLag";
	int fixedLag;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		fixedLag = ps.getInt(PROP_FIXED_LAG);
	}
	
	@Override
	/*
	 * remove *word tokens* (and corresponding unit tokens) that have started within the fixed lag 
	 */
	protected synchronized List<Token> getTokens(Token token) {
		List<Token> newTokens = ResultUtil.getTokenList(token, true, true);
		/* word tokens precede unit tokens in the list */
		if (!recoFinal) {
			// start from the beginning (much easier, as word tokens now precede unit tokens)
			ListIterator<Token> iter = newTokens.listIterator(0);
			while (iter.hasNext()) {
				if (iter.next().getFrameNumber() > currentFrame - fixedLag) {
					iter.previous();
					break;
				}
			}
			while (iter.hasNext()) {
				iter.next();
				iter.remove();
			}
		}
		return newTokens;
	}
	
	@Override
	public String toString() {
		return "FixedLagDeltifier with lag " + fixedLag;
	}
	
}
