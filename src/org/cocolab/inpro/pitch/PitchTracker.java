/* 
 * Copyright 2007, 2008, 2009, 2010 
 * Timo Baumann, Gabriel Skantze and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.pitch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.pitch.notifier.SignalFeatureListener;

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
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4Double;

/**
 * Data Processor for voicing-decision and pitch tracking.
 * 
 * pitch tracking usually consists of several steps:
 * - preprocessing (e. g. low pass filtering)
 * - period candidate generation (score, auto-correlation, cross-correlation, normalized cross-correlation)
 * - candidate refinement (e. g. thresholding of candidates)
 * - voicing decision and final path determination (dynamic programming, ...)
 */
public class PitchTracker extends BaseDataProcessor {
	
	private static final float SAMPLING_FREQUENCY = 16000f; // TODO: parametrisieren
	
	@S4Double(defaultValue = 0.15)
	public final static String PROP_CAND_SCORE_THRESHOLD = "scoreThreshold";
	private double candidateScoreThreshold; // default set by newProperties()
	@S4Double(defaultValue = 66.66666666)
	public final static String PROP_MIN_PITCH_HZ = "minimumPitch";
	private int maxLag = 320; // samples, entspricht f0 von 50 Hz bei 16 kHz Abtastung
	@S4Double(defaultValue = 500.0)
	public final static String PROP_MAX_PITCH_HZ = "maximumPitch";
	private int minLag = 26; // entspricht f0 von 600 Hz bei 16 kHz Abtastung; 
	
	/** 
	 * the silence threshold below which we don't do any pitch extraction 
	 * (this avoids division by zero, and especially reduces processing overhead) 
	 */
	private double energyThreshold = 5;
	
	private double[] signalBuffer;
	/** 
	 * pitch can only be assigned to a PitchedDoubleData object a little later, 
	 * when this data is in the center of the signalBuffer (not the newest
	 * data on the right of the buffer) but we don't want to buffer the objects 
	 * before we output them; we therefore output the PDD objects immediately,
	 * keep reference to them in this queue and fill in the pitch values once
	 * they have been calculated. The size of the queue (and hence the lag with
	 * which pitch becomes available) depends on the size of the signalBuffer, 
	 * which in turn depends on PROP_MIN_PITCH_HZ.
	 */ 
	private ArrayBlockingQueue<PitchedDoubleData> pitchAssignmentQueue;
	
	@S4ComponentList(type = SignalFeatureListener.class)
	public static final String PROP_LISTENERS = "listeners";	
	private List<SignalFeatureListener> listeners;

