package done.inpro.system.carchase;


import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.swing.SwingRepaintTimeline;

import done.inpro.system.carchase.CarChaseExperimenter.WorldAction;
import done.inpro.system.carchase.CarChaseExperimenter.WorldStartAction;

public class CarChaseViewer extends JPanel {

	private static Logger logger = Logger.getLogger("CarChaseViewer");
	
	private final boolean PAINT_PATH = false;
	
	Image background;
	Image car;
	Point carPosition;
	double carAngle;
	double carTargetAngle;
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
			action.installmentIU = timeline;
		}
	}

	public void execute(WorldAction action) {
		if (action instanceof WorldStartAction) {
			carPosition = action.target;
		} else {
			if (timeline != null && !timeline.isDone()) {
				timeline.end();
			}
			if (action.installmentIU == null) {
				precompute(action);
			}
			assert action.installmentIU instanceof Timeline;
			this.timeline = (Timeline) action.installmentIU;
			timeline.playSkipping(10);
		}
	}
}
