package org.cocolab.inpro.incremental.filter;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

public class FixedLagDeltifier extends ASRWordDeltifier {

    @S4Integer(defaultValue = 0)
	public final static String PROP_FIXED_LAG = "fixedLag";
	int fixedLag;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		fixedLag = ps.getInt(PROP_FIXED_LAG);
	}
	
	public void deltify(Result result) {
		Token token = result.getBestToken();
		for (int i = 0; i < fixedLag; i++) {
			if (token != null) 
				token = token.getPredecessor();
		}
		deltify(token);
	}

}