	protected static final boolean debug = false;

	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * lag scoring
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	/** 
	 * calculate score of a given signalBuffer and downsample the signalBuffer on the way.
	 * NOTE: this is currently not used in the algorithm implemented below.
	 */
	public double[] amdf(double[] signal) {
		double[] amdf = new double[maxLag + 2];
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
	
	/** calculate the sum magnitude difference square function of a signal. */
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
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * lag score normalization
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	/** cumulative mean normalization */
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
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * candidate generation
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	/** 
	 * pick global minima in the lagScoreFunction that are smaller 
	 * than candidateScoreThreshold as candidates and return the least lag.
	 * @return the least lag of all the candidates considered
	 */
	private List<PitchCandidate> qualityThresheldCandidates(double[] lagScoreTrajectory) {
		List<PitchCandidate> candidates = new ArrayList<PitchCandidate>();
		double maxVoicing = Double.MAX_VALUE; // start with the maximum 
		for (int lag = minLag + 1; lag < maxLag - 1; lag++) {
			// all minima at or below threshold
			maxVoicing = Math.min(maxVoicing, lagScoreTrajectory[lag]);
			if ((lagScoreTrajectory[lag] <= candidateScoreThreshold) &&
				(lagScoreTrajectory[lag - 1] > lagScoreTrajectory[lag]) &&
				(lagScoreTrajectory[lag] < lagScoreTrajectory[lag + 1])) {
					candidates.add(new PitchCandidate(lag, lagScoreTrajectory[lag], SAMPLING_FREQUENCY));
			}
		}
		return candidates;
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * best candidate selection
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	/** for tracking, we have to keep track of the previously selected lag */
	private int lastBestLag = -99;
	/**
	 * select lag of the candidate that is closest to the most recently selected lag
	 * (which is recorded in lastBestLag)
	 * @param candidates
	 * @return the selected candidate 
	 */
	PitchCandidate trackingCandidateSelection(List<PitchCandidate> candidates) {
		// check if there is a candidate corresponding to the last best candidate
		for (Iterator<PitchCandidate> iter = candidates.iterator(); iter.hasNext(); ) {
			PitchCandidate candidate = iter.next();
			int lag = iter.next().getLag(); 
			if (Math.abs(lag - lastBestLag) < 10) { // TODO: for higher lags (lower freqs), this must be higher than for lower freqs
				lastBestLag = lag;
				return candidate;
			}
		}
		// backoff to simple candidate selection
		PitchCandidate candidate = simplisticCandidateSelection(candidates);
		lastBestLag = candidate.getLag();
		return candidate;
	}
	
	/** select the first candidate (with the highest f0) */
	private static PitchCandidate simplisticCandidateSelection(List<PitchCandidate> candidates) {
		return candidates.get(0);
	}
	
	/** select the candidate with the highest quality (deepest minimum in the lagScoreTrajectory */
	static PitchCandidate bestCandidateSelection(List<PitchCandidate> candidates) {
		PitchCandidate bestCandidate = candidates.get(0);
		for (PitchCandidate candidate : candidates) {
			if (candidate.getScore() < bestCandidate.getScore()) {
				bestCandidate = candidate;
			}
		}
		return bestCandidate;
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * interpolation of selected lag
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	double parabolicInterpolation(double[] lagScoreTrajectory, int lag) {
		//TODO: implement parabolic interpolation
		// compare http://www.iua.upf.es/~xserra/cursos/IAM/labs/lab5/lab-5.html
		// http://ccrma.stanford.edu/~jos/parshl/Peak_Detection_Steps_3.html
		return lag;
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * high level pitch tracker
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
        getTimer().start(); // start timer that keeps track of front-end processing
		if (input instanceof DoubleData) {
			PitchedDoubleData output = new PitchedDoubleData((DoubleData) input);
			double[] newSamples = ((DoubleData) input).getValues();
			System.arraycopy(signalBuffer, newSamples.length, signalBuffer, 0, signalBuffer.length - newSamples.length);
			System.arraycopy(newSamples, 0, signalBuffer, signalBuffer.length - newSamples.length, newSamples.length);
			List<PitchCandidate> candidates = null;
			PitchCandidate selectedCandidate = null;
			if (output.getPower() > energyThreshold) { // TODO: better silence detection, maybe?
				double[] lagScoreTrajectory = cmn(smdsf(signalBuffer));
				// voicing is between 0 (unvoiced) and 1 (voiced)
				candidates = qualityThresheldCandidates(lagScoreTrajectory);
				if (!candidates.isEmpty()) {
					selectedCandidate = simplisticCandidateSelection(candidates);
				} else {
					lastBestLag = -99;
				}
			}
			// NOTE: the (pitch and score) values of candidate do NOT apply
			// to the Data processed in this call to getData, but to the Data
			// that is in the center of the signalBuffer. Hence, we output the
			// current data in this step WITHOUT setting pitch, etc, and keep
			// output PitchedDoubleData objects in a queue, to set their values
			// a once they become known (typical lag is 2 frames for 50Hz, may
			// be lower for higher PROP_MINIMUM_PITCH)
			/* transform current input into PitchedDoubleData; do not fill in any values yet */
			
			pitchAssignmentQueue.add(output);
			// fill in values into element in pitchAssignmentQueue and deal with
			// signal listeners
			signalListeners(selectedCandidate, candidates);
		}
        getTimer().stop(); // stop timer that keeps track of front-end processing
		return input;
	}
	
	/**
	 *	signal listeners, and apply values to first element of queue 
	 * @param candidate or null if no candidate/voiceless
	 * @param signalPower
	 */
	private void signalListeners(PitchCandidate selectedCandidate,
			List<PitchCandidate> candidates) {
		if (pitchAssignmentQueue.remainingCapacity() == 0) {
			boolean voiced = (selectedCandidate != null);
			double pitchHz = voiced ? selectedCandidate.pitchInHz() : -1f;
			double voicing = voiced ? Math.max(0f, 1.0f - selectedCandidate.getScore()) : Double.NaN;
			/* fill in voiced, voicing, pitchHz, candidates to element in queue */
			PitchedDoubleData pdd = pitchAssignmentQueue.remove();
			pdd.setPitch(candidates, pitchHz, voiced, voicing);
			double power = pdd.getPower();
			int frame = (int) pdd.getFirstSampleNumber() / 160; // frame since start of audio (regardless of VAD)
    		/* notify listeners */
			for (SignalFeatureListener sfl : listeners) {
				sfl.newSignalFeatures(frame, power, voiced, pitchHz);
			}
		}
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Configurable
	 * * * * * * * * * * * * * * * * * * * * * * * * * */

	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		// add tracking parameters here
		candidateScoreThreshold = ps.getDouble(PROP_CAND_SCORE_THRESHOLD);
		double freq = ps.getDouble(PROP_MIN_PITCH_HZ);
		maxLag = Double.valueOf(SAMPLING_FREQUENCY / freq).intValue();
		//System.err.println("Setting maxLag to " + maxLag);
		freq = ps.getDouble(PROP_MAX_PITCH_HZ);
		minLag = Double.valueOf(SAMPLING_FREQUENCY / freq).intValue();
		//System.err.println("Setting minLag to " + minLag);
		signalBuffer = new double[maxLag * 2 + 2];
		Arrays.fill(signalBuffer, 0);
		int queueSize = ((int) (maxLag / 160)) + 1; // should be 1 for freq_low > 100Hz, 2 for f_l > 50Hz, ...
		pitchAssignmentQueue = new ArrayBlockingQueue<PitchedDoubleData>(queueSize, false);
		listeners = ps.getComponentList(PROP_LISTENERS, SignalFeatureListener.class);
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * *
	 * main 
	 * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private static Queue<Double> getReferencePitch(String filename) {
		Queue<Double> referencePitch = new LinkedList<Double>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while (br.ready()) {
				String line = br.readLine();
				if (!line.matches("^#")) { // allow comments in pitch file
					referencePitch.add(new Double(line));
				}
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return referencePitch;
	}
	
	public static void functionalTest(String[] args, ConfigurationManager cm) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException, DataProcessingException {
		// read reference pitch (if available)
		final Queue<Double> referencePitch = (args.length > 1) 
    		  ? getReferencePitch(args[1]) : new LinkedList<Double>();
    	// add a listener for the output
    	PitchTracker pt = (PitchTracker) cm.lookup("pitchTracker");
    	pt.listeners.add(new SignalFeatureListener(){
			@Override
			public void newSignalFeatures(int frame, double logEnergy,
					boolean voicing, double pitch) {
        		System.out.printf((Locale) null, "%7.3f", pitch);
        		System.out.print("\t");
        		if (!referencePitch.isEmpty()) {
        			double refPitch = referencePitch.remove().doubleValue();
        			System.out.printf((Locale) null, "%8.3f", refPitch);
        			if (Math.abs(pitch - refPitch) > 20) {
        				System.out.print("\t!!!");
        			}
        		}
        		System.out.println();
			}
			@Override
			public void newProperties(PropertySheet ps)
					throws PropertyException { }
			@Override
			public void reset() { }
    		
    	});
        // drain the frontend
        BaseDataProcessor fe = (BaseDataProcessor) cm.lookup("frontEnd");
        while ((fe.getData()) != null) { }
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
            
            AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);
            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());
            
//            speedTest(fe);
        	functionalTest(args, cm);
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
							System.err.println("lag: " + last_min_pos + ", corresponding to frequency: " + SAMPLING_FREQUENCY / last_min_pos);
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