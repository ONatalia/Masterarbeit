package org.cocolab.inpro.domains.calendar;

import java.util.Random;

import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;

import scalendar.adaptionmanager.AdaptionManager;

public class NoiseThread extends Thread {
	private Random random = new Random(); 
	private SynthesisModule sm;
	private IUUpdateListener updateListener;
	private AdaptionManager am;
	
	private int noiseLength = 1000;
	private int responsiveness = 50;
	
	public NoiseThread(AdaptionManager am, SynthesisModule sm, IUUpdateListener updateListener) {
		super("Noise Thread");
		this.am = am;
		this.sm = sm;
		this.updateListener = updateListener;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(randomIntBetween(2000, 5000));
				System.out.println("BRRRRRRRRRRRRRRRUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUMMMMMMMMMMMMMM!");
				//String pathToFile = "file:///Users/hendrik/Desktop/iNLG_iSS/noise/pinknoise.";
				String pathToFile = "file:/home/timo/uni/experimente/050_itts+inlg/noise/pinknoise.";
				//String fileSuffix = "ms.-3db.wav";
				String fileSuffix = "ms.wav";

				// CONDITION A: totally ignore noise 
				/*sm.playNoiseDeaf(pathToFile + new Integer(noiseLength).toString() + fileSuffix);
				Thread.sleep(noiseLength - responsiveness); /**/

				// CONDITION B: stop & continue audio stream
				/*sm.playNoiseDumb(pathToFile + new Integer(noiseLength).toString() + fileSuffix);
				Thread.sleep(noiseLength - responsiveness); /**/

				// CONDITION C: stop after current word, regenerate & resynthesize
				sm.playNoiseSmart(pathToFile + new Integer(noiseLength).toString() + fileSuffix);
				Thread.sleep(noiseLength - responsiveness);
				am.noGrounding(); 
				am.setLevelOfUnderstanding(5);
				am.setVerbosityFactor(0);
				updateListener.update(null); /**/

			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
	int randomGaussianIntBetween(int min, int max, int mean, int sd) {
		assert(min < max);
		double g;
		do {
			g = random.nextGaussian() * sd + mean;
		} while (g < min || g > max);
		return (int) Math.round(g);
	}
	
	private int randomIntBetween(int min, int max) {
		assert(min < max);
		min *= -1;
		return random.nextInt(min + max) - min;
	}
	
}
