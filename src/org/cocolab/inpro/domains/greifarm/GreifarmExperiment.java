package org.cocolab.inpro.domains.greifarm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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

	private static final int MAX_GAMES = 1;
	
	Random random;
	private CurrentASRHypothesis casrh;
	private List<ASRWordDeltifier> deltifiers;
	private RecoRunner rr;
	private GreifarmActor ga;
	private GameScore gameScore;
	private SpeechStateVisualizer ssv;
	
	HashMap<ASRWordDeltifier, List<Score>> testResult = new HashMap<ASRWordDeltifier, List<Score>>();
	
	int gameCount = 0;
	
	public GreifarmExperiment(ConfigurationManager cm) {
    	SimpleReco simpleReco;
		try {
			simpleReco = new SimpleReco(cm);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		simpleReco.setupMicrophoneWithEndpointing();
		ssv = (SpeechStateVisualizer) cm.lookup("speechStateVisualizer");
		ga = (GreifarmActor) cm.lookup("greifarmActor");
		ga.greifarmController.dropListener = this;
		gameScore = ga.gameScore;
		casrh = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
		deltifiers = new ArrayList<ASRWordDeltifier>(3);
//		deltifiers.add((ASRWordDeltifier) cm.lookup("none"));
//		deltifiers.add((ASRWordDeltifier) cm.lookup("smoothing"));
		deltifiers.add((ASRWordDeltifier) cm.lookup("adaptivesmoothing"));
		logger.debug("i've got the following deltifiers to chose from: " + deltifiers);
		for (ASRWordDeltifier deltifier : deltifiers) {
			testResult.put(deltifier, new ArrayList<Score>());
		}
		random = new Random();
		rr = new RecoRunner(simpleReco.getRecognizer());
		(new Thread(rr, "recognizer thread")).start();
		showDialog();
		
		Runnable shutdownHook = new Runnable() {
			public void run() {
				showTestResult();
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "shutdown thread"));
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
    				try {
    					recognizer.recognize();
    				} catch (Throwable e) { // also AssertionErrors
    					e.printStackTrace();
    					logger.warn("Something's wrong further down, trying to continue anyway" , e);
    				}
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
	
	private ASRWordDeltifier getDeltifier() {
		return casrh.getDeltifier();
	}
	
	/** only call this on the Swing Thread! */
	void showDialog() {
		ssv.setRecording(false);
		rr.setInRecoMode(false);
		setRandomSmoothingStyle();
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
		gameScore.reset();
		ga.greifarmController.reset();
		ga.processorReset();		
	}
	
	void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void notifyDrop(final GameScore gameScore) {
		gameCount++;
		logger.debug("gameCount is now " + gameCount);
		logger.info("score is now " + gameScore.getCombinedScore());
		testResult.get(getDeltifier()).add(new Score(gameScore.time, gameScore.score, gameScore.getCombinedScore()));
		if (gameCount >= MAX_GAMES) {
			gameCount = 0;
			try {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						sleep(600);
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
					sleep(600);
					nextRound();
				}				
			}, "advance to next round with delay").start();
		}
	}


	private void showTestResult() {
		PrintStream out = System.out;
		out.println("Statistics for points earned: ");
		for (ASRWordDeltifier deltifier : testResult.keySet()) {
			out.println("Results for deltifier: " + deltifier);
			for (Score score : testResult.get(deltifier)) {
				out.print(score.score);
				out.print(" ");
			}
			out.println();
		}
		out.println("Statistics for time of the interaction: ");
		for (ASRWordDeltifier deltifier : testResult.keySet()) {
			out.println("Results for deltifier: " + deltifier);
			for (Score score : testResult.get(deltifier)) {
				out.print(score.time);
				out.print(" ");
			}
			out.println();
		}
		out.println("Statistics for combined score: ");
		for (ASRWordDeltifier deltifier : testResult.keySet()) {
			out.println("Results for deltifier: " + deltifier);
			for (Score score : testResult.get(deltifier)) {
				out.print(score.combinedScore);
				out.print(" ");
			}
			out.println();
		}		
	}

	private static class Score {
		static int nextIndex = 0;
		int time;
		int score;
		int combinedScore;
		@SuppressWarnings("unused")
		int index;
		Score(int time, int score, int combinedScore) {
			this.time = time;
			this.score = score;
			this.combinedScore = combinedScore;
			index = nextIndex();
		}
		private int nextIndex() {
			return nextIndex++;
		}
		
		
	}


	/**
	 * @param args ignores all arguments
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure(GreifarmExperiment.class.getResource("log4j.properties"));
    	ConfigurationManager cm = new ConfigurationManager(GreifarmExperiment.class.getResource("greifarmconfig.xml"));
		new GreifarmExperiment(cm);
	}
	
}
