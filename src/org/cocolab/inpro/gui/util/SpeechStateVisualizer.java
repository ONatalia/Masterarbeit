/* (c) 2009 Timo Baumann. released as-is to the public domain. */
package org.cocolab.inpro.gui.util;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

public class SpeechStateVisualizer extends BaseDataProcessor {

	JLabel speechIndicator;
	JFrame f;

	public SpeechStateVisualizer() {
		ImageIcon silentIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive.png"));
		ImageIcon talkingIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive-vad.png"));
		speechIndicator = new JLabel(talkingIcon);
		speechIndicator.setDisabledIcon(silentIcon);
		speechIndicator.setEnabled(false);
		f = new JFrame("your speech estimation");
		f.add(speechIndicator);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
	}

	@Override
	public Data getData() throws DataProcessingException {
		if (!f.isVisible()) {
			f.setVisible(true);
		}
		Data d = getPredecessor().getData();
		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData scd = (SpeechClassifiedData) d;
			speechIndicator.setEnabled(scd.isSpeech());
		}
		return d;
	}

}
