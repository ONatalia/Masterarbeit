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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.cocolab.inpro.incremental.listener.CurrentHypothesisViewer;
import org.cocolab.inpro.sphinx.frontend.WavTEDLogger;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class DataCollector extends JFrame {
	
	private static final long serialVersionUID = 2871403525664635161L;

	JToggleButton asrButton;
	ResultPanel resultPanel;
	
	Recognizer recognizer;

	private boolean recognizing;
	
	WavTEDLogger wavWriter;
	
	DataCollector() {
		ConfigurationManager cm = new ConfigurationManager(DataCollector.class.getResource("config.xml"));

		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

		JPanel asrControl = new JPanel(new FlowLayout(FlowLayout.LEFT));

		CurrentHypothesisViewer chv = (CurrentHypothesisViewer) cm.lookup("hypViewer");
		asrControl.add(chv.getTextField());
		
		final Microphone mic = (Microphone) cm.lookup("microphone");

		final Icon playIcon = new ImageIcon(DataCollector.class.getResource("media-playback-start.png"), "start");
		final Icon pauseIcon = new ImageIcon(DataCollector.class.getResource("media-playback-pause.png"), "stop");
		asrButton = new JToggleButton(playIcon);
		asrButton.setSelectedIcon(pauseIcon);
		asrButton.setSelected(true);
		asrButton.setEnabled(false);
		asrButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (asrButton.isSelected()) {
					System.out.println("Stopping microphone");
					mic.stopRecording();
				} else {
					System.out.println("Starting microphone");
					mic.startRecording();
				}
			}
		});
		
		asrControl.add(asrButton);
		this.add(asrControl);
		
		resultPanel = new ResultPanel();
		resultPanel.setEnabled(false);
		this.add(resultPanel);
		
		this.pack();
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
		
		recognizer = (Recognizer) cm.lookup("recognizer");
		recognizer.allocate();
		
		wavWriter = (WavTEDLogger) cm.lookup("utteranceWavWriter");

		mic.initialize();
		setRecognizing(true);
		Result result;
		do {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (recognizing && mic.isRecording()) {
				System.out.println("starting reco");
				result = recognizer.recognize();
				System.out.println("Reco is done" + result);
				if ((result != null) && (result.getDataFrames() != null) && (result.getDataFrames().size() > 4)) {
					newResult(result);
				}
			}
		} while (true);
	}
	
	void setRecognizing(boolean recognizing) {
		if (recognizing) {
			this.recognizing = true;
			
			asrButton.setEnabled(true);
		} else {
			System.exit(1);
		}
	}
	
	public void newResult(Result result) {
		if (result.isFinal()) {
			asrButton.setEnabled(false);
			System.out.println(result.getBestResultNoFiller());
			resultPanel.setEnabled(true);
			resultPanel.newResult(result);
		}
	}

	private static String getDataPrefix(String basePath) {
/*		JFrame f = new JFrame();
		f.setLayout(new GridLayout(3, 2));
		f.add(new JLabel("Verzeichnis: "));
		JTextField path = new JTextField(basePath);
		f.add(path);
		f.add(new JLabel("Dateipräfix: "));
		JTextField name = new JTextField();
		f.add(name);
		JButton abort = new JButton("Abbrechen");
		f.add(abort);
		JButton ok = new JButton("Weiter");
		f.add(ok);
		f.setVisible(true);
		f.pack();
		return null;
*/
		return "/tmp/timo";
	}
	
	static void saveTranscript(String transcript, String filename) {
		try {
			PrintStream outFile = new PrintStream(filename);
			outFile.println(transcript);
		} catch (FileNotFoundException e) {
			System.err.println("Could not write to " + filename);
		}
	}
	
	private class ResultPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
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
			submitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					String orthoFilename = wavWriter.getMostRecentFilename().replaceFirst("\\.wav$", ".txt");
					saveTranscript(bestResult.getText(), orthoFilename);
					goBackToRecording();
				}
			});
			this.add(submitButton);
			
			skipButton = new JButton(new ImageIcon(DataCollector.class.getResource("dialog-cancel.png")));
			this.add(skipButton);
			skipButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					System.err.println("Skipping text" + bestResult.getText());
					goBackToRecording();
				}
			});
		}
		
		void goBackToRecording() {
			this.setEnabled(false);
			asrButton.setEnabled(true);
			setRecognizing(true);
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			bestResult.setEnabled(enabled);
			submitButton.setEnabled(enabled);
			skipButton.setEnabled(enabled);
		}
		
		void newResult(Result result) {
			String resultText = result.getBestFinalToken().getWordPath();
			bestResult.setText(resultText);
			String hypFilename = wavWriter.getMostRecentFilename().replaceFirst("\\.wav$", ".hyp");
			saveTranscript(resultText, hypFilename);
		}
			
	}

	public static void main(String[] args) {
		new DataCollector();	
	}
	
}
