package org.cocolab.inpro.greifarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cocolab.inpro.apps.SimpleReco;
import org.cocolab.inpro.incremental.deltifier.ASRWordDeltifier;
import org.cocolab.inpro.incremental.processor.CurrentASRHypothesis;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class GreifarmExperiment implements DropListener {

	private static final Logger logger = Logger.getLogger(GreifarmExperiment.class);

	private static final int MAX_GAMES = 5;
	
	Random random;
	CurrentASRHypothesis casrh;
	List<ASRWordDeltifier> deltifiers;
	RecoRunner rr;
	GreifarmActor ga;
	GreifarmController gc;
	
	int gameCount = 0;
	
	public GreifarmExperiment(ConfigurationManager cm) {
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
		SimpleReco.setupMicrophoneWithEndpointing(cm);
		ga = (GreifarmActor) cm.lookup("greifarmActor");
		ga.greifarmController.dropListener = this;
		casrh = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
		deltifiers = new ArrayList<ASRWordDeltifier>(3);
		deltifiers.add((ASRWordDeltifier) cm.lookup("none"));
		deltifiers.add((ASRWordDeltifier) cm.lookup("smoothing"));
		deltifiers.add((ASRWordDeltifier) cm.lookup("adaptivesmoothing"));
		random = new Random();
		rr = new RecoRunner(recognizer);
		(new Thread(rr)).start();
		rr.setInRecoMode(true);
		JOptionPane.showMessageDialog(null, "\n\n\n\n\n\n\n\n\n\n\n\n\n\n                                                      Neues Spiel, neues Glück.                                                      \n\n\n\n\n\n\n\n\n\n\n\n\n\n");
	}
	
	private class RecoRunner implements Runnable {

		private Recognizer recognizer;
		private boolean inRecoMode;
		
		RecoRunner(Recognizer recognizer) {
			this.recognizer = recognizer;
			inRecoMode = false;
		}
		
		public synchronized void setInRecoMode(boolean inRecoMode) {
			this.inRecoMode = inRecoMode;
		}
		
		@Override
		public void run() {
    		System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    		while(true) {
    			if (inRecoMode) {
        			recognizer.recognize();    				
    			} else {
    				sleep();
    			}
    		}
		}
		
		private void sleep() {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		
	}
	
	private void setRandomSmoothingStyle() {
		rr.setInRecoMode(false);
		int randomIndex = random.nextInt(deltifiers.size());
		
		ASRWordDeltifier newDeltifier = deltifiers.get(randomIndex);
		logger.info("setting deltifier " + newDeltifier);
		casrh.setDeltifier(newDeltifier);
		rr.setInRecoMode(true);		
	}
	
	@Override
	public void notifyDrop(GameScore gameScore) {
		gameCount++;
		if (gameCount > MAX_GAMES) {
			gameCount = 0;
			setRandomSmoothingStyle();
			JOptionPane.showMessageDialog(null, "\n\n\n\n\n\n\n\n\n\n\n\n\n\n                                                      Neues Spiel, neues Glück.                                                      \n\n\n\n\n\n\n\n\n\n\n\n\n\n");
			gameScore.reset();
			ga.greifarmController.reset();
			ga.processorReset();
		}
	}

	
	/**
	 * @param ignores all parameters
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure(GreifarmExperiment.class.getResource("log4j.properties"));
    	ConfigurationManager cm = new ConfigurationManager(GreifarmExperiment.class.getResource("greifarmconfig.xml"));
		new GreifarmExperiment(cm);
	}

}
