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
