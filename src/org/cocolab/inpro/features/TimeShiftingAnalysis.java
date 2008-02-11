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
	protected double range;
	protected double mse;
	protected double meanDifference;
	protected double minPos;
	protected double maxPos;
	
	public TimeShiftingAnalysis(int maxLag) {
		dataPoints = new LinkedList<DataPoint>();
		assert maxLag >= 0;
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
			dirty = false;
			// sums of t and x
			// see, for example, http://en.wikipedia.org/w/index.php?title=Least_squares&oldid=176674204
			int sumT = 0;
			double sumX = 0;
			int sumTT = 0;
			double sumTX = 0;
			double sumXX = 0;
			// find largest and smallest data points
			Iterator<DataPoint> dataIt = dataPoints.listIterator();
			DataPoint maxDP = dataPoints.getFirst();
			DataPoint minDP = maxDP;
			double accumulatedDifferences = 0.0; // summs the differences between values (skewness of curve) 
			double lastX = maxDP.x; // for the summation of deltas
			int n = 0; // number of data points in list
			while (dataIt.hasNext()) {
				DataPoint dp = dataIt.next();
				sumT += dp.t;
				sumTT += dp.t * dp.t;
				sumX += dp.x;
				sumTX += dp.t * dp.x;
				sumXX += dp.x * dp.x;
				accumulatedDifferences += Math.abs(dp.x - lastX);
				lastX = dp.x;
				if (dp.x > maxDP.x) {
					maxDP = dp;
				}
				if (dp.x < minDP.x) {
					minDP = dp;
				}
				n++;
			}
			double sumSqDevTX = (n * sumTX - sumT * sumX);
			double sumSqDevT = (n * sumTT - sumT * sumT);
			double sumSqDevX = (n * sumXX - (sumX * sumX));
			if (n > 1) {
				slope = sumSqDevTX / sumSqDevT;
				intercept = (sumX - slope * sumT) / n;
			}
			// while we're there, also compute some other measures
			mean = (sumX / n);
			meanDifference = accumulatedDifferences / ((double) (n - 1));
			mse = ((sumSqDevX) - (sumSqDevT) * (slope * slope)) / (n*n);
			range = (maxDP != null) ? (maxDP.x - minDP.x) : 0;
			// int timeSpanned = dataPoints.getFirst().t - dataPoints.getLast().t;
			int timeSpanned = maxLag;
			minPos = ((double) (minDP.t - dataPoints.getLast().t)) / ((timeSpanned != 0) ? timeSpanned : 1);
			maxPos = ((double) (maxDP.t - dataPoints.getLast().t)) / ((timeSpanned != 0) ? timeSpanned : 1);
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
	
	public double getRange() {
		doRegression();
		return range;
	}
	
	public double getMSE() {
		doRegression();
		return mse;
	}
	
	public double getMeanStepDifference() {
		doRegression();
		return meanDifference;
	}
	
	public double getMinPosition() {
		return minPos;
	}
	
	public double getMaxPosition() {
		return maxPos;
	}
	
	public double predictValueAt(int t) {
		doRegression();
		return intercept + t * slope;
	}
	
	public double getLatestValue() {
		return dataPoints.isEmpty() ? 0 : dataPoints.getFirst().x;
	}
	
	public boolean hasValidData() {
		return dataPoints.size() > 1; // a regression of just one data point is useless
	}
	
	public void reset() {
		dataPoints.clear();	
		currentTime = 0;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("TimeShiftingAnalysis with maxLag: ");
		sb.append((maxLag != 0) ? maxLag : "none");
		sb.append("\ndata points:\n");
		sb.append(dataPoints.toString());
		sb.append("\ny(t) = ");
		sb.append(getSlope());
		sb.append(" * t + ");
		sb.append(getIntercept());
		sb.append("\nmean: ");
		sb.append(getMean());
		sb.append("\nrange: ");
		sb.append(getRange());
		sb.append("\nMSE of regression: ");
		sb.append(getMSE());
		sb.append("\nmean difference: ");
		sb.append(getMeanStepDifference());
		sb.append("\nposition of minimum: ");
		sb.append(getMinPosition());
		sb.append("\nposition of maximum: ");
		sb.append(getMaxPosition());
		sb.append("\nvalue at 0: ");
		sb.append(predictValueAt(0));
		sb.append("\nerror for prediction at last value: ");
		sb.append(predictValueAt(dataPoints.getFirst().t) - getLatestValue());
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
		System.out.println("\n\tadding point (4, 2.0)");
		tsa.add(4, 2.0);
		System.out.println(tsa);
		System.out.println("\n\tsetting lag to 2");
		tsa.setMaxLag(2);
		System.out.println(tsa);
		System.out.println("\n\tadding point (5, 1.0)");		
		tsa.add(5, 1.0);
		System.out.println(tsa);
		tsa.reset();
		System.out.println("\n\tsetting new points: (0/-1), (1/-2), (2/4), (3/-1), (10/-1)");
		tsa.setMaxLag(10);
		tsa.add(0, -1.0);
		tsa.add(1, -2.0);
		tsa.add(2, 4.0);
		tsa.add(3, -1.0);
		tsa.add(10, -1.0);
		System.out.println(tsa);

		tsa.reset();
		System.out.println("\n\tsetting new points: -1, -1, 4, -1, -1");
		tsa.setMaxLag(10);
		tsa.add(0, -1.0);
		tsa.add(1, -1.0);
		tsa.add(2, 4.0);
		tsa.add(3, -1.0);
		tsa.add(4, -1.0);
		System.out.println(tsa);
		tsa.reset();
		System.out.println("\n\tsetting some random values");
		tsa.setMaxLag(10);
		for (int i = 0; i < 10; i++) {			
			tsa.add(i * 2, Math.random() * 100 - 50);
		}
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
		
		public String toString() {
			return this.t + ", " + this.x + "\n";
		}
	}
	
}
