package org.cocolab.inpro.domains.carchase;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.tts.MaryAdapter;
import org.yaml.snakeyaml.Yaml;

public class CarChaseExperimenter {
	
	private static Logger logger = Logger.getLogger("CarChaseExperimenter");
	
	Articulator articulator;
	DispatchStream dispatcher;
	CarChaseViewer maptaskviewer;
	JFrame frame;
	
	private static long globalTimeOffsetMS;

	CarChaseExperimenter() {
		dispatcher = SimpleMonitor.setupDispatcher();
//		articulator = new StandardArticulator(dispatcher);
		articulator = new IncrementalArticulator(dispatcher);
		setupGUI();
	}

	private void precompute(List<Action> actions) {
		for (Action a : actions) {
			if (a instanceof TTSAction) {
				articulator.precompute((TTSAction) a);
			} else if (a instanceof WorldAction) {
				maptaskviewer.precompute((WorldAction) a);
			}
		}
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
		logger.info("Now dispatching " + action.getClass().getSimpleName() + " " + action.toString() + " on " + getGlobalTime());
		if (action instanceof TTSAction) {
			articulator.say((TTSAction) action);
		} else if (action instanceof WorldStartAction) {
			maptaskviewer.execute((WorldStartAction) action);
			logger.info("resetting runtime offset");
			globalTimeOffsetMS = System.currentTimeMillis();
		} else if (action instanceof WorldAction) {
			maptaskviewer.execute((WorldAction) action);
		} else if (action instanceof ShutdownAction) {
			dispatcher.shutdown();
			frame.dispose();
		}
		logger.info("Done dispatching " + action.getClass().getSimpleName() + " " + action.toString() + " on " + getGlobalTime());
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
//		List<Action> actionList = Arrays.<Action>asList(
//				//new WorldAction(10, new Point(250, 670), 0),
//				new WorldAction(50, new Point(250, 430), 3000), 
//				new TTSAction( 100, "Das Auto fährt durch die A-Straße."),
//				new TTSAction(2000, "Das Auto fährt durch die B-Straße."),
//				new ShutdownAction(10000)
//		);
		Yaml yaml = new MyYaml();
		List<Action> actions = new ArrayList<Action>();
		WorldAction prevWorldAction = null;
		for (Object a : yaml.loadAll(CarChaseViewer.class.getResourceAsStream("config2"))) {
			assert a instanceof Action;
			Action action = (Action) a;
			if (action instanceof WorldAction) {
				if (action.start == -1) {
					assert prevWorldAction != null;
					action.start = prevWorldAction.getEnd();
					logger.info("filling in start time for " + action);
				} else {
					assert prevWorldAction == null || action.start >= prevWorldAction.getEnd() : action.toString() + prevWorldAction.toString(); // the first disjunctor necessary for startup
				}
				prevWorldAction = (WorldAction) action;
			}
			actions.add(action);
		}
		Collections.sort(actions, new Comparator<Action>() {
			@Override
			public int compare(Action o1, Action o2) {
				return o1.start - o2.start;
			}
		});
//		System.out.println(yaml.dumpAll(actionList.iterator()));
		logger.info("pre-computing system utterances");
		exp.precompute(actions);
		logger.info("starting to run");
		globalTimeOffsetMS = System.currentTimeMillis();
		exp.execute(actions);
	}
	
	/** an action for the world or for iTTS */
	public static class Action {
		int start; // in milliseconds
		public Object appData;
		public Action() {}
		Action(int t) { this.start = t; }
		public int getStart() { return start; }
		public void setStart(int t) { start = t; }
	}
	
	public static class ShutdownAction extends Action { 
		public ShutdownAction(int t) { super(t); }
		public ShutdownAction() {}
		@Override
		public String toString() {
			return "ShutdownAction at " + start;
		}
	}
	
	public static class WorldStartAction extends WorldAction {
		double angle;
		public WorldStartAction() { setStart(0); }
		public void setAngle(double d) { angle = d; }
	}
	
	public static class WorldAction extends Action {
		int duration; // in milliseconds
		Point target;
		public WorldAction() { super(-1); }
		WorldAction(int t, Point p, int d) {
			super(t);
			target = p;
			duration = d;
		}
		public int getDuration() { return duration; }
		public void setDuration(int duration) { this.duration = duration; }
		public int getEnd() { return start + duration; }
		public void setTarget(Point p) { target = p; }
		public Point getTarget() { return target; } 
		@Override
		public String toString() {
			return "world: t=" + start + ", duration: " + duration + ", target " + target.toString();
		}
	}
	
	public static class TTSAction extends Action {
		String text; // in the simplest case: text to synthesize
		Object cont; // possible continuation
		public TTSAction() {}
		TTSAction(int t, String content) {
			super(t);
			this.text = content;
		}
		public String getText() { return text; }
		public void setText(String t) { text = t; }
		public void setTryCont(String t) { cont = t; }
		@Override
		public String toString() { return "TTS: t=" + start + ", " + text; }
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
		/** by default, their is no precomputation */
		public void precompute(@SuppressWarnings("unused") TTSAction action) {}
		/** a way for an articulator to ask for the current time */
		public final static int getGlobalTime() {
			return (int) (System.currentTimeMillis() - globalTimeOffsetMS);
		}
	}
}
