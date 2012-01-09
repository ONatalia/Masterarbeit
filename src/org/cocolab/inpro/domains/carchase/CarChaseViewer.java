package org.cocolab.inpro.domains.carchase;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.cocolab.inpro.domains.carchase.CarChaseExperimenter.WorldAction;
import org.cocolab.inpro.domains.carchase.CarChaseExperimenter.WorldStartAction;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.swing.SwingRepaintTimeline;

public class CarChaseViewer extends JPanel {

	private static Logger logger = Logger.getLogger("CarChaseViewer");
	
	private final boolean PAINT_PATH = false;
	
	Image background;
	Image car;
	Point carPosition;
	double carAngle;
	double carTargetAngle;
	boolean carIsReverseGear = false;
	private static final double CAR_SCALE = 1f / 4.3f;
	Timeline timeline;
	Point targetPoint;
	
	public CarChaseViewer() {
		try {
			background = ImageIO.read(CarChaseViewer.class.getResource("map.png"));
			car = ImageIO.read(CarChaseViewer.class.getResource("car.png"));
			carPosition = new Point(0, 0);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void setCarPosition(Point p) {
		carPosition = p;
	}
	
	public void setCarAngle(double a) {
		carAngle = a;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(background, 0, 0, null);
		if (PAINT_PATH)
			paintPath(g);
		paintCar((Graphics2D) g);
		if (PAINT_PATH)
			g.fillOval(carPosition.x - 2, carPosition.y - 2, 4, 4);
	}
	
	List<Point> carPath = new ArrayList<Point>();
	Point prevCarPosition;
	private void paintPath(Graphics g) {
		if (!carPosition.equals(prevCarPosition)) {
			if (prevCarPosition != null)
				carPath.add(carPosition);
			prevCarPosition = carPosition;
		}
		if (!carPath.isEmpty()) {
			Iterator<Point> it = carPath.iterator();
			Point p1 = it.next();
			while(it.hasNext()) {
				Point p2 = it.next();
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
				p1 = p2;
			}
		}
	}
	
	private void paintCar(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		AffineTransform xform = new AffineTransform();
		xform.translate(carPosition.x - car.getWidth(null) / 2 * CAR_SCALE, carPosition.y - car.getHeight(null) / 2 * CAR_SCALE);
		xform.rotate(carAngle, car.getWidth(null) / 2 * CAR_SCALE, car.getHeight(null) / 2 * CAR_SCALE);
		xform.scale(CAR_SCALE, CAR_SCALE);
		g2.drawImage(car, xform, null);
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(1024, 768);
	}
	
	public void precompute(WorldAction action) {
		if (action instanceof WorldStartAction) {
			targetPoint = action.target;
			carPosition = new Point(action.target);
			carAngle = carTargetAngle = ((WorldStartAction) action).angle;
		} else {
			assert targetPoint != null;
			Point startPoint = targetPoint; // the start position is the previous' target
			targetPoint = action.target;
			Timeline timeline = new SwingRepaintTimeline(this);
			timeline.addPropertyToInterpolate("carPosition", startPoint, targetPoint);
//			carTargetAngle %= Math.PI;
			carAngle = carTargetAngle;
			carTargetAngle = action.isReverseGear() ? Math.atan2(startPoint.x - targetPoint.x, targetPoint.y - startPoint.y) :
													Math.atan2(targetPoint.x - startPoint.x, startPoint.y - targetPoint.y);
//			carTargetAngle %= Math.PI;
			if (Math.abs(carTargetAngle - carAngle) >= Math.PI || Math.abs(carAngle - carTargetAngle) >= Math.PI) {
				if (carTargetAngle < carAngle)
					carTargetAngle += 2 * Math.PI;
				else
					carAngle += 2 * Math.PI;
			}
			logger.info("action : " + action.toString() + " at speed (px/ms): " + (targetPoint.distance(startPoint) / action.duration));
			logger.debug("start angle: " + carAngle + " , target angle: " + carTargetAngle);
			timeline.addPropertyToInterpolate("carAngle", carAngle, carTargetAngle);
			timeline.setDuration(action.duration);
			action.appData = timeline;
		}
	}

	public void execute(WorldAction action) {
		if (action instanceof WorldStartAction) {
			carPosition = action.target;
		} else {
			if (timeline != null && !timeline.isDone()) {
				timeline.end();
			}
			if (action.appData == null) {
				precompute(action);
			}
			carIsReverseGear = action.isReverseGear();
			assert action.appData instanceof Timeline;
			this.timeline = (Timeline) action.appData;
			timeline.playSkipping(10);
		}
	}

	private static void playTimeline(CarChaseViewer viewer, Point targetPosition, int duration) throws InterruptedException {
		viewer.execute(new WorldAction(0, targetPosition, duration));
		while (!viewer.timeline.isDone()) { 
			Thread.sleep(10);
		}
	}
	
/*	private static void playCurve(final CarChaseViewer viewer, final double radius, final double targetAngle, long duration) throws InterruptedException {
		Timeline timeline = new SwingRepaintTimeline(viewer);
		final double startAngle = viewer.carAngle;
		timeline.addPropertyToInterpolate("carAngle", startAngle, targetAngle);
		final Point startPosition = new Point(viewer.carPosition);
		final Point targetPosition = new Point(startPosition.x + (int) (radius * Math.cos(targetAngle-Math.PI / 2-startAngle)),
											   startPosition.y + (int) (radius * Math.sin(startAngle-targetAngle)));
		timeline.addCallback(new TimelineCallback() {
			@Override
			public void onTimelineStateChanged(TimelineState oldState, TimelineState newState, float durationFraction, float timelinePosition) {
				if (newState == TimelineState.DONE) {
					viewer.carPosition = targetPosition;
					viewer.carAngle = targetAngle;
				}
			}
			@Override
			public void onTimelinePulse(float durationFraction, float timelinePosition) {
				int newX = (int) (radius * Math.cos((targetAngle - startAngle) * timelinePosition));
				int newY = (int) (radius * Math.sin((targetAngle - startAngle) * timelinePosition));
				viewer.setCarPosition(new Point(targetPosition.x - newX, startPosition.y - newY));
			}
		});
		timeline.setDuration(duration);
		timeline.playSkipping(10);
		while (!timeline.isDone()) { 
			Thread.sleep(10);
		}
	} */
	
	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		final CarChaseViewer panel = new CarChaseViewer();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame("CarApp");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				// add our object
				frame.setContentPane(panel);
				//Display the window.
		        frame.pack();
				frame.setVisible(true);
			}
		});
		panel.carPosition = new Point(250, 670);
		panel.targetPoint = panel.carPosition;
		playTimeline(panel, new Point(250, 430), 3000);
//		playCurve(panel, 20, Math.PI / 2, 350);
		playTimeline(panel, new Point(255, 415),  250);
		playTimeline(panel, new Point(270, 410),   50);
		playTimeline(panel, new Point(280, 410),  250);
		playTimeline(panel, new Point(580, 410), 3000);
//		playCurve(panel, 10, Math.PI, 350);
//		playCurve(panel, 100, -Math.PI / 2, 3000);
	}

}
