package org.cocolab.inpro.greifarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cocolab.inpro.apps.SimpleReco;
import org.cocolab.inpro.gui.util.SpeechStateVisualizer;
import org.cocolab.inpro.incremental.deltifier.ASRWordDeltifier;
import org.cocolab.inpro.incremental.processor.CurrentASRHypothesis;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class GreifarmExperiment implements DropListener {

	private static final Logger logger = Logger.getLogger(GreifarmExperiment.class);

	private static final int MAX_GAMES = 3;
	
	Random random;
	CurrentASRHypothesis casrh;
	List<ASRWordDeltifier> deltifiers;
	RecoRunner rr;
	GreifarmActor ga;
	GreifarmController gc;
	GameScore gameScore;
	SpeechStateVisualizer ssv;
	
	int gameCount = 0;
	
	public GreifarmExperiment(ConfigurationManager cm) {
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
		SimpleReco.setupMicrophoneWithEndpointing(cm);
		ssv = (SpeechStateVisualizer) cm.lookup("speechStateVisualizer");
		ga = (GreifarmActor) cm.lookup("greifarmActor");
		ga.greifarmController.dropListener = this;
		gameScore = ga.gameScore;
		casrh = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
		deltifiers = new ArrayList<ASRWordDeltifier>(3);
		deltifiers.add((ASRWordDeltifier) cm.lookup("none"));
		deltifiers.add((ASRWordDeltifier) cm.lookup("smoothing"));
		deltifiers.add((ASRWordDeltifier) cm.lookup("adaptivesmoothing"));
		random = new Random();
		rr = new RecoRunner(recognizer);
		(new Thread(rr)).start();
		showDialog();
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
		int randomIndex = random.nextInt(deltifiers.size());
		ASRWordDeltifier newDeltifier = deltifiers.get(randomIndex);
		logger.info("setting deltifier " + newDeltifier);
		casrh.setDeltifier(newDeltifier);
	}
	
	/** only call this on the Swing Thread! */
	void showDialog() {
		ssv.setRecording(false);
		rr.setInRecoMode(false);
		String lineBreaks = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
		String whiteSpace = "                                                     ";
		whiteSpace = whiteSpace + whiteSpace + whiteSpace;
		JOptionPane.showMessageDialog(null,
				lineBreaks + whiteSpace + 
				"Neues Spiel, neues Glück." +
				whiteSpace + lineBreaks);
		nextRound();
		rr.setInRecoMode(true);
		ssv.setRecording(true);
	}
	
	void nextRound() {
		setRandomSmoothingStyle();
		gameScore.reset();
		ga.greifarmController.reset();
		ga.processorReset();		
	}
	
	@Override
	public void notifyDrop(final GameScore gameScore) {
		gameCount++;
		logger.debug("gameCount is now " + gameCount);
		if (gameCount >= MAX_GAMES) {
			gameCount = 0;
			try {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						showDialog();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					nextRound();
				}				
			}).start();
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
