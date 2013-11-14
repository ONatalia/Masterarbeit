package done.inpro.system.calendar;

import inpro.incremental.unit.IU.IUUpdateListener;

import java.io.InputStream;
import java.util.Random;

import org.soa.incremental.nlg.adaptionmanager.AdaptionManager;

public class NoiseThread extends Thread {
	private Random random = new Random(); 
	private NoisySynthesisModule sm;
	private IUUpdateListener updateListener;
	private AdaptionManager am;
	
	private int noiseLength = 1000; // in milliseconds
	private int responsiveness = 50; // in milliseconds
	
	NoiseHandling noiseHandling;
	
	enum NoiseHandling { 
		none, // condition A in SigDial paper: totally ignore noise
		pauseStream, // condition B in SigDial paper: stop & continue audio stream
		regenerate; // condition C in SigDial paper: stop after current word, regenerate & resynthesize

		public static NoiseHandling parseParam(String param) {
			if (param.equals("conditionA")) return none;
			if (param.equals("conditionB")) return pauseStream;
			if (param.equals("conditionC")) return regenerate;
			throw new RuntimeException("illegal parameter for noise handling");
		}
	}
	
	public NoiseThread(AdaptionManager am, NoisySynthesisModule sm, IUUpdateListener updateListener, NoiseHandling nh) {
		super("Noise Thread");
		this.am = am;
		this.sm = sm;
		this.updateListener = updateListener;
		this.noiseHandling = nh;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(randomIntBetween(2000, 5000));
				System.out.println("BRRRRRRRRRRRRRRRUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUMMMMMMMMMMMMMM!");
				//String pathToFile = "file:///Users/hendrik/Projects/iNLG_iSS/noise/pinknoise.";
				//String pathToFile = "file:/home/timo/uni/experimente/050_itts+inlg/noise/pinknoise.";
				//String fileSuffix = "ms.-3db.wav";
				String fileSuffix = "ms.wav";
				InputStream noiseFile = NoiseThread.class.getResourceAsStream("noise/pinknoise." + Integer.toString(noiseLength) + fileSuffix);
				
				switch (noiseHandling) {
				case none: // CONDITION A
					sm.playNoiseDeaf(noiseFile);
					Thread.sleep(noiseLength - responsiveness);
					break;
				case pauseStream: // CONDITION B
					sm.playNoiseDumb(noiseFile);
					Thread.sleep(noiseLength - responsiveness);
					break;
				case regenerate: // CONDITION C
					sm.playNoiseSmart(noiseFile);
					Thread.sleep(noiseLength - responsiveness);
					am.noGrounding(); 
					am.setLevelOfUnderstanding(5);
					am.setVerbosityFactor(0);
					updateListener.update(null);
					break;
				}
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
