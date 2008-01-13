package org.cocolab.inpro.pitch;

/**
 * Data Processor for voicing-decision and pitch tracking
 * 
 * uses the AMDF (average magnitude difference function) and some heuristics based 
 * on:  
 * Ying, Jamieson, Michell: A Probabilistic Approach to AMDF Pitch Detection, ICSLP 1996
 * 
 * pitch tracking usually consists of several steps:
 * - preprocessing (e. g. low pass filtering)
 * - period candidate generation (score, auto-correlation, cross-correlation, normalized cross-correlation)
 * - candidate refinement (e. g. thresholding of candidates)
 * - voicing decision and final path determination (dynamic programming, ...)
 * 
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.Registry;

public class PitchTracker extends BaseDataProcessor {
	
	// TODO: sollten aus Parametern für min/max-Pitch errechnet werden (signal = samplingFrequency / pitch)
	private int minLag = 32; // entspricht f0 von 500 Hz bei 16 kHz Abtastung; 
	private int maxLag = 320; // entspricht f0 von 50 Hz bei 16 kHz Abtastung
	private int samplingFrequency = 16000; // TODO: parametrisieren
	
	private double candidateScoreThreshold = 0.15;
	
	private double[] signalBuffer;
	
	private static boolean debug = false;

	/**************************
	 * lag scoring
	 **************************/

	// calculate score of a given signalBuffer and downsample the signalBuffer on the way
	public double[] amdf(double[] signal) {
		double[] amdf = new double[maxLag + 2];
		Arrays.fill(amdf, 0, minLag, 0.0f);
		int numSamples = signal.length;
		if (minLag < 2) {
			minLag = 2;
		}
		for (int shift = minLag; shift < maxLag; shift++) {
			double sum = 0;
			for (int i = 0; i < numSamples - shift; i++) {
				sum += Math.abs(signal[i] - signal[i + shift]);
			}
			double value = sum / (numSamples - shift);
			amdf[shift] = value;
		}
		return amdf;
	}
	
/**/
	public double[] smdsf(double[] signal) {
		double[] smdsf = new double[maxLag];
		int numSamples = signal.length;
		for (int lag = 1; lag < maxLag; lag++) {
			double sum = 0;
			for (int i = 0; i < numSamples - maxLag; i++) {
				double difference = signal[i] - signal[i + lag];  
				sum += difference * difference;
			}
			smdsf[lag] = sum;
		}
		return smdsf;
	}
