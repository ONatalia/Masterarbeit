package org.cocolab.inpro.features;

import java.util.Iterator;
import java.util.LinkedList;

import edu.cmu.sphinx.util.props.Resetable;


/**
 * class to perform linear regression and mean calculation; 
 * ideal for incrementally added data points. old data points 
 * can be discarded automatically using setMaxLag()
 * @author timo
 *
 */
public class TimeShiftingAnalysis implements Resetable {

	// linear regression of y(t) = a*t + b
	// using data points (t1, x1), (t2, x2), ...
	
	// container for the data points, newest data is always stored in front
	protected LinkedList<DataPoint> dataPoints;
	
	// maximum lag of the oldest datapoint compared to the current time
	protected int maxLag;
	
	protected int currentTime = 0;
	
	protected boolean dirty = false;
	
	protected double slope;
	protected double intercept;
	protected double mean;
	
	public TimeShiftingAnalysis(int maxLag) {
		dataPoints = new LinkedList<DataPoint>();
		this.maxLag = maxLag;
	}
	
	public TimeShiftingAnalysis() {
		this(0);
	}
	
	public void add(int t, double x) {
		// add data point and remove data points, that are more than maxLag ago
		dataPoints.addFirst(new DataPoint(t, x));
		shiftTime(t);
		dirty = true;
	}
	
	public void shiftTime(int t) {
		currentTime = t;
		removeOldPoints();
	}
	
	private void removeOldPoints() {
		if (maxLag > 0) {
			while (!dataPoints.isEmpty() && (dataPoints.getLast().t < currentTime - maxLag)) {
				dataPoints.removeLast();
				dirty = true;
			}
		}
	}
	
	/**
	 * set the maximum lag of the time shifting analysis
	 * @param maxLag maximum lag of the oldest considered data point
	 * 				 0: consider all data points
	 */
	public void setMaxLag(int maxLag) {
		this.maxLag = maxLag;
		removeOldPoints();
	}
	
	protected void doRegression() {
		if (dirty) {
			// sums of t and x
			double st = 0.0;
			double sx = 0.0;
			double stt = 0.0;
			double stx = 0.0;
			int n = 0;
			Iterator<DataPoint> dataIt = dataPoints.listIterator();
			while (dataIt.hasNext()) {
				DataPoint dp = dataIt.next();
				st += dp.t;
				sx += dp.x;
				stt += dp.t * dp.t;
				stx += dp.t * dp.x;
				n++;
			}
			assert n > 0;
			if (n > 1) {
				slope = (n * stx - st * sx) / (n * stt - st * st);
				intercept = (sx - slope * st) / n;
				// while we're there, also compute the mean, 
				// even though it doesn't have anything to do with regression
			}
			mean = sx / n;
			dirty = false;
		}
	}

	public double getSlope() {
		doRegression();
		return slope;
	}
	
	public double getIntercept() {
		doRegression();
		return intercept;
	}
	
	public double getMean() {
		doRegression();
		return mean;		
	}
	
	public double predictValueAt(int t) {
		doRegression();
		return intercept + t * slope;
	}
	
	
	public void reset() {
		dataPoints.clear();	
		currentTime = 0;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("y(t) = ");
		sb.append(getSlope());
		sb.append(" * t + ");
		sb.append(getIntercept());
		sb.append("\nmean: ");
		sb.append(getMean());
		sb.append("; value at 0: ");
		sb.append(predictValueAt(0));
		return sb.toString();
	}
	
	public static void main(String[] args) {
		TimeShiftingAnalysis tsa = new TimeShiftingAnalysis();
		System.out.println("\tadding point (1, 1.0)");
		tsa.add(1, 1.0);
		System.out.println("\tadding point (2, 2.0)");
		tsa.add(2, 2.0);
		System.out.println("\tadding point (3, 3.0)");
		tsa.add(3, 3.0);
		System.out.println(tsa);
		System.out.println("\tadding point (4, 2.0)");
		tsa.add(4, 2.0);
		System.out.println(tsa);
		System.out.println("\tsetting lag to 2");
		tsa.setMaxLag(2);
		System.out.println(tsa);
		System.out.println("\tadding point (5, 1.0)");		
		tsa.add(5, 1.0);
		System.out.println(tsa);
	}

	/* utility class for data points */
	private class DataPoint {
		int t;
		double x;
		
		DataPoint(int t, double x) {
			this.t = t;
			this.x = x;
		}
	}
	
}
