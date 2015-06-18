package work.inpro.synthesis.ihmms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import marytts.htsengine.HTSModel;

public class HTSModelComparator {

	private static final int MCP_DIMENSIONS = 25;
	private static final int STR_DIMENSIONS = 5;
	
    private static List<Boolean> voicingDecisions = new ArrayList<Boolean>(); // true for errors, false for identical decisions
    private static DescriptiveStatistics f0mae = new DescriptiveStatistics();
    private static DescriptiveStatistics durmae = new DescriptiveStatistics();
    private static SummaryStatistics f0Stats = new SummaryStatistics();
    private static SummaryStatistics f0Error = new SummaryStatistics();
    private static SummaryStatistics durStats = new SummaryStatistics();
    private static SummaryStatistics durError = new SummaryStatistics();
    private static SummaryStatistics[] mcpStats = new SummaryStatistics[25];
    private static SummaryStatistics[] mcpError = new SummaryStatistics[25];
    private static SummaryStatistics[] strStats = new SummaryStatistics[5];
    private static SummaryStatistics[] strError = new SummaryStatistics[5];

    /** whether evaluation should be switched on or off */
    public static boolean active = true;
    
    static {
        for (int i = 0; i < MCP_DIMENSIONS; i++) {
            mcpStats[i] = new SummaryStatistics();
            mcpError[i] = new SummaryStatistics();
        }
        for (int i = 0; i < STR_DIMENSIONS; i++) {
            strStats[i] = new SummaryStatistics();
            strError[i] = new SummaryStatistics();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
        	  public void run() {
//        	      printFeatureStatistics(dynamicFeatureCounts);
//        	      printFeatureValueStatistics(dynamicFeatureValueCounts);
        		  int voicingErrorCount = 0;
        		  for (Boolean b : voicingDecisions) {
        			  if (b) voicingErrorCount++;
        		  }
        		  System.err.println("voicing errors: " + (100f * voicingErrorCount / voicingDecisions.size()) + " %");
        		  System.err.println("total eval-points: " + voicingDecisions.size() + ", " + durStats.getN() + ", " + durError.getN() + ", " + f0Stats.getN() + ", " + f0Error.getN());
        	      
        	      System.err.println("f0 (Cent) mean:\t" + f0Stats.getMean());
        	      System.err.println("f0 (Cent) stddev:\t" + f0Stats.getStandardDeviation());
        	      System.err.println("f0 (Cent) error mean:\t" + f0Error.getMean());
        	      System.err.println("f0 (Cent) error stddev:\t" + f0Error.getStandardDeviation());
        	      System.err.println("--> RMSE: " + rmse(f0Error));
        	      System.err.println("f0 (Cent) Median absolute error:\t" + f0mae.getPercentile(50));
        	      System.err.println("f0 z-normalized absolute error:\t" + f0mae.getMean() / f0Stats.getStandardDeviation());

        	      // original counts are in frames, each frame lasts 5ms
        	      System.err.println("dur (ms) mean:\t" + 5f * durStats.getMean());
        	      System.err.println("dur (ms) stddev:\t" + 5f * durStats.getStandardDeviation());
        	      System.err.println("dur (ms) error mean:\t" + 5f * durError.getMean());
        	      System.err.println("dur (ms) error stddev:\t" + 5f * durError.getStandardDeviation());
        	      System.err.println("--> RMSE: " + 5f * rmse(durError));
        	      System.err.println("dur (ms) Median absolute error:\t" + 5 * durmae.getPercentile(50));
        	      System.err.println("dur z-normalized absolute error:\t" + durmae.getMean() / durStats.getStandardDeviation());
//        	      System.err.println("MCP Euklidian z-normalized error:\t" + aggregatedError(mcpStats, mcpError));
//        	      System.err.println("STR Euklidian z-normalized error:\t" + aggregatedError(strStats, strError));
        	  }

			private double rmse(SummaryStatistics errorStat) {
				return Math.sqrt(errorStat.getMean() * errorStat.getMean() + errorStat.getStandardDeviation() * errorStat.getStandardDeviation());
			}
        }); /**/
    }
    
    public static final double CENT_CONST = 1731.2340490667560888319096172; // 1200 / ln(2)
    public static double lf0ToCent(double value) {
        return CENT_CONST * Math.log(Math.exp(value) / 110); 
    }
    
    /** to be used when computing summary statistics for MCP and STR features */
    @SuppressWarnings("unused")
	private static double aggregatedError(SummaryStatistics[] stats, SummaryStatistics[] error) {
        double squaredAggregatedError = 0f;
        for (int i = 0; i < stats.length; i++) {
          double normalizedError = error[i].getMean() / stats[i].getStandardDeviation();
          squaredAggregatedError += normalizedError * normalizedError;
      }
        return Math.sqrt(squaredAggregatedError);
    }
    


	public static void compare(HTSModel croppedHTSmodel, HTSModel fullHTSmodel) {
		if (active) {
			// compare durations (based on full phone duration)
			double fullDur = fullHTSmodel.getTotalDur() + fullHTSmodel.getDurError();
			double croppedDur = croppedHTSmodel.getTotalDur() + croppedHTSmodel.getDurError();
			durStats.addValue(fullDur);
			double dError = croppedDur - fullDur;
			durError.addValue(dError);
			durmae.addValue(Math.abs(dError));
			for (int i = 0; i < 5; i++) {
				// mark whether voicing decisions were identical between models
				voicingDecisions.add(fullHTSmodel.getVoiced(i) != croppedHTSmodel.getVoiced(i));
				// deal with f0: both voiced --> entry into f0Stats
				if (fullHTSmodel.getVoiced(i) && croppedHTSmodel.getVoiced(i)) {
					double fullF0 = lf0ToCent(fullHTSmodel.getLf0Mean(i, 0));
					double croppedF0 = lf0ToCent(croppedHTSmodel.getLf0Mean(i, 0));
					f0Stats.addValue(fullF0);
					double error = croppedF0 - fullF0;
					f0Error.addValue(error);
					f0mae.addValue(Math.abs(error));
				}
			}
		}
	}
	


}
