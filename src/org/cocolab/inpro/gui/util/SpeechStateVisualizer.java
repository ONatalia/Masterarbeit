package org.cocolab.inpro.gui.util;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.cocolab.inpro.gui.pentomino.p2.UserInterface;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

public class SpeechStateVisualizer extends BaseDataProcessor {

	JLabel speechIndicator;
	
	public SpeechStateVisualizer() {
		ImageIcon silentIcon = new ImageIcon(UserInterface.class.getResource("happyhal-inactive.png"));
		ImageIcon talkingIcon = new ImageIcon(UserInterface.class.getResource("happyhal-active.png"));
		speechIndicator = new JLabel(talkingIcon);
		speechIndicator.setDisabledIcon(silentIcon);
		speechIndicator.setEnabled(false);
		JFrame f = new JFrame("your speech estimation");
		f.add(speechIndicator);
		f.pack();
		f.setVisible(true);		
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();
		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData scd = (SpeechClassifiedData) d;
			speechIndicator.setEnabled(scd.isSpeech());
		}
		return d;
	}

}
