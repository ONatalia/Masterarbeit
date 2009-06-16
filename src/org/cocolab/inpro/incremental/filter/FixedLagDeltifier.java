package org.cocolab.inpro.incremental.filter;

import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.ResultUtil;

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
	
	@Override
	protected synchronized List<Label> getWordLabels(Token token) {
		List<Label> newWords =  ResultUtil.getWordLabelSequence(token);
		// starting from the end...
		ListIterator<Label> iter = newWords.listIterator(newWords.size());
		// remove all word labels that have started within the last fixedLag frames
		while (iter.hasPrevious() && (iter.previous().getStart() * 100.0 > currentFrame - fixedLag)) {
			iter.remove();
		}
		return newWords;
	}
	
}
