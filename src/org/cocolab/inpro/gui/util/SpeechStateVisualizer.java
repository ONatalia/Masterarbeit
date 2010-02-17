/* (c) 2009 Timo Baumann. released as-is to the public domain. */
package org.cocolab.inpro.gui.util;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;

public class SpeechStateVisualizer extends BaseDataProcessor {

	@S4Boolean(defaultValue = true)
    public final static String PROP_SHOW_WINDOW = "showWindow";
	
	JLabel speechIndicator;
	JFrame f;

	public SpeechStateVisualizer() {
		ImageIcon silentIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive.png"));
		ImageIcon talkingIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive-vad.png"));
		speechIndicator = new JLabel(talkingIcon);
		speechIndicator.setDisabledIcon(silentIcon);
		speechIndicator.setEnabled(false);
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		if (ps.getBoolean(PROP_SHOW_WINDOW)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					f = new JFrame("speech estimation");
					f.add(speechIndicator);
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.pack();
				}
			});
		}
	}
	
	public JComponent getSpeechIndicator() {
		return speechIndicator;
	}

	@Override
	public Data getData() throws DataProcessingException {
		if (f != null && !f.isVisible()) {
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
