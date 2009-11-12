package test.org.cocolab.inpro.sphinx.train;

/* 
 * Copyright 2009, Timo Baumann and the Inpro project
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

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.cocolab.inpro.incremental.listener.CurrentHypothesisViewer;
import org.cocolab.inpro.sphinx.frontend.WavTEDLogger;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

@SuppressWarnings("serial")
public class DataCollector extends JFrame {
	
	static final Object noResult = new Object();
	
	// sub panels for ASR and result handling
	ASRPanel asrPanel;
	ResultPanel resultPanel;
	// handles all (three) actions in the GUI
	ActionListener myActionListener;

	// Sphinx' ASR stuff 
	ConfigurationManager cm;
	
	WavTEDLogger wavWriter;
	Recognizer recognizer;
	
	// encapsulates (most) ASR stuff
	RecoRunner recoRunner;
	
	boolean isRecognizing;
	
	DataCollector() {
		super("Inpro Data Collector");
		cm = new ConfigurationManager(DataCollector.class.getResource("config.xml"));
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

		// all actions are handled in MyActionListener, see below
		myActionListener = new MyActionListener();		
		
		// setup CurrentHypothesisViewer (Sphinx' ResultListener and TextField)
		CurrentHypothesisViewer chv = (CurrentHypothesisViewer) cm.lookup("hypViewer");
		// create the ASR panel using this CurrentHypothesisViewer
		asrPanel = new ASRPanel(chv); 
		asrPanel.setEnabled(false); // disable for now, will be enabled once ASR initialization completes
		asrPanel.chvField.setText("\t\tInitialisierung...");
		this.add(asrPanel);
		
		resultPanel = new ResultPanel();
		resultPanel.setEnabled(false);
		this.add(resultPanel);
		
		// show GUI
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.pack();
		this.setVisible(true);
		
		// ASR initialization, will enable asrPanel when it's finished
		(new RecoStart()).execute();
	}
	
	private class RecoStart extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
			Microphone mic = (Microphone) cm.lookup("microphone");
			recognizer = (Recognizer) cm.lookup("recognizer");
			recognizer.allocate();
			wavWriter = (WavTEDLogger) cm.lookup("utteranceWavWriter");
			mic.initialize();
			if (!mic.startRecording()) {
				System.err.println("Could not start microphone. Exiting...");
				System.exit(1);
			}
			return null;
		}
		@Override
		protected void done() {
			asrPanel.setEnabled(true);
			recoRunner = new RecoRunner();
			recoRunner.start();
		}
	}
	
	private class RecoRunner extends Thread {
		
		boolean updateResults = false;
		Result mostRecentResult = null;
		String mostRecentWaveFile = "";
		
		public void setRecognizing(boolean enabled) {
			updateResults = enabled;
			asrPanel.chv.updateResults(enabled);
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
	
	private class ASRPanel extends JPanel {
		CurrentHypothesisViewer chv;
		JTextField chvField;
		JToggleButton asrButton;

		ASRPanel(CurrentHypothesisViewer chv) {
			super(new FlowLayout(FlowLayout.LEFT));
			this.chv = chv;
			chvField = chv.getTextField();
			this.add(chvField);
			asrButton = new JToggleButton(new ImageIcon(DataCollector.class.getResource("media-playback-start.png"), "start"));
			asrButton.setSelectedIcon(new ImageIcon(DataCollector.class.getResource("media-playback-pause.png"), "stop"));
			asrButton.setSelected(true);
			asrButton.setActionCommand("PAUSE");
			asrButton.addActionListener(myActionListener);
			chv.updateResults(false);
			this.add(asrButton);
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			chvField.setText("");
			asrButton.setEnabled(enabled);
		}
		
	}

	private class ResultPanel extends JPanel {
		JTextField bestResult;
		JButton submitButton;
		JButton skipButton;
		
		ResultPanel() {
			super(new FlowLayout(FlowLayout.LEFT));
			bestResult = new JTextField("", 35);
			bestResult.setFont(new Font("Dialog", Font.BOLD, 24));
			bestResult.setEditable(true);
			this.add(bestResult);
			submitButton = new JButton(new ImageIcon(DataCollector.class.getResource("dialog-ok.png")));
			submitButton.setActionCommand("SUBMIT");
			submitButton.addActionListener(myActionListener);
			this.add(submitButton);
			skipButton = new JButton(new ImageIcon(DataCollector.class.getResource("dialog-cancel.png")));
			skipButton.setActionCommand("DISCARD");
			skipButton.addActionListener(myActionListener);
			this.add(skipButton);
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			bestResult.setEnabled(enabled);
			submitButton.setEnabled(enabled);
			skipButton.setEnabled(enabled);
		}
	}

	private class MyActionListener implements ActionListener {
		
		private void setRecognizing(boolean enabled) {	
			if (enabled) {
				recoRunner.setRecognizing(true);
				asrPanel.setEnabled(true);
				resultPanel.bestResult.setText("");
				resultPanel.setEnabled(false);
			} else {
				recoRunner.setRecognizing(false);
				asrPanel.setEnabled(false);
				resultPanel.setEnabled(true);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent ae) {
			String command = ae.getActionCommand();
			if (command.equals("PAUSE")) {
				boolean isPaused = ((JToggleButton) ae.getSource()).isSelected(); 
				recoRunner.setRecognizing(!isPaused);
			} else if (command.equals("ASR_RESULT")) {
				setRecognizing(false);
				Result result = ((RecoRunner) ae.getSource()).mostRecentResult;
				String resultText = result.getBestFinalToken().getWordPath();
				resultPanel.bestResult.setText(resultText);
				saveTranscript(resultText, ".hyp");
			} else if (command.equals("SUBMIT")) {
				String resultText = resultPanel.bestResult.getText();
				saveTranscript(resultText, ".ref");
				setRecognizing(true);
			} else if (command.equals("DISCARD")) {
				setRecognizing(true);
			} 
		}	
	}	

	void saveTranscript(String transcript, String filetype) {
		String filename = recoRunner.mostRecentWaveFile.replaceFirst("\\.wav$", filetype);
		try {
			PrintStream outFile = new PrintStream(filename);
			outFile.println(transcript);
		} catch (FileNotFoundException e) {
			System.err.println("Could not write to " + filename);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new DataCollector();	
			}
		});
	}
	
}
