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
 * - period candidate generation (amdf, auto-correlation, cross-correlation, normalized cross-correlation)
 * - candidate refinement (e. g. thresholding of candidates)
 * - voicing decision and final path determination (dynamic programming, ...)
 * 
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.Registry;
import gov.nist.sphere.jaudio.SphereFileReader;

public class PitchTracker extends BaseDataProcessor {
	
	// TODO: sollten aus Parametern für min/max-Pitch errechnet werden (samples = samplingFrequency / pitch)
	private int minLag = 32; // entspricht f0 von 500 Hz bei 16 kHz Abtastung; 
	// Achtung: eigentlich entspricht 32 einer 500 Hz, aber wir brauchen auch noch das erste Maximum, deswegen muss hier 16 gewählt werden (das Maximum ist bei der Hälfte)   
	private int maxLag = 320; // entspricht f0 von 50 Hz bei 16 kHz Abtastung
	private int samplingFrequency = 16000; // TODO: parametrisieren
	private int maxLobeWidth = 400; // FIXME: must be configurable: 20kHz->100, 16kHz->80, ...
	
	private double[] samples;
	
	private int lastBestLag = -1;
	
	private static boolean debug = false;
	
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
	
	// calculate amdf of a given signal and downsample the signal on the way
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
	
	/**
	 * generate candidates from the amdf following the marker selection in
	 * Ying, Jamieson, Michell: A Probabilistic Approach to AMDF Pitch Detection, ICSLP 1996
	 * @return List of candidate LAGS! 
	 */
	public List<Candidate> candidates(double[] amdf) {
		// find the highest peak in the amdf
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
		double global_range = global_max - global_min;
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
						// FIXME: correct heuristics for amdf-range instead of just maximum
						
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
						if (//(Math.abs(lastBestLag - last_min_pos) < 3) || // always allow th epeak in the vicinity of the last winning peak
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
	
	int selectCandidate(List<Candidate> candidates) {
		int lag;
		// check if there is a candidate corresponding to the last best candidate
		for (Iterator<Candidate> iter = candidates.iterator(); iter.hasNext(); ) {
			lag = iter.next().lag; 
			if (Math.abs(lag - lastBestLag) < 10) { // TODO: for higher lags (lower freqs), this must be higher than for lower freqs
				lastBestLag = lag;
				return lag;
			}
		}
		// always select the first candidate for the time being (the highest f0
		lag = candidates.get(0).lag;
		// for the first candidate, select the one with the least amdf
/*		double leastAMDF = Double.MAX_VALUE;
		for (Iterator<Candidate> iter = candidates.iterator(); iter.hasNext(); ) {
			Candidate cand = iter.next();
			if (cand.amdf < leastAMDF) {
				lag = cand.lag;
			}
		}
*/		lastBestLag = lag;
		return lag;
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
		if (input instanceof DoubleData) {
			double[] newSamples = ((DoubleData) input).getValues();
			if (samples == null) {
				samples = new double[maxLag * 3 + 2];
				Arrays.fill(samples, 0);
			}
			else {
				System.arraycopy(samples, newSamples.length, samples, 0, samples.length - newSamples.length);
			}
			System.arraycopy(newSamples, 0, samples, samples.length - newSamples.length, newSamples.length);
			double[] filteredSignal = lowPassFilter(samples);
			double[] amdf = amdf(filteredSignal);
			List<Candidate> candidates = candidates(amdf);
			double pitch = -1.0f;
			boolean voiced = !candidates.isEmpty();
			if (voiced) {
				int lag = selectCandidate(candidates);
				pitch = ((double) samplingFrequency) / lag;
			} else {
				lastBestLag = -1;
			}
			input = new PitchedDoubleData((DoubleData) input, voiced, pitch);
		}
		return input;
	}

	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		// add tracking parameters here
	}

	public void register(String name, Registry registry) throws PropertyException {
		super.register(name, registry);
		// add tracking parameters here
	}

	public static void main(String[] args) {
        try {
            URL audioFileURL;
            
            if (args.length > 0) {
                audioFileURL = new File(args[0]).toURI().toURL();
            } else {
                audioFileURL = new URL("file:res/summkurz.wav");
            }

            URL configURL = PitchTracker.class.getResource("config.xml");
//            System.out.println("Loading Pitch Tracker...");

            ConfigurationManager cm = new ConfigurationManager(configURL);
            FrontEnd fe = (FrontEnd) cm.lookup("frontEnd");
            fe.initialize();
            
            System.err.println("Tracking " + audioFileURL.getFile());

            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");

            AudioInputStream ais;
            
			if (audioFileURL.getFile().endsWith(".sph") ||
				audioFileURL.getFile().endsWith(".SPH") ||
				audioFileURL.getFile().endsWith(".nis") ||
				audioFileURL.getFile().endsWith(".NIS")) {
				AudioFileReader sfr = new SphereFileReader(); 
				ais = sfr.getAudioInputStream(audioFileURL);
			} else {
	            System.err.println(AudioSystem.getAudioFileFormat(audioFileURL));
				ais = AudioSystem.getAudioInputStream(audioFileURL);
			}
            
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());

            // start doing something.
            Data d;
            int debugCounter = 0;
            while ((d = fe.getData()) != null) {
            	if (d instanceof PitchedDoubleData) {
//            		debug = (debugCounter == 45) || (debugCounter == 46)
//            			 || (debugCounter == 47) || (debugCounter == 44);
            		if (debug) System.out.println("Frame " + debugCounter + ": ");
            	//System.out.println(d.toString());
            		System.out.println(((PitchedDoubleData) d).getPitch());
                	debugCounter++;
            	} else if (d instanceof DoubleData) {
            		System.out.println("oups.");
            	}
            }
            
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

	private class Candidate {
		int lag;
		double amdf;
		
		Candidate(int lag, double amdf) {
			this.lag = lag;
			this.amdf = amdf;
		}
	}
	
}
