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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
 * slides (e.g. spelling out the sentence to read) result in
 * non-spontaneous, read data.
 * 
 * @author timo
 */
@SuppressWarnings("serial")
public class DataCollector extends JPanel implements ActionListener {
	
	static final Object noResult = new Object();
	
	// sub panels for ASR and result handling
	private ASRPanel asrPanel;
	private ResultPanel resultPanel;
	/** panel to show an inspirational slide show */
	private SlideShowPanel slidePanel;

	private CurrentHypothesisViewer chv;

	private WavTEDLogger wavWriter;
	
	/** encapsulates (most) ASR stuff */
	private RecoRunner recoRunner;
	
	boolean isRecognizing;
	
	DataCollector() {
		super(new BorderLayout());
		ConfigurationManager cm = new ConfigurationManager(DataCollector.class.getResource("config.xml"));

		// setup CurrentHypothesisViewer (Sphinx' ResultListener and TextField)
		chv = (CurrentHypothesisViewer) cm.lookup("hypViewer");
		wavWriter = (WavTEDLogger) cm.lookup("utteranceWavWriter");

		SpeechStateVisualizer ssv = (SpeechStateVisualizer) cm.lookup("speechStateVisualizer");
		
		JPanel p = new JPanel();
		p.add(ssv.getSpeechIndicator());
		// all actions are handled in MyActionListener, see below
		
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
		
		// create the ASR panel using this CurrentHypothesisViewer
		asrPanel = new ASRPanel(chv, this); 
		asrPanel.setEnabled(false); // disable for now, will be enabled once ASR initialization completes
		p2.add(asrPanel);
		
		resultPanel = new ResultPanel(this);
		resultPanel.setEnabled(false);
		p2.add(resultPanel);
		
		p.add(p2);
		this.add(p, BorderLayout.NORTH);
		slidePanel = new SlideShowPanel(SwingConstants.TOP);
		this.add(slidePanel, BorderLayout.CENTER);
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
		
		ActionListener myActionListener;
		
		boolean updateResults = false;
		Result mostRecentResult = null;
		String mostRecentWaveFile = "";
		
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
		
		public void run() {
			while (true) {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
				Result result = recognizer.recognize();
				System.out.println("Reco is done: " + result);
				if ((updateResults) && (result != null) && (result.getDataFrames() != null) && (result.getDataFrames().size() > 4)) {
					mostRecentResult = result;
					mostRecentWaveFile = wavWriter.getMostRecentFilename();
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
			 * - stop to recognize (we now come to the editing phase
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
		} else if (command.equals("CONFIG")) {
			configurePath();
		}
	}	
	
	private void configurePath() {
		String oldPath = wavWriter.getDumpFilePath();
		String newPath = (String) JOptionPane.showInputDialog(null, 
				"Path and prefix for collected files", 
				"Configuration", 
				JOptionPane.PLAIN_MESSAGE, null, null, oldPath);
		wavWriter.setDumpFilePath(newPath);
	}

	private void saveTranscript(String transcript, String filetype) {
		String filename = recoRunner.mostRecentWaveFile.replaceFirst("\\.wav$", filetype);
		try {
			PrintStream outFile = new PrintStream(filename);
			outFile.println(transcript);
		} catch (FileNotFoundException e) {
			System.err.println("Could not write to " + filename);
		}
	}

	/**
	 * used to construct the GUI on the Swing thread.
	 */
	public static void createAndShowGUI() {
		JFrame frame = new JFrame("Inpro Data Collector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		DataCollector contentPane = new DataCollector();
		contentPane.setOpaque(true);
		contentPane.slidePanel.setXML(DataCollector.class.getResourceAsStream("slides.xml"));
		frame.setContentPane(contentPane);
		//Display the window.
        frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * main method for the application
	 * @param args arguments are ignored
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();	
			}
		});
	}
	
}
