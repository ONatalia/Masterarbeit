package demo.inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.hts.LoudnessPostProcessor;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class ProsodyDemonstrator extends PatternDemonstrator {
	
	public ProsodyDemonstrator() {
		generatedText.setPreferredSize(new JTextField(52).getPreferredSize());
		//generatedText.setText("Press the play button to synthesize this utterance."); 
		generatedText.setEditable(true);
		String synText = System.getProperty("inpro.tts.demo.longUtt", "Nimm bitte das Kreuz ganz oben links in der Ecke, lege es in den Fuss des Elefanten bevor Du ihn auf den Kopf drehst.");
		generatedText.setText(synText);
		final BoundedRangeModel tempoRange = new DefaultBoundedRangeModel(0, 0, -100, 100);
		final BoundedRangeModel pitchRange = new DefaultBoundedRangeModel(0, 0, -1200, 1200);
		final BoundedRangeModel loudnessRange = new DefaultBoundedRangeModel(0, 0, -100, 100);
		tempoRange.addChangeListener(tempoChangeListener);
		pitchRange.addChangeListener(pitchChangeListener);
		loudnessRange.addChangeListener(loudnessChangeListener);
		this.add(generatedText);
		this.add(new JButton(new AbstractAction("", new ImageIcon(ProsodyDemonstrator.class.getResource("media-playback-start.png"))) {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.err.println(generatedText.getText());
				String html = generatedText.getText();
				String txt = html.replaceAll("<.*?>", "").replaceAll("^[\\n\\w]+", "").replaceAll("[\\n\\w]+$", "");
				System.err.println(txt);
				greatNewUtterance(txt);
		        dispatcher.playStream(installment.getAudio(), true);
		        for (SysSegmentIU seg : installment.getSegments()) {
		        	seg.setVocodingFramePostProcessor(loudnessPostProcessor);
		        }
		        tempoRange.setValue(0);
		        pitchRange.setValue(0);
		        loudnessRange.setValue(0);
			}
		}));
		this.addLabel("tempo:");
		this.add(createSlider(tempoRange, "0.5", "0.7", "1.0", "1.4", "2.0"));
		this.addLabel("pitch:");
		this.add(createSlider(pitchRange, "-12", "-6", "0", "+6", "+12"));
		this.addLabel("voice:");
		this.add(createSlider(loudnessRange, "", "softer", "", "stronger", ""));
	}
	
	/** add a JLabel with the given string to this component */ 
	private void addLabel(String label) { this.add(new JLabel(label)); }

	/** create a slider for a given rangeModel with equidistant labels */ 
	private static JSlider createSlider(BoundedRangeModel rangeModel, String... labels) {
		JSlider slider = new JSlider(rangeModel);
		int min = rangeModel.getMinimum();
		int max = rangeModel.getMaximum();
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		int range = max - min;
		slider.setMajorTickSpacing(range / 2);
		slider.setMinorTickSpacing(range / 4);
		slider.setLabelTable(createLabelTable(min, max, labels));
		return slider;
	}
	
	/** create a hashtable of position/label pairs, equidistantly spaced between min and max */
	private static Hashtable<Integer, JComponent> createLabelTable(int min, int max, String... labels) {
		Hashtable<Integer, JComponent> labelHash = new Hashtable<Integer, JComponent>();
		int increment = (max - min) / (labels.length - 1);
		for (String label : labels) {
			labelHash.put(min, new JLabel(label));
			min += increment;
		}
		return labelHash;
	}
	
	private static int getSourceValue(ChangeEvent e) {
		BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
		return brm.getValue();
	}
	
	/**
	 * sets the pitchShiftInCent value of every segment in the utterance to the value indicated by the slider-model
	 */
	final ChangeListener pitchChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			double offset = getSourceValue(e);
			for (SysSegmentIU seg : getSegments()) {
				seg.pitchShiftInCent = offset;
			}
		}
	};
	
	/**
	 * stretches every segment in the utterance with the value indicated by the slider-model
	 */
	final ChangeListener tempoChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			double factor = getSourceValue(e) / 100f; // normalize to [-1;+1]
			factor = Math.exp(factor * Math.log(2)); // convert to [-.5;2]
			for (SysSegmentIU seg : getSegments()) {
				seg.stretchFromOriginal(factor);
			}
		}
	};
	
	final LoudnessPostProcessor loudnessPostProcessor = new LoudnessPostProcessor();
	final ChangeListener loudnessChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			loudnessPostProcessor.setLoudness(getSourceValue(e));
		}
	};
	
	/** return the segments in the ongoing utterance (if any) */ 
	private List<SysSegmentIU> getSegments() {
		if (installment != null)
			return installment.getSegments();
		else
			return Collections.<SysSegmentIU>emptyList();
	}
	
	@Override
	public void greatNewUtterance(String command) {
		installment = new TreeStructuredInstallmentIU(Collections.<String>singletonList(command));
		for (IU word : installment.groundedIn()) {
			word.updateOnGrinUpdates();
			word.addUpdateListener(iuUpdateRepainter);
		}
		System.err.println("created a new installment: " + command);
	}
	
	@Override
	public String applicationName() {
		return "Prosody Demonstrator";
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(new ProsodyDemonstrator());
			}
		});
	}

}
