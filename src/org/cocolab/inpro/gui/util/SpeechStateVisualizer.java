/* (c) 2009 Timo Baumann. released as-is to the public domain. */
package org.cocolab.inpro.gui.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
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
	JToggleButton recordButton;
	
	boolean isRecording = true;

	public SpeechStateVisualizer() {
		ImageIcon silentIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive.png"));
		ImageIcon talkingIcon = new ImageIcon(SpeechStateVisualizer.class.getResource("happyhal-inactive-vad.png"));
		speechIndicator = new JLabel(talkingIcon);
		speechIndicator.setDisabledIcon(silentIcon);
		speechIndicator.setEnabled(false);
		recordButton = new JToggleButton(new ImageIcon(SpeechStateVisualizer.class.getResource("media-playback-pause.png"), "PAUSE"));
		recordButton.setActionCommand("PAUSE");
		recordButton.setSelectedIcon(new ImageIcon(SpeechStateVisualizer.class.getResource("media-record.png"), "PAUSE"));
		recordButton.setSelected(true);
		recordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				setRecording(((JToggleButton) ae.getSource()).isSelected());
			}
		});
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		if (ps.getBoolean(PROP_SHOW_WINDOW)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					f = new JFrame("speech estimation");
					JPanel p = new JPanel(new BorderLayout());
					p.add(speechIndicator, BorderLayout.CENTER);
					p.add(recordButton, BorderLayout.SOUTH);
					f.add(p);
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.pack();
				}
			});
		}
	}
	
	public void setRecording(boolean recording) {
		isRecording = recording;
		recordButton.setSelected(recording);
	}
	
	public JComponent getSpeechIndicator() {
		return speechIndicator;
	}
	
	public JToggleButton getMuteButton() {
		return recordButton;
	}

	@Override
	public Data getData() throws DataProcessingException {
		if (f != null && !f.isVisible()) {
			f.setVisible(true);
		}
		Data d;
		do { // at least once and until recording
			d = getPredecessor().getData();
		} while (!isRecording);
		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData scd = (SpeechClassifiedData) d;
			speechIndicator.setEnabled(scd.isSpeech());
		}
		return d;
	}
	
	/**
	 * for testing purposes; this should initialize the microphone, add 
	 * a speech state visualizer and a mutebutton preceeding it.
	 * when muted, nothing should pass the speech state visualizer
	 * when not muted, speech state should be visualized
	 */
	public static void main(String args[]) {
		
	}

}
