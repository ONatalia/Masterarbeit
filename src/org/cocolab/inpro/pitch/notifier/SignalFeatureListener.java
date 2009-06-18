/* 
 * Copyright 2007, 2008, 2009, Timo Baumann and the Inpro project
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

package org.cocolab.inpro.pitch.notifier;

import java.util.EventListener;

import edu.cmu.sphinx.instrumentation.Resetable;

/**
 *  The listener interface for being informed when 
 *  new signal features are available
 */
public interface SignalFeatureListener extends EventListener, Resetable {
	/**
     * Method called when a new set of signal features is available
     *
     * @param logEnergy log energy of the frame
     *
     */
     public void newSignalFeatures(double logEnergy, boolean voicing, double pitch);
     
}
