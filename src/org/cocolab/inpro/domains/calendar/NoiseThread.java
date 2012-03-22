package org.cocolab.inpro.domains.calendar;

import java.util.Random;

import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;

import scalendar.adaptionmanager.AdaptionManager;

public class NoiseThread extends Thread {
	private Random random = new Random(); 
	private SynthesisModule sm;
	private IUUpdateListener updateListener;
	private AdaptionManager am;
	
	private int delay = 1300;
	private int noiseLength = 1000;
	private int responsiveness = 50;
	
	public NoiseThread(AdaptionManager am, SynthesisModule sm, IUUpdateListener updateListener) {
		this.am = am;
		this.sm = sm;
		this.updateListener = updateListener;
	}
	
	public void setTiming(int delay) {
		this.delay = delay;
	}
	
	public void setTiming(int delay, int length) {
		this.delay = delay;
		this.noiseLength = length;
	}
	
	public void run() {
		int i = 0;
		while (true) {
			i++;
		try {
			Thread.sleep(randomIntBetween(2000, 7000));
			System.out.println("BRRRRRRRRRRRRRRRUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUMMMMMMMMMMMMMM!");
			sm.playNoise("file:///Users/hendrik/Desktop/iNLG_iSS/pinknoise." + new Integer(noiseLength).toString() + "ms.wav");
			Thread.sleep(noiseLength - responsiveness);
		} catch (InterruptedException e) {e.printStackTrace();}
		am.noGrounding();
		am.setLevelOfUnderstanding(5);
		am.setVerbosityFactor(0);
		updateListener.update(null);
		}
	}

	
	private int randomIntBetween(int min, int max) {
		assert(min < max);
		min *= -1;
		return random.nextInt(min + max) - min;
	}
	
}
