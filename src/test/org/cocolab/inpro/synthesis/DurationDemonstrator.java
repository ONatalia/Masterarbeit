package test.org.cocolab.inpro.synthesis;

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

import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;

public class DurationDemonstrator extends PatternDemonstrator {
	
	public DurationDemonstrator() {
		final JTextField textToSynthesize = new JTextField("Nimm bitte das Kreuz ganz oben links in der Ecke, lege es in den Fuß des Elefanten bevor Du ihn auf den Kopf drehst.");
		this.add(textToSynthesize);
		this.add(new JButton(new AbstractAction("", new ImageIcon(DurationDemonstrator.class.getResource("media-playback-start.png"))) {
			@Override
			public void actionPerformed(ActionEvent e) {
				greatNewUtterance(textToSynthesize.getText());
		        dispatcher.playStream(installment.getAudio(), true);
			}
		}));
		BoundedRangeModel brm = new DefaultBoundedRangeModel(0, 0, -100, 100);
		final JSlider slider = new JSlider(brm);
		this.add(slider);
		Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
		labels.put(-100, new JLabel("0.5"));
		labels.put(-50, new JLabel("0.7"));
		labels.put(0, new JLabel("1.0"));
		labels.put(50, new JLabel("1.4"));
		labels.put(100, new JLabel("2.0"));
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(50);
		slider.setMinorTickSpacing(25);
		final JTextField sliderValue = new JTextField(4); 
		sliderValue.setText("1.00");
		this.add(sliderValue);
		brm.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
			//	if (!brm.getValueIsAdjusting()) {
					double factor = brm.getValue();
					//System.err.print(value + " -> ");
					factor /= 100; // normalize to [-1;+1]
					factor = Math.exp(factor * Math.log(2));
					//System.err.println(value);
					for (SysSegmentIU seg : installment.getSegments()) {
						seg.stretchFromOriginal(factor);
					}
					sliderValue.setText(String.format("%.2f", factor));
			//	}
			}
		});
	}
	
	@Override
	public void greatNewUtterance(String command) {
		installment = new IncrSysInstallmentIU(Collections.<String>singletonList(command));
		System.err.println("created a new installment: " + command);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(new DurationDemonstrator());
			}
		});
	}


}
