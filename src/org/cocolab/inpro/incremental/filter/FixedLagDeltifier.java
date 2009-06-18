/* 
 * Copyright 2008, 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.incremental.filter;

import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.incremental.util.ResultUtil;

import edu.cmu.sphinx.decoder.search.Token;
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
