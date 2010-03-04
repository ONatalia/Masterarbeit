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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.cocolab.inpro.gui.util.SpeechStateVisualizer;
import org.cocolab.inpro.incremental.listener.CurrentHypothesisViewer;
import org.cocolab.inpro.sphinx.frontend.WavTEDLogger;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

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
	
	// sub panels for ASR and result handling
	private final ASRPanel asrPanel;
	private final ResultPanel resultPanel;
	/** panel to show an inspirational slide show */
	private final SlideShowPanel slidePanel;

	private final CurrentHypothesisViewer chv;
	private final WavTEDLogger wavWriter;
	
	/** encapsulates (most) ASR stuff */
	private final RecoRunner recoRunner;
	
	/** stores references to the various files, so that they can be sent to the server later on */
	private final SessionData sessionData = new SessionData();
	/** meta data for this recording session */
	MetaData metaData = new MetaData(null);

	DataCollector(URL configXML) {
		super(new GridBagLayout());
		ConfigurationManager cm = new ConfigurationManager(configXML);

		// setup CurrentHypothesisViewer (Sphinx' ResultListener and TextField)
		chv = (CurrentHypothesisViewer) cm.lookup("hypViewer");
		wavWriter = (WavTEDLogger) cm.lookup("utteranceWavWriter");
		wavWriter.setDumpFilePath(System.getProperty("java.io.tmpdir") + "/dc.");

		SpeechStateVisualizer ssv = (SpeechStateVisualizer) cm.lookup("speechStateVisualizer");

		// create the ASR panel using this CurrentHypothesisViewer
		asrPanel = new ASRPanel(chv, this); 
		asrPanel.setEnabled(false); // disable for now, will be enabled once ASR initialization completes
		
		resultPanel = new ResultPanel(this);
		resultPanel.setEnabled(false);

		slidePanel = new SlideShowPanel(SwingConstants.TOP);
		
		// add all necessary components to the GridBag
		GridBagConstraints gbs = new GridBagConstraints();
		gbs.gridx = 1;
		gbs.gridy = 1;
		gbs.fill = GridBagConstraints.HORIZONTAL;
		gbs.gridheight = 2;
		gbs.insets = new Insets(5, 5, 0, 5); // a little slack around the components
		add(ssv.getSpeechIndicator(), gbs);		
		
		gbs.gridheight = 1;
		gbs.gridx = 2;
		gbs.weightx = 1.0;
		add(asrPanel.getHypothesisField(), gbs);
		gbs.gridy = 2;
		gbs.gridwidth = 2;
		add(resultPanel.getResultField(), gbs);
		gbs.gridwidth = 2;
		gbs.weightx = 0.0;
		gbs.gridx = 3;
		gbs.gridy = 1;
		add(asrPanel.getASRButton(), gbs);
		//add(new MuteButton((Microphone) cm.lookup("microphone")), gbs);
		gbs.gridwidth = 1;
		gbs.gridx = 4;
		gbs.gridy = 2;
		add(resultPanel.getAcceptButton(), gbs);
		gbs.gridx = 5;
		gbs.gridy = 1;
		gbs.gridwidth = 1;
		add(asrPanel.getUploadButton(), gbs);
		gbs.gridy = 2;
		add(resultPanel.getDiscardButton(), gbs);
		
		gbs.gridx = 1;
		gbs.gridy = 3;
		gbs.gridwidth = 5;
		gbs.weightx = 1.0;
		gbs.weighty = 1.0;
		gbs.fill = GridBagConstraints.BOTH;
		gbs.insets = new Insets(0, 1, 0, 0); // a little slack around the components
		add(slidePanel, gbs);

		recoRunner = new RecoRunner(this);
		// ASR initialization, will enable asrPanel when it's finished
		(new RecoStart(recoRunner, cm)).execute();
	}
	
	private class RecoStart extends SwingWorker<Void, Void> {
		
		RecoRunner recoRunner;
		ConfigurationManager cm;
		
		RecoStart(RecoRunner recoRunner, ConfigurationManager cm) {
			this.recoRunner = recoRunner;
			this.cm = cm;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			Microphone mic = (Microphone) cm.lookup("microphone");
			Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
			recognizer.allocate();
			
			mic.initialize();
			if (!mic.startRecording()) {
				System.err.println("Could not start microphone. Exiting...");
				System.exit(1);
			}
			this.recoRunner.setRecognizer(recognizer);
			return null;
		}
		
		@Override
		protected void done() {
			asrPanel.setEnabled(true);
			this.recoRunner.start();
		}
	}
	
	private class RecoRunner extends Thread {
		
		private ActionListener myActionListener;
		
		private boolean updateResults = false;
		private Result mostRecentResult = null;
		private String mostRecentWaveFile = "";
		
		Recognizer recognizer;

		RecoRunner(ActionListener myActionListener) {
			this.myActionListener = myActionListener;
		}
		
		public void setRecognizer(Recognizer recognizer) {
			this.recognizer = recognizer;
		}
		
		public void setRecognizing(boolean enabled) {
			updateResults = enabled;
			chv.updateResults(enabled);
		}
		
		public String getMostRecentFilename() {
			return mostRecentWaveFile;
		}
		
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
					mostRecentResult = result;
					mostRecentWaveFile = wavWriter.getMostRecentFilename();
					sessionData.addFile(mostRecentWaveFile);
					// make sure we don't eternally fill up temp space
					(new File(mostRecentWaveFile)).deleteOnExit();
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run(){
								myActionListener.actionPerformed(new ActionEvent(recoRunner, 0, "ASR_RESULT"));
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void setRecognizing(boolean enabled) {	
		if (enabled) {
			recoRunner.setRecognizing(true);
			asrPanel.setEnabled(true);
			resultPanel.reset();
			resultPanel.setEnabled(false);
		} else {
			recoRunner.setRecognizing(false);
			asrPanel.setEnabled(false);
			resultPanel.setEnabled(true);
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
			boolean isPaused = ((JToggleButton) ae.getSource()).isSelected(); 
			recoRunner.setRecognizing(!isPaused);
			asrPanel.setConfigurable(isPaused);
		} else if (command.equals("ASR_RESULT")) {
			/* when a new result comes in:
			 * - stop to recognize (we now come to the editing phase)
			 * - save current scene information,
			 * - save the ASR hypothesis,
			 * - show the ASR hypothesis
			 * */
			setRecognizing(false);
			saveTranscript(slidePanel.getCurrentSlideInfo(), ".scene");
			Result result = ((RecoRunner) ae.getSource()).mostRecentResult;
			String resultText = result.getBestFinalToken().getWordPath();
			saveTranscript(resultText, ".hyp");
			resultPanel.setResult(resultText);
		} else if (command.equals("ACCEPT")) {
			String resultText = resultPanel.getResult();
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
			waitDialog.pack();
			// start a new worker thread that submits session data
			// and destroys the waitDialog when the upload is done 
			(new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					sessionData.postToServer("http://www.sfb632.uni-potsdam.de/cgi-timo/upload.pl");
					return null;
				}
				@Override 
				protected void done() {
					sessionData.clear();
					waitDialog.dispose();					
				}
			}).execute();
			// show the wait dialog (which blocks execution until dispose is
			// called once upload has finished
			waitDialog.setVisible(true);
		}
	}	
	
	private void saveTranscript(String transcript, String filetype) {
		String filename = recoRunner.getMostRecentFilename().replaceFirst("\\.wav$", filetype);
		sessionData.addSmallFile(filename, transcript);
	}

	/**
	 * used to construct the GUI on the Swing thread.
	 * @param commandLineOptions 
	 */
	public static void createAndShowGUI(CommandLineOptions options) {
		JFrame frame = new JFrame("Inpro Data Collector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		DataCollector mainPanel = new DataCollector(options.configURL);
		mainPanel.sessionData.addFromURL(options.configURL);
		mainPanel.sessionData.addFromURL(options.slideURL);
		mainPanel.setOpaque(true);
		try {
			mainPanel.slidePanel.setXML(options.slideURL);
		} catch (IOException e) { } // just ignore broken slide show URL 
		frame.setContentPane(mainPanel);
		//Display the window.
        frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * main method for the application
	 * @param args for argument parsing see CommandLineOptions
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(new CommandLineOptions(args));
			}
		});
	}
	
}
