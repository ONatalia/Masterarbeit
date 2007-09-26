package org.cocolab.inpro.sphinx.instrumentation;

import java.util.EventListener;

/**
 *  The listener interface for being informed when 
 *  new signal features are available
 */
public interface SignalFeatureListener extends EventListener {
	/**
     * Method called when a new set of signal features is available
     *
     * @param logEnergy log energy of the frame
     *
     */
     public void newSignalFeatures(double logEnergy);
}
