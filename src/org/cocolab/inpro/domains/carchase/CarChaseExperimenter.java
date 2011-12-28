package org.cocolab.inpro.domains.carchase;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.tts.MaryAdapter;
import org.yaml.snakeyaml.Yaml;

public class CarChaseExperimenter {
	
	private static Logger logger = Logger.getLogger(CarChaseExperimenter.class);
	
	Articulator articulator;
	DispatchStream dispatcher;
	CarChaseViewer maptaskviewer;
	JFrame frame;
	
	private static long globalTimeOffsetMS;

	CarChaseExperimenter() {
		dispatcher = SimpleMonitor.setupDispatcher();
		articulator = new StandardArticulator(dispatcher);
		setupGUI();
	}

	private void execute(List<? extends Action> aList) {
		for (Action a : aList)
			execute(a);
	}

	private void execute(Action action) {
		int wait = action.start - getGlobalTime();
		if (wait < 0) {
			logger.warn("I'm behind schedule by " + wait);
		} else {
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				logger.warn("I couldn't sleep for " + wait);
			}
		}
		logger.info("Executing " + action.getClass().getCanonicalName() + " " + action.toString() + " on " + getGlobalTime());
		if (action instanceof TTSAction) {
			articulator.say((TTSAction) action);
		} else if (action instanceof WorldAction) {
			maptaskviewer.execute((WorldAction) action);
		} else if (action instanceof ShutdownAction) {
			dispatcher.shutdown();
			frame.dispose();
		}
	}
	
	public static int getGlobalTime() {
		return (int) (System.currentTimeMillis() - globalTimeOffsetMS);
	}
	
	public void setupGUI() {
		maptaskviewer = new CarChaseViewer();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					frame = new JFrame("CarApp");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					// add our object
					frame.setContentPane(maptaskviewer);
					//Display the window.
			        frame.pack();
					frame.setVisible(true);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		MaryAdapter.getInstance();
		CarChaseExperimenter exp = new CarChaseExperimenter();
		/** startup mary */
		logger.info("starting to initialize");
		/** disable global variance optimization */
		//((HMMVoice) Voice.getVoice(MaryAdapter4internal.DEFAULT_VOICE)).getHMMData().setUseGV(false);
		globalTimeOffsetMS = System.currentTimeMillis();
		logger.info("starting to run");
//		List<Action> actionList = Arrays.<Action>asList(
//				//new WorldAction(10, new Point(250, 670), 0),
//				new WorldAction(50, new Point(250, 430), 3000), 
//				new TTSAction( 100, "Das Auto fährt durch die A-Straße."),
//				new TTSAction(2000, "Das Auto fährt durch die B-Straße."),
//				new ShutdownAction(10000)
//		);
		Yaml yaml = new MyYaml();
		List<Action> actions = new ArrayList<Action>();
		for (Object a : yaml.loadAll(CarChaseViewer.class.getResourceAsStream("config1"))) {
			assert a instanceof Action;
			actions.add((Action) a);
		}
//		System.out.println(yaml.dumpAll(actionList.iterator()));
		exp.execute(actions);
	}
	
	/** an action for the world or for iTTS */
	public static class Action {
		public Action() {}
		Action(int t) { this.start = t; }
		int start; // in milliseconds
		public int getStart() { return start; }
		public void setStart(int t) { start = t; }
	}
	
	public static class ShutdownAction extends Action { 
		public ShutdownAction(int t) { super(t); }
		public ShutdownAction() {}
	}
	
	public static class WorldAction extends Action {
		public WorldAction() { super(0); }
		WorldAction(int t, Point p, int d) {
			super(t);
			target = p;
			duration = d;
		}
		int duration; // in milliseconds
		Point target;
		public int getDuration() { return duration; }
		public void setDuration(int duration) { this.duration = duration; }
		public void setTarget(Point p) { target = p; }
		public Point getTarget() { return target; } 
	}
	
	public static class WorldStartAction extends WorldAction {
		public WorldStartAction() { 
			setStart(0);
		}
	}
	
	public static class TTSAction extends Action {
		public TTSAction() {}
		TTSAction(int t, String content) {
			super(t);
			this.text = content;
		}
		String text; // in the simplest case: text to synthesize
		public String getText() { return text; }
		public void setText(String t) { text = t; }
	}
	
	/**
	 * the public interface for articulators to implement 
	 * @author timo
	 */
	public abstract static class Articulator {
		protected DispatchStream dispatcher;
		public Articulator(DispatchStream dispatcher) {
			this.dispatcher = dispatcher;
		}
		public abstract void say(TTSAction action);
		/** a way for an articulator to ask for the current time */
		public final static int getGlobalTime() {
			return (int) (System.currentTimeMillis() - globalTimeOffsetMS);
		}
	}
}
