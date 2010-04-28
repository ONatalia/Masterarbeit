package org.cocolab.inpro.training;

/* 
 * Copyright 2009, 2010, Timo Baumann and the Inpro project
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

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.cocolab.inpro.apps.SimpleReco;
import org.cocolab.inpro.gui.util.SpeechStateVisualizer;
import org.cocolab.inpro.incremental.listener.CurrentHypothesisViewer;
import org.cocolab.inpro.sphinx.frontend.Microphone;
import org.cocolab.inpro.sphinx.frontend.WavTEDLogger;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

/**
 * Software to collect user-annotated semi-spontaneous speech.
 * 
 * DataCollector combines ASR and recording tool in order
 * to improve understanding between users and ASR. The user
 * gets a feeling for what the ASR may be able to understand
 * while the annotated output from the user can be used
 * to further improve ASR. 
 * <p>
 * DataCollector can be distributed as a Java WebStart application
 * which enables data collections from a broad (and cheap) user base.
 * <p>
 * Visual cues to what should or could be recorded can be
 * given in a slide show panel, which also enables the collector
 * to prime specific words or contexts in which an utterance
 * is produced.
 * <p>
 * This approach allows to balance the degree of freedom
 * the collected data will expose: Very generic slide shows
 * lead to almost spontaneous utterances, while very specific
 * slides (i.e. spelling out the sentence to read) result in
 * non-spontaneous, read data.
 * 
 * @author timo
 */
@SuppressWarnings("serial")
public class DataCollector extends JPanel implements ActionListener {
	
	private final SpeechStateVisualizer ssv;
	
	// GUI buttons (we need these later to selectively disable them
	// when their related actions are not applicable)
	private final JButton uploadButton;
	private final JButton acceptButton;
	private final JButton discardButton;
	private final JButton playbackButton;
	private final JTextField resultField;
	/** panel to show and control an inspirational slide show */
	private final SlideShowPanel slidePanel;

	/** Sphinx ResultListener with a JTextField showing the current hypothesis */
	private final CurrentHypothesisViewer chv;
	/** Sphinx FrontEnd processor which writes VAD speech chunks to disk */
	// we need to know the file names of stored speech chunks
	private final WavTEDLogger wavWriter;
	
	/** encapsulates (most) ASR stuff */
	private final RecoRunner recoRunner;
	
	/** stores references to the various files, so that they can be sent to the server later on */
	private final SessionData sessionData = new SessionData();
	/** meta data for this recording session */
	MetaData metaData = new MetaData(null);

	private final CommandLineOptions configuration; 

