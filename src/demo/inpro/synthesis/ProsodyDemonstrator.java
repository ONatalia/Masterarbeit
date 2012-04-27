package demo.inpro.synthesis;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.IncrSysInstallmentIU;
import inpro.incremental.unit.SysSegmentIU;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Hashtable;

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
		generatedText.setText("Nimm bitte das Kreuz ganz oben links in der Ecke, lege es in den Fuß des Elefanten bevor Du ihn auf den Kopf drehst.");

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
			}
		}));
		BoundedRangeModel tempoRange = new DefaultBoundedRangeModel(0, 0, -100, 100);
		this.add(new JLabel("tempo:"));
		JSlider tempoSlider = new JSlider(tempoRange);
		this.add(tempoSlider);
		Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
		labels.put(-100, new JLabel("0.5"));
		labels.put(-50, new JLabel("0.7"));
		labels.put(0, new JLabel("1.0"));
		labels.put(50, new JLabel("1.4"));
		labels.put(100, new JLabel("2.0"));
		tempoSlider.setLabelTable(labels);
		tempoSlider.setPaintLabels(true);
		tempoSlider.setPaintTicks(true);
		tempoSlider.setMajorTickSpacing(100);
		tempoSlider.setMinorTickSpacing(50);
		final JTextField sliderValue = new JTextField(4); 
		sliderValue.setText("1.00");
		//this.add(sliderValue);
		tempoRange.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
				double factor = brm.getValue();
				//System.err.print(value + " -> ");
				factor /= 100; // normalize to [-1;+1]
				factor = Math.exp(factor * Math.log(2));
				//System.err.println(value);
				for (SysSegmentIU seg : installment.getSegments()) {
					seg.stretchFromOriginal(factor);
				}
				sliderValue.setText(String.format("%.2f", factor));
			}
		});
		this.add(new JLabel("pitch:"));
		BoundedRangeModel pitchRange = new DefaultBoundedRangeModel(0, 0, -1200, 1200);
		JSlider pitchSlider = new JSlider(pitchRange);
		this.add(pitchSlider);
		labels = new Hashtable<Integer, JComponent>();
		labels.put(-1200, new JLabel("-12"));
		labels.put(-600, new JLabel("-6"));
		labels.put(0, new JLabel("0"));
		labels.put(600, new JLabel("+6"));
		labels.put(1200, new JLabel("+12"));
		pitchSlider.setLabelTable(labels);
		pitchSlider.setPaintLabels(true);
		pitchSlider.setPaintTicks(true);
		pitchSlider.setMajorTickSpacing(1200);
		pitchSlider.setMinorTickSpacing(600);
		pitchRange.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
				double offset = brm.getValue();
				for (SysSegmentIU seg : installment.getSegments()) {
					seg.pitchShiftInCent = offset;
				}
			}
		});
	}
	
	@Override
	public void greatNewUtterance(String command) {
		installment = new IncrSysInstallmentIU(Collections.<String>singletonList(command));
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
