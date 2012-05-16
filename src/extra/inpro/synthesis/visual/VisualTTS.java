package extra.inpro.synthesis.visual;

import inpro.synthesis.MaryAdapter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A GUI to manipulate (improve, mimic, parody) Mary TTS.
 * The TTS's unit and prosody model can be manipulated and
 * this can be resynthesized. For example, TTS errors can be
 * corrected, prosody can be improved, etc.
 * <p/>
 * the GUI is divided into three parts: 
 * <ol>
 * <li>a panel containing buttons for loading/saving,
 * a textfield for inputting text to TTS, and a spinner to change the zoom.
 * <li>an audio panel which shows a spectrogram and can play the displayed audio
 * <li>a segment panel which displays the synthesis data model (in SegmentModel)
 * </ol>
 * @author timo
 */
@SuppressWarnings("serial")
public class VisualTTS extends JPanel {

	private static final String STARTUP_GREETING = "Hallo";
	
	static final Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 24); 

	private final TopPanel topPanel;
	private final AudioPanel audioPanel;
	private final SegmentPanel segmentPanel;
	
	private final EditDialog editDialog;
	
	/** our interface to mary */
	private final MaryAdapter maryAdapter;

	/** this is global in order to keep the selected directories in sync between loading/saving */
	private final JFileChooser fileChooser = new JFileChooser();

	VisualTTS() {
		assert (SwingUtilities.isEventDispatchThread());
		MaryAdapter maryAdapter;
		try {
			maryAdapter = MaryAdapter.getInstance();
		} catch(Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Problem connecting to TTS server: \n" + e.toString());
			System.exit(1);
			maryAdapter = null;			
		}
		this.maryAdapter = maryAdapter;
		setLayout(new BorderLayout());
		audioPanel = new AudioPanel();
		segmentPanel = new SegmentPanel(synthesisAction, null);
		topPanel = new TopPanel();
		add(topPanel, BorderLayout.NORTH);
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(audioPanel, BorderLayout.CENTER);
		mainPanel.add(segmentPanel, BorderLayout.SOUTH);
		JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);
		/** start with a friendly greeting */
		ttsAction.actionPerformed(new ActionEvent(new JTextField(STARTUP_GREETING), 0, ""));
		editDialog = new EditDialog();
	}
	
	/** calls TTS */
	Action ttsAction = new AbstractAction("TTS!", new ImageIcon(VisualTTS.class.getResource("application-x-executable.png"))) {
		@Override
		public void actionPerformed(ActionEvent e) {
			String ttsText = ((JTextField) e.getSource()).getText();
			InputStream mbrola = maryAdapter.text2mbrola(ttsText);
			File f = fileChooser.getSelectedFile();
			String fileName = ttsText.replaceAll("[,.!\\?\\-\\+ ]", "").toLowerCase() + ".wav";
			fileChooser.setSelectedFile(new File(f != null ? f.getAbsolutePath() : null, fileName));
			setNewSegmentModel(SegmentModel.readFromStream(mbrola));
		}
	};
	
	/** allows manual editing of the segment model in mbrola format */
	Action editAction = new AbstractAction("Edit", new ImageIcon(VisualTTS.class.getResource("text-editor.png"))) {
		@Override
		public void actionPerformed(ActionEvent e) {
			String mbrola = segmentPanel.getSegmentModel().toString();
			editDialog.setText(mbrola);
			editDialog.setVisible();
			if (!editDialog.aborted())
				try {
					SegmentModel newSm = SegmentModel.readFromString(editDialog.getText());
					setNewSegmentModel(newSm);
				} catch(Throwable t) {
					// notify of error
					JOptionPane.showMessageDialog(null, "Problem parsing your input. Your changes will be discarded. Please try again.\nError message was: \n" + t.toString());
					// and try again
					editAction.actionPerformed(e);
				}
		}
	};
	
	/** resynthesizes */
	Action synthesisAction = new AbstractAction("synthesize") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String mbrola = segmentPanel.getSegmentModel().toString();
			AudioInputStream audio = maryAdapter.mbrola2audio(mbrola);
			audioPanel.setAudio(audio);
			playAction.actionPerformed(e);
		}
	};
	
	/** plays the current audio */
	Action playAction = new AbstractAction("Play", new ImageIcon(VisualTTS.class.getResource("media-playback-start.png"))) {
		@Override
		public void actionPerformed(ActionEvent e) {
			audioPanel.playAudio();
		}
	};
	
	/** changes the zoom level of the segment and audio panels */
	ChangeListener zoomAction = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			assert e.getSource() instanceof JSpinner : e;
			float zoom = ((Integer) ((JSpinner) e.getSource()).getValue()).floatValue() / 100.f;
			segmentPanel.setZoom(zoom);
			audioPanel.setZoom(zoom);
		}
	};
	
	private void setNewSegmentModel(SegmentModel segmentModel) {
		segmentPanel.setSegmentModel(segmentModel);
		synthesisAction.actionPerformed(null);
	}
	
	/** contains the buttons/textfields of the UI */
	private class TopPanel extends JPanel {
		TopPanel() {
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(2, 1, 2, 1);
			add(new JButton(loadMbrola), gbc);
			add(new JButton(saveMbrola), gbc);
			add(new JButton(saveAudio), gbc);
			gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5f;
			add(Box.createHorizontalGlue(), gbc);
			gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0f;
			add(new JLabel("Neuer Text: "), gbc);
			final JTextField newText = new JTextField(STARTUP_GREETING, 10);
			newText.setFont(DEFAULT_FONT);
			newText.addActionListener(ttsAction);
			gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1f;
			add(newText, gbc);
			gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0f;
			add(new JButton(new AbstractAction("TTS!", new ImageIcon(VisualTTS.class.getResource("application-x-executable.png"))) {
				@Override
				public void actionPerformed(ActionEvent ae) {
					newText.postActionEvent();
				}
			}), gbc);
			gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5f;
			add(Box.createHorizontalGlue(), gbc);
			gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0f;
			add(new JLabel("Zoom: "), gbc);
			Integer[] zoomLevels = new Integer[]{10, 20, 33, 50, 100, 200, 300, 400};
			JSpinner zoomSpinner = new JSpinner(new SpinnerListModel(zoomLevels));
			zoomSpinner.addChangeListener(zoomAction);
			zoomSpinner.setValue(zoomLevels[4]);
			add(zoomSpinner, gbc);
			add(new JLabel("%"), gbc);
			add(new JButton(playAction), gbc);
			add(new JButton(editAction), gbc);
		}
		
		private void setFileExtension(String ext) {
			File f = fileChooser.getSelectedFile();
			if (f != null) {
				String labName = f.getAbsoluteFile().toString().replaceAll("\\.(wav|lab)$", "." + ext);
				fileChooser.setSelectedFile(new File(labName));
			}
		}
		
		Action loadMbrola = new AbstractAction("", new ImageIcon(VisualTTS.class.getResource("document-import.png"))) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				setFileExtension("lab");
				int returnValue = fileChooser.showOpenDialog(null);
				File selectedFile = fileChooser.getSelectedFile();
				if (returnValue == JFileChooser.APPROVE_OPTION && selectedFile != null) {
					try {
						setNewSegmentModel(SegmentModel.readFromStream(new FileInputStream(selectedFile)));
					} catch (Exception e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(null, "Datei " + selectedFile.toString() + " kann nicht geladen werden.");
					}
				}
			}
		};
		
		Action saveMbrola = new AbstractAction("", new ImageIcon(VisualTTS.class.getResource("document-export.png"))) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				setFileExtension("lab");
				int returnValue = fileChooser.showSaveDialog(null);
				File selectedFile = fileChooser.getSelectedFile();
				if (returnValue == JFileChooser.APPROVE_OPTION && selectedFile != null) {
					if (selectedFile.exists()) {
						String messageText = "Soll die Datei " + selectedFile.toString() + " überschrieben werden?";
						returnValue = JOptionPane.showConfirmDialog(null, messageText, "Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					}
					if (!selectedFile.exists() || returnValue == JOptionPane.YES_OPTION) {
						try {
							segmentPanel.segmentModel.saveToFile(selectedFile);
						} catch (Exception e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(null, "Datei " + selectedFile.toString() + " konnte nicht geschrieben werden.");
						}
					}
				}
			}
		};
		
		Action saveAudio = new AbstractAction("", new ImageIcon(VisualTTS.class.getResource("save-audio.png"))) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				setFileExtension("wav");
				int returnValue = fileChooser.showSaveDialog(null);
				File selectedFile = fileChooser.getSelectedFile();
				if (returnValue == JFileChooser.APPROVE_OPTION && selectedFile != null) {
					if (selectedFile.exists()) {
						String messageText = "Soll die Datei " + selectedFile.toString() + " überschrieben werden?";
						returnValue = JOptionPane.showConfirmDialog(null, messageText, "Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					}
					if (!selectedFile.exists() || returnValue == JOptionPane.YES_OPTION) {
						try {
							String mbrola = segmentPanel.getSegmentModel().toString();
							maryAdapter.mbrola2file(mbrola, selectedFile);
						} catch (Exception e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(null, "Datei " + selectedFile.toString() + " konnte nicht geschrieben werden.");
						}
					}
				}			
			}
		};

	}
	
	private static void createAndShowGUI() {
		JFrame frame = new JFrame("GooeyMary");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		JPanel mainPanel = new VisualTTS();
		frame.setContentPane(mainPanel);
		// Display the window.
        frame.pack();
        frame.setMaximizedBounds(new Rectangle(new Dimension(Integer.MAX_VALUE, frame.getHeight())));
		frame.setVisible(true);
	}

	/**
	 * @param args are ignored
	 */
	public static void main(String[] args) {
		System.setProperty("inpro.tts.voice", "de6");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}