package org.cocolab.inpro.pitch.notifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

@SuppressWarnings("serial")
public class ProsodyMonitor extends JPanel implements SignalFeatureListener {

	private static final int DATA_POINTS = 70;
	
	private static final int SPACING = 3;
	
	private static final int HEIGHT = 150;
	
	final List<Double> powerValues = new ArrayList<Double>();
	Range powerRange = new Range();
	
	final List<Double> pitchValues = new ArrayList<Double>();
	Range pitchRange = new Range();
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		final ProsodyMonitor self = this; // weird masking of "this" in the Runnable below
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame f = new JFrame("prosody monitor");
				f.add(self);
				f.pack();
				f.setVisible(true);
			}
		});
	}

	@Override
	public void reset() { 
		powerValues.clear();
		pitchValues.clear();
	}

	@Override
	public void newSignalFeatures(int frame, double power, boolean voicing,
			double pitch) {
		powerValues.add(power);
		powerRange.update(power);
		if (powerValues.size() > DATA_POINTS) {
			powerValues.remove(0);
		}
		if (voicing) {
			pitchValues.add(pitch);
			pitchRange.update(pitch);
		} else {
			pitchValues.add(null);
		}
		if (pitchValues.size() > DATA_POINTS) {
			pitchValues.remove(0);
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.BLUE);
		for (int i = 0; i < pitchValues.size(); i++) {
			if (pitchValues.get(i) != null) {
				int x = i * SPACING;
				int y = pitchRange.toScreenCoordinate(pitchValues.get(i));
				g.drawOval(x - 1, y - 1, 2, 2);
			}
		}
		g.setColor(Color.RED);
		for (int i = 0; i < powerValues.size(); i++) {
			int x = i * SPACING;
			int y = powerRange.toScreenCoordinate(powerValues.get(i));
			g.drawRect(x - 1, y - 1, 2, 2);
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(DATA_POINTS * SPACING, HEIGHT);
		
	}
	
	class Range {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		void update(List<Double> values) {
			for (double d : values) {
				min = Math.min(d, min);
				max = Math.max(d, max);
			}
		}
		
		void update(double d) {
			min = Math.min(d, min);
			max = Math.max(d, max);
		}
		
		int toScreenCoordinate(double d) {
			return HEIGHT - (int) (d * ((double) HEIGHT) / (max - min));
		}
	}
	
}
