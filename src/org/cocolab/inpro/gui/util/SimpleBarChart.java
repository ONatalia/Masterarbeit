package org.cocolab.inpro.gui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SimpleBarChart extends JPanel {

	Bar[] bars;
	double maxValue;
	
	SimpleBarChart() {
		super();
		maxValue = 1.0;
		setPreferredSize(new Dimension(600, 400));
	}
	
	void setMaxValue(double mv) {
		maxValue = mv;
	}
	
	void setBars(Bar[] bars) {
		this.bars = bars;
		repaint();
	}
	
	/*
	 * set new bars with the given values and labels
	 */
	void setBars(double[] values, String[] labels) {
		assert (values.length == labels.length);
		Bar[] newBars = new Bar[values.length];
		for (int i = 0; i < newBars.length; i++) {
			newBars[i] = new Bar(values[i], labels[i]);
		}
		setBars(newBars);
	}
	
	/*
	 * set new bars with the given values, create default labels
	 */
	void setBars(double[] values) {
		Bar[] newBars = new Bar[values.length];
		for (int i = 0; i < newBars.length; i++) {
			newBars[i] = new Bar(values[i], Integer.toString(i + 1));
		}
		setBars(newBars);
	}
	
	/*
	 * update the values of the bars (keep the labels)
	 */
	void updateBars(double[] values) {
		assert (values.length == bars.length);
		for (int i = 0; i < values.length; i++) {
			bars[i].value = values[i];
		}
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setFont(new Font("SansSerif", Font.BOLD, 20));
		if (bars != null) {
			Dimension dim = this.getSize();
			int barWidth =  dim.width / bars.length;
			for (int i = 0; i < bars.length; i++) {
				int height = (int) ((bars[i].value / maxValue) * dim.height); 
				g.setColor(Color.RED);
				g.fillRect(barWidth * i + 2, dim.height - height, barWidth - 4, height);
				g.setColor(Color.BLACK);
				g.drawString(bars[i].label, barWidth * i + 2, dim.height - 5);
			}
		}
	}
	
	class Bar {
		double value;
		String label;
		
		Bar(double v) {
			value = v;
			label = "";
		}
		
		Bar(double v, String l) {
			value = v;
			label = l;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f = new JFrame("Bar Chart");
		SimpleBarChart sbc = new SimpleBarChart();
		f.add(sbc);
		f.pack();
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		double[] values = {.1, .2, .3, .4};
		sbc.setBars(values);
		try {
			Thread.sleep(800);
			values = new double[] {.5, .3, .8, .2};
			sbc.updateBars(values);
			Thread.sleep(400);
			String[] labels = {"eins", "zwei", "drei", "vier"};
			sbc.setBars(values, labels);
			Thread.sleep(800);
			values = new double[] {.1, .2, .3, .4};
			sbc.updateBars(values);
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}