/**
	
/**/
	/**************************
	 * lag score normalization
	 **************************/

	/* cumulative mean normalization */
	public double[] cmn(double[] smdsf) {
		int maxLag = smdsf.length;
		double[] cmn = new double[maxLag];
		cmn[0] = 1;
		double smdsfSum = smdsf[1];
		for (int shift = 1; shift < maxLag; shift++) {
			smdsfSum += smdsf[shift];
			cmn[shift] = (smdsf[shift] * shift) / smdsfSum;
		}
		return cmn;
	}
	
	/**************************
	 * candidate generation
	 **************************/

	/**
	 * pick global minima in the lagScoreFunction that are smaller than threshold as candidates 
	 */
	private List<Candidate> qualityThresheldCandidates(double[] lagScoreFunction) {
		List<Candidate> candidates = new ArrayList<Candidate>(); 
		for (int lag = minLag + 1; lag < maxLag - 1; lag++) {
			// all minima at or below threshold
			if ((lagScoreFunction[lag] <= candidateScoreThreshold) &&
				(lagScoreFunction[lag - 1] > lagScoreFunction[lag]) &&
				(lagScoreFunction[lag] < lagScoreFunction[lag + 1])) {
					candidates.add(new Candidate(lag, lagScoreFunction[lag]));
			}
		}
		return candidates;
	}
	
	/**************************
	 * candidate selection
	 **************************/
	
	private int lastBestLag = -1;
	
	private int trackingCandidateSelection(List<Candidate> candidates) {
		int lag;
		// check if there is a candidate corresponding to the last best candidate
		for (Iterator<Candidate> iter = candidates.iterator(); iter.hasNext(); ) {
			lag = iter.next().lag; 
			if (Math.abs(lag - lastBestLag) < 10) { // TODO: for higher lags (lower freqs), this must be higher than for lower freqs
				lastBestLag = lag;
				return lag;
			}
		}
		// backoff to simple candidate selection
		lag = simplisticCandidateSelection(candidates);
		lastBestLag = lag;
		return lag;
	}
	
	private static int simplisticCandidateSelection(List<Candidate> candidates) {
		// select the first candidate (with the highest f0)
		return candidates.get(0).lag;
	}
	
	/**************************
	 * interpolation of selected lag
	 **************************/
	double parabolicInterpolation(double[] lagScoreFunction, int lag) {
		//TODO: implement parabolic interpolation
		// compare http://www.iua.upf.es/~xserra/cursos/IAM/labs/lab5/lab-5.html
		return lag;
	}
	
	/**************************
	 * Candidate class
	 **************************/

	private class Candidate {
		int lag;
		double score;
		
		Candidate(int lag, double score) {
			this.lag = lag;
			this.score = score;
		}
	}
	
	/**************************
	 * high level pitch tracker
	 **************************/

	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
		if (input instanceof DoubleData) {
			double[] newSamples = ((DoubleData) input).getValues();
			if (signalBuffer == null) {
				signalBuffer = new double[maxLag * 2 + 2];
				Arrays.fill(signalBuffer, 0);
			}
			else {
				System.arraycopy(signalBuffer, newSamples.length, signalBuffer, 0, signalBuffer.length - newSamples.length);
				System.arraycopy(newSamples, 0, signalBuffer, signalBuffer.length - newSamples.length, newSamples.length);
				double[] lagScoreFunction = cmn(smdsf(signalBuffer));
				List<Candidate> candidates = qualityThresheldCandidates(lagScoreFunction);
				double pitchHz = -1.0f;
				boolean voiced = !candidates.isEmpty();
				if (voiced) {
					int lag = simplisticCandidateSelection(candidates);
					pitchHz = ((double) samplingFrequency) / lag;
				} else {
					lastBestLag = -1;
				}
				input = new PitchedDoubleData((DoubleData) input, voiced, pitchHz);
			}
		}
		return input;
	}

	/**************************
	 * configuration utility functions
	 **************************/

	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		// add tracking parameters here
	}

	public void register(String name, Registry registry) throws PropertyException {
		super.register(name, registry);
		// add tracking parameters here
	}

	/**************************
	 * main 
	 **************************/
	
	private static Deque<Double> getReferencePitch(String filename) {
		Deque<Double> referencePitch = new LinkedList<Double>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while (br.ready()) {
				String line = br.readLine();
				if (!line.matches("^#")) { // allow comments in pitch file
					referencePitch.addLast(new Double(line));
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return referencePitch;
	}
	
	public static void functionalTest(String[] args, BaseDataProcessor fe) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException, DataProcessingException {
    	Deque<Double> referencePitch;
    	if (args.length > 1) {
        	referencePitch = getReferencePitch(args[1]);
        } else {
        	referencePitch = new LinkedList<Double>();
        }
        // start doing something.
        Data d;
        int debugCounter = 0;
        while ((d = fe.getData()) != null) {
        	if (d instanceof PitchedDoubleData) {
//        		debug = (debugCounter == 45) || (debugCounter == 46)
//        			 || (debugCounter == 47) || (debugCounter == 44);
        		if (debug) System.out.println("Frame " + debugCounter + ": ");
        		double pitch = ((PitchedDoubleData) d).getPitchHz();
        		System.out.printf((Locale) null, "%7.3f", pitch);
        		System.out.print("\t");
        		if (!referencePitch.isEmpty()) {
        			double refPitch = referencePitch.removeFirst().doubleValue();
        			System.out.printf((Locale) null, "%8.3f", refPitch);
        			if (Math.abs(pitch - refPitch) > 20) {
        				System.out.print("\t!!!");
        			}
        		}
        		System.out.println();
            	debugCounter++;
        	} else if (d instanceof DoubleData) {
        		//System.out.println("oups.");
        	}
        }
	}
	
	public static void speedTest(BaseDataProcessor fe) throws DataProcessingException {
		long startTime = System.currentTimeMillis();
		Data d = null;
		Data e;
		while ((e = fe.getData()) != null) {
			d = e;
        }
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		long duration = ((DataEndSignal) d).getDuration();
		double speedFactor = ((double) duration) / (double) elapsedTime;
		System.err.println("Audio duration: " + duration + " ms");
		System.err.println("Elapsed time: " + elapsedTime + " ms");
		System.err.print("Speed: ");
		System.err.printf((Locale) null, "%5.3f", speedFactor);
		System.err.println(" X realtime");
	}
	
	public static void main(String[] args) {
        try {
            URL audioFileURL;
            if (args.length > 0) {
                audioFileURL = new File(args[0]).toURI().toURL();
            } else {
                audioFileURL = new URL("file:res/summkurz.wav");
            }
            if (debug) System.err.println("Tracking " + audioFileURL.getFile());
            URL configURL = PitchTracker.class.getResource("config.xml");

            ConfigurationManager cm = new ConfigurationManager(configURL);
            FrontEnd fe = (FrontEnd) cm.lookup("frontEnd");
            fe.initialize();
            
            AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioFileURL);
            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());
            
//            speedTest(fe);
        	functionalTest(args, fe);
        } catch (IOException e) {
            System.err.println("Problem when loading PitchTracker: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

}


/* unused functions:

	/**************************
	 * pre processing
	 ************************** /

	private double[] hammingFilter =// {0.0241, 0.0934, 0.2319, 0.3012,
									//  0.2319, 0.0934, 0.0241};
	{ 0.0182, 0.0488, 0.1227, 0.1967, 0.2273, 
	 0.1967, 0.1227, 0.0488, 0.0182};

	private double[] lowPassFilter(double[] signal) {
		double[] filter = hammingFilter;
		double[] filteredSignal = new double[signal.length + filter.length]; 
		for (int signalIndex = 0; signalIndex < signal.length; signalIndex++) {
			for (int filterIndex = 0; filterIndex < filter.length; filterIndex++) {
				filteredSignal[signalIndex + filterIndex] 
				               += signal[signalIndex] * filter[filterIndex]; 
			}
		}
		System.arraycopy(filteredSignal, (filter.length - 1) / 2, signal, 0, signal.length);
		return signal;
	}
	
	/**
	 * generate candidates from the score following the marker selection in
	 * Ying, Jamieson, Michell: A Probabilistic Approach to AMDF Pitch Detection, ICSLP 1996
	 * @return List of candidate LAGS (not frequencies)!
	 * /
	private int maxLobeWidth = 400; // FIXME: must be configurable: 20kHz->100, 16kHz->80, ...

	private List<Candidate> candidatesViaHeuristics(double[] amdf) {
		// find the highest peak in the score
		double global_max = amdf[minLag];
		double global_min = amdf[minLag];
		for (int i = minLag + 1; i < maxLag; i++) {
			if (amdf[i] > global_max) {
				global_max = amdf[i];
			}
			if (amdf[i] < global_min) {
				global_min = amdf[i];
			}
		}
		List<Candidate> candidates = new ArrayList<Candidate>(); 
		double last_max = 0.0f;
		int last_max_pos = 0;
		double last_min = 0.0f;
		int last_min_pos = 0;
		double amdfAtLastMin = amdf[minLag];
		boolean rising = true;
		for (int lag = minLag + 1; lag < maxLag; lag++) {
			double amdfAtLag = amdf[lag];
			if ((rising && (amdfAtLag - amdfAtLastMin < 0)) ||
				(!rising && (amdfAtLag - amdfAtLastMin > 0))) {
//				System.out.println(((rising) ? "maximum" : "minimum") + " at " + i);
				if (rising) {
					// process once we get to a local maximum (since only then we know everything about the preceding valley)
					if (last_max_pos > 0) {
						// FIXME: correct heuristics for score-range instead of just maximum
						
						// peak_ratio: height of maxima right/left compared to global_max, 
						// FIXME: height of maxima should be scaled to global_range 
						double peak_ratio = (last_max + amdfAtLag) / (global_max * 2);
						// height: depth/height of valley/peak, should be scaled to global range 
						double height = Math.min(last_max, amdfAtLag) - last_min;
						// difference in height of left and right maxima of valley
						double diff = Math.abs(last_max - amdfAtLag);
						// width of valley
						int lobe_width = lag - last_max_pos;
						if (debug) {
							System.err.println("lag: " + last_min_pos + ", corresponding to frequency: " + samplingFrequency / last_min_pos);
							System.err.println("global_max: " + global_max);
							System.err.println("peak_ratio: " + peak_ratio);
							System.err.println("height:     " + height);
							System.err.println("diff:       " + diff);
							System.err.println("lobe_width: " + lobe_width);							
						}
						if (//(Math.abs(lastBestLag - last_min_pos) < 3) || // always allow the peak in the vicinity of the last winning peak
							((peak_ratio >= 0.8) &&
							(global_max >= 200.0) && // assert signal strength 
							(height >= 0.3 * global_max) &&
							(diff <= 0.15 * global_max) &&
							(lobe_width <= maxLobeWidth))) {
								Candidate candidate = new Candidate(last_min_pos, amdfAtLag);
								candidates.add(candidate);
								if (debug) {
									System.err.println("taking as candidate.\n");
								}
						} else {
							if (debug) {
								System.err.println("rejecting as candidate. offending heuristics:");
								if (peak_ratio < 0.8) System.err.print("peak_ratio >= 0.8, ");
								if (global_max < 10.0) System.err.print("global_max >= 10.0"); 
								if (height < 0.3 * global_max) System.err.print("height >= 0.3 * global_max, ");
								if (diff > 0.1 * global_max) System.err.print("diff <= 0.1 * global_max, ");
								if (lobe_width > maxLobeWidth) System.err.print("lobe_width <= maxLobeWidth, ");
								System.err.println("\n");
							}
						}
						
					}
					last_max = amdfAtLag;
					last_max_pos = lag;
				} else {
					last_min = amdfAtLag;
					last_min_pos = lag;
				}
					
				rising = !rising;
			}
			amdfAtLastMin = amdfAtLag;
		}
		return candidates;
	}
	

 */