	DataCollector(CommandLineOptions configuration) {
		super(new GridBagLayout());
		this.configuration = configuration;

		// setup CurrentHypothesisViewer (Sphinx' ResultListener and TextField)
		chv = (CurrentHypothesisViewer) configuration.lookup("hypViewer");
		chv.updateResults(false); // do not update for now
		chv.getTextField().setText("\t\tInitialisierung...");
		wavWriter = (WavTEDLogger) configuration.lookup("utteranceWavWriter");
		wavWriter.setDumpFilePath(System.getProperty("java.io.tmpdir") + "/dc.");

		ssv = (SpeechStateVisualizer) configuration.lookup("speechStateVisualizer");

		ssv.getMuteButton().setEnabled(false);
		ssv.getMuteButton().addActionListener(this);

		uploadButton = createButton("pack-and-upload.png", "Upload to Server", this, "UPLOAD");
		acceptButton = DataCollector.createButton("dialog-ok.png", "This is what I said.", this, "ACCEPT");
		discardButton = DataCollector.createButton("dialog-cancel.png", "Cancel this Recording.", this, "DISCARD");

		playbackButton = new JButton(new AbstractAction("", new ImageIcon(SpeechStateVisualizer.class.getResource("media-playback-start.png"))) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				playFile(recoRunner.getMostRecentFilename());
			}
		});
		playbackButton.setToolTipText("Play back the recording.");
		playbackButton.setEnabled(false);

		resultField = new JTextField();
		resultField.setFont(CurrentHypothesisViewer.DEFAULT_FONT);
		resultField.setEditable(true);
		resultField.setEnabled(false);
		
		slidePanel = new SlideShowPanel(SwingConstants.TOP);
		
		// add all necessary components to the GridBag
		GridBagConstraints gbs = new GridBagConstraints();
		gbs.gridx = 1;
		gbs.gridy = 1;
		gbs.fill = GridBagConstraints.HORIZONTAL;
		gbs.gridheight = 2;
		gbs.insets = new Insets(5, 0, 0, 0); // a little slack around the components
		ssv.getSpeechIndicator().setIcon(new ImageIcon(DataCollector.class.getResource("happyhal-inactive-vad.png")));
		add(ssv.getSpeechIndicator(), gbs);
		
		gbs.insets = new Insets(5, 0, 0, 5); // a little slack around the components
		gbs.gridheight = 1;
		gbs.gridx = 2;
		gbs.weightx = 1.0;
		chv.getTextField().setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.LIGHT_GRAY));
		add(chv.getTextField(), gbs);
		gbs.gridy = 2;
		gbs.gridwidth = 2;
		add(resultField, gbs);
		gbs.insets = new Insets(5, 5, 0, 5); // a little slack around the components
		gbs.gridwidth = 2;
		gbs.weightx = 0.0;
		gbs.gridx = 3;
		gbs.gridy = 1;
		add(ssv.getMuteButton(), gbs);
		gbs.gridwidth=1;
		gbs.gridx = 6;
		
		gbs.gridwidth = 1;
		gbs.gridx = 4;
		gbs.gridy = 2;
		add(acceptButton, gbs);
		gbs.gridx = 5;
		gbs.gridy = 1;
		gbs.gridwidth = 1;
		add(uploadButton, gbs);
		gbs.gridy = 2;
		add(discardButton, gbs);
		gbs.gridx = 6;
		gbs.gridy = 2;
		add(playbackButton, gbs);
		
		gbs.gridx = 1;
		gbs.gridy = 3;
		gbs.gridwidth = 7;
		gbs.weightx = 1.0;
		gbs.weighty = 1.0;
		gbs.fill = GridBagConstraints.BOTH;
		gbs.insets = new Insets(0, 1, 0, 0); // a little slack around the components
		add(slidePanel, gbs);

		setOptions();
		
		recoRunner = new RecoRunner();
		// ASR initialization, will enable asrPanel when it's finished
		(new RecoStart()).execute();
	}
	
	/** 
	 * used in GUI creation
	 * @param icon name of the icon resource (relative to this class)
	 * @param tooltip tooltip for this button
	 * @param al action listener for this button
	 * @param command executed on action listener
	 * @return
	 */
	static JButton createButton(String icon, String tooltip, ActionListener al, String command) {
		JButton button = new JButton(new ImageIcon(DataCollector.class.getResource(icon)));
		button.setToolTipText(tooltip);
		button.setEnabled(false); // all buttons start disabled
		button.addActionListener(al);
		button.setActionCommand(command);
		return button;
	}
	
	/**
	 * add config files to sessionData. 
	 */
	private void setOptions() {
		sessionData.addFromURL(configuration.configURL);
		try {
			slidePanel.setXML(configuration.slideURL);
			sessionData.addFromURL(configuration.slideURL);
		} catch (IOException e) { } // simply ignore broken slide show URLs
	}
	
	/**
	 * RecoStart is used to start up recognition (AM loading, ...) in a SwingWorker thread.
	 * once finished, it calls recoRunner, the thread that continuously
	 * runs recognition in the background and pushes ASR_RESULT events 
	 * onto the Swing event queue.
	 */
	private class RecoStart extends SwingWorker<Void, Void> {
		
		@Override
		protected Void doInBackground() throws Exception {
			try {
				Recognizer recognizer = (Recognizer) configuration.lookup("recognizer");
				recognizer.allocate();
				recoRunner.setRecognizer(recognizer);
				final Microphone mic = (Microphone) configuration.lookup("microphone");
				SimpleReco.setupMicrophone(mic);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "ASR Error: " + e.toString());
				System.exit(1);
			}
			return null;
		}
		
		@Override
		protected void done() {
			// enable controls, remove initialization message
			ssv.getMuteButton().setEnabled(true);

			chv.getTextField().setText("");
			recoRunner.start();
			// and start recognition mode
			recoRunner.setRecognizing(true);
		}
	}
	
	private class RecoRunner extends Thread {
		/** determines whether new results should be thrown away or passed onto swing UI */ 
		private boolean updateResults = false;
		private String mostRecentRecoResult = null;
		private String mostRecentFileName = "";
		
		Recognizer recognizer;

		public synchronized void setRecognizer(Recognizer recognizer) {
			this.recognizer = recognizer;
		}
		
		public void setRecognizing(boolean enabled) {
			updateResults = enabled;
			chv.updateResults(enabled);
		}
		
		public String getMostRecentRecoResult() {
			return mostRecentRecoResult;
		}
		
		public String getMostRecentFilename() {
			return mostRecentFileName;
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
				Result result = recognizer.recognize();
				//System.out.println("Reco is done: " + result);
				if ((updateResults) 
					&& (result != null) 
					&& (result.getDataFrames() != null)
					&& (result.getDataFrames().size() > 4)) {
					mostRecentRecoResult = result.getBestFinalToken().getWordPath();
					mostRecentRecoResult = mostRecentRecoResult.replaceAll("<\\/?s>", ""); // remove <s> and </s>
					mostRecentRecoResult = mostRecentRecoResult.replaceFirst("^\\s*<sil> ", ""); // remove leading "<sil> "
					mostRecentFileName = wavWriter.getMostRecentFilename();
					sessionData.addFile(mostRecentFileName);
					// make sure we don't eternally fill up temp space
					(new File(mostRecentFileName)).deleteOnExit();
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run(){
								actionPerformed(new ActionEvent(recoRunner, 0, "ASR_RESULT"));
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void setRecognizing(boolean recognizing) {
		chv.getTextField().setText("");
		ssv.getMuteButton().setEnabled(recognizing);
		ssv.setRecording(recognizing);
		
		acceptButton.setEnabled(!recognizing);
		discardButton.setEnabled(!recognizing);
		playbackButton.setEnabled(!recognizing);
		resultField.setEnabled(!recognizing);
		recoRunner.setRecognizing(recognizing);
		if (recognizing) {
			resultField.setText("");
		}
	}
	
	/**
	 * Action handler for all actions in the application.
	 * handles all (four) actions in the GUI
	 * plus the one action from ASR.
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		if (command.equals("PAUSE")) {
			boolean isPaused = !((JToggleButton) ae.getSource()).isSelected(); 
			recoRunner.setRecognizing(!isPaused);
			uploadButton.setEnabled(isPaused);
		} else if (command.equals("ASR_RESULT")) {
			/* when a new result comes in:
			 * - stop to recognize (we now come to the editing phase)
			 * - save current scene information,
			 * - save the ASR hypothesis,
			 * - show the ASR hypothesis
			 * */
			setRecognizing(false);
			saveTranscript(slidePanel.getCurrentSlideInfo(), ".scene");
			String resultText = recoRunner.getMostRecentRecoResult();
			saveTranscript(resultText, ".hyp");
			resultField.setText(resultText);
		} else if (command.equals("ACCEPT")) {
			String resultText = resultField.getText();
			saveTranscript(resultText, ".ref");
			setRecognizing(true);
		} else if (command.equals("DISCARD")) {
			setRecognizing(true);
		} else if (command.equals("UPLOAD")) {
			// get meta data
			String metaDataString = metaData.getData();
			// store it in session data
			sessionData.addSmallFile("META-DATA.txt", metaDataString);
			// prepare a dialogue to be displayed during upload
			final JDialog waitDialog = new JDialog((JFrame) null, "Bitte warten", true);
			waitDialog.add(new JLabel("Dateien werden zum Server hochgeladen", 
					new ImageIcon(SessionData.class.getResource("spinning_wheel_throbber.gif")), 0));
			waitDialog.setLocationRelativeTo(null);
			waitDialog.pack();
			// start a new worker thread that submits session data
			// and destroys the waitDialog when the upload is done 
			(new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					sessionData.postToServer(configuration.uploadURL);
					return null;
				}
				@Override 
				protected void done() {
					sessionData.clear();
					setOptions();
					waitDialog.dispose();					
				}
			}).execute();
			// show the wait dialog (which blocks execution until dispose is
			// called once upload has finished
			waitDialog.setVisible(true);
		}
	}	
	
	/** save a transcript to sessiondata */
	private void saveTranscript(String transcript, String filetype) {
		String filename = recoRunner.getMostRecentFilename().replaceFirst("\\.wav$", filetype);
		sessionData.addSmallFile(filename, transcript);
	}
	
	
	/** playback the given audio file to the speakers */
	public void playFile(String fileName) {
		AudioStream as;
		try {
			as = new AudioStream(new FileInputStream(fileName));
			AudioPlayer.player.start(as);
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}

	/**
	 * used to construct the GUI on the Swing thread.
	 * @param commandLineOptions 
	 */
	public static void createAndShowGUI(CommandLineOptions configuration) {
		JFrame frame = new JFrame("Inpro Data Collector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		DataCollector mainPanel = new DataCollector(configuration);
		// add config to session data
		mainPanel.setOpaque(true);
		frame.setContentPane(mainPanel);
		// Display the window.
        frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * main method for the application
	 * @param args for argument parsing see CommandLineOptions
	 */
	public static void main(final String[] args) {
		final CommandLineOptions clo = new CommandLineOptions(args); 
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(clo);
			}
		});
	}
	
}
