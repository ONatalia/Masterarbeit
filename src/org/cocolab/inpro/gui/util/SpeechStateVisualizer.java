/* (c) 2009 Timo Baumann. released as-is to the public domain. */
package org.cocolab.inpro.gui.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

public class SpeechStateVisualizer extends BaseDataProcessor {

	@S4Boolean(defaultValue = true)
    public final static String PROP_SHOW_WINDOW = "showWindow";
	
	@S4Component(type = Configurable.class, mandatory = false)
	public final static String PROP_STYLE = "style";
	Configurable styleResource;
	
	FourStateJLabel speechIndicator;
	JFrame f;
	JToggleButton recordButton;
	
	boolean isRecording = true;

	public SpeechStateVisualizer() {
		styleResource = this;
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
	
	@SuppressWarnings("serial")
	private class FourStateJLabel extends JLabel {

		private boolean userIsTalking = false;
		private boolean systemIsTalking = false;
		private ImageIcon silentIcon = new ImageIcon(styleResource.getClass().getResource("happyhal-inactive.png"));
		private ImageIcon bothTalkingIcon = new ImageIcon(styleResource.getClass().getResource("happyhal-active-vad.png"));
		private ImageIcon userTalkingIcon = new ImageIcon(styleResource.getClass().getResource("happyhal-inactive-vad.png"));
		private ImageIcon systemTalkingIcon = new ImageIcon(styleResource.getClass().getResource("happyhal-active.png"));

		public FourStateJLabel() {this.setIcon();}
		
		public void setIcon(){
			if (systemIsTalking && userIsTalking) {
				super.setIcon(bothTalkingIcon);
			} else if (systemIsTalking) {
				super.setIcon(systemTalkingIcon);
			} else if (userIsTalking) {
				super.setIcon(userTalkingIcon);
			} else {
				super.setIcon(silentIcon);
			}
		}
		
		public void userTalking(boolean talking) {
			this.userIsTalking = talking;
			this.setIcon();
		}

		public void systemTalking(boolean talking) {
			this.systemIsTalking = talking;
			this.setIcon();
		}
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		styleResource = ps.getComponent(PROP_STYLE);
		if (styleResource == null) 
			styleResource = this;
		speechIndicator = new FourStateJLabel();
		speechIndicator.setEnabled(true);
		if (ps.getBoolean(PROP_SHOW_WINDOW)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					speechIndicator.setIcon();
					f = new JFrame("speech estimation");
					f.setLocation(500, 100);
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

	public void systemTalking(boolean talking) {
		speechIndicator.systemTalking(talking);
	}

	public JLabel getSpeechIndicator() {
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
			speechIndicator.userTalking(scd.isSpeech());
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
