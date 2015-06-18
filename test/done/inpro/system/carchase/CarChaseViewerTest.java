package done.inpro.system.carchase;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Test;
import done.inpro.system.carchase.CarChaseExperimenter.WorldAction;

public class CarChaseViewerTest {
	
	JFrame frame;

	static void playTimeline(CarChaseViewer viewer, Point targetPosition, int duration) throws InterruptedException {
		viewer.execute(new WorldAction(0, targetPosition, duration));
		while (!viewer.timeline.isDone()) { 
			Thread.sleep(10);
		}
	}
	
	@Test(timeout=60000)
	public void test() throws InvocationTargetException, InterruptedException {
		final CarChaseViewer panel = new CarChaseViewer();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame = new JFrame("CarApp");
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
		playTimeline(panel, new Point(255, 415),  250);
		playTimeline(panel, new Point(270, 410),   50);
		playTimeline(panel, new Point(280, 410),  250);
		playTimeline(panel, new Point(580, 410), 3000);
	}
	
	@After
	public void tearDownWindow() throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame.setVisible(false);
				frame.dispose();
			}
		});
	}
	

}
