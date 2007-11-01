package org.cocolab.inpro.pitch;

/**
 * Data Processor for voicing-decision and pitch tracking
 * 
 * uses the AMDF (average magnitude difference function) approach
 * as outlined in: 
 * Ying, Jamieson, Michell: A Probabilistic Approach to AMDF Pitch Detection, ICSLP 1996
 * 
 * pitch tracking usually has several steps:
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
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

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

public class PitchTracker extends BaseDataProcessor {
	
	// TODO: sollten aus Parametern für min/max-Pitch errechnet werden (samples = samplingFrequency / pitch)
	private int minSamples = 16; // entspricht f0 von 500 Hz bei 16 kHz Abtastung; 
	// Achtung: eigentlich entspricht 32 einer 500 Hz, aber wir brauchen auch noch das erste Maximum, deswegen muss hier 16 gewählt werden (das Maximum ist immer genau bei der Hälfte)   
	private int maxSamples = 320; // entspricht f0 von 50 Hz bei 16 kHz Abtastung
	private int samplingFrequency = 16000; // TODO: parametrisieren
	private int maxLobeWidth = 400; // FIXME: must be configurable: 20kHz->100, 16kHz->80, ...
	
	
	// calculate amdf of a given signal and downsample the signal on the way
	public double[] amdf(double[] samples) {
		double[] amdf = new double[maxSamples + 2];
		Arrays.fill(amdf, 0, minSamples, 0.0f);
		int numSamples = samples.length;
		if (minSamples < 2) {
			minSamples = 2;
		}
		for (int shift = minSamples; shift < maxSamples; shift++) {
			double sum = 0;
			for (int i = 0; i < numSamples - shift; i++) {
				sum += Math.abs(samples[i] - samples[i + shift]);
			}
			double value = sum / (numSamples - shift);
			// do some von-Hann-filtering on the way
/*			value = value;
			double v025 = value * 0.25;
			double v075 = value * 0.75; 
			amdf[shift - 2] = v025;
			amdf[shift - 1] = v075;
*/			amdf[shift] = value;
//			amdf[shift + 1] = v075;
//			amdf[shift + 2] = v025;
		}
		return amdf;
	}
	
	// generate candidates from the amdf following the marker selection in
	// Ying, Jamieson, Michell: A Probabilistic Approach to AMDF Pitch Detection, ICSLP 1996
	public List<Double> candidates(double[] amdf) {
		// find the highest peak in the amdf
		double global_max = 0.0f;
		for (int i = minSamples; i < maxSamples; i++) {
			if (amdf[i] > global_max) {
				global_max = amdf[i];
			}
		}
		List<Double> candidates = new ArrayList<Double>(); 
		double last_max = 0.0f;
		int last_max_pos = 0;
		double last_min = 0.0f;
		int last_min_pos = 0;
		double lastValue = amdf[minSamples];
		boolean rising = true;
		for (int i = minSamples + 1; i < maxSamples; i++) {
			double value = amdf[i];
			if ((rising && (value - lastValue < 0)) ||
				(!rising && (value - lastValue > 0))) {
//				System.out.println(((rising) ? "maximum" : "minimum") + " at " + i);
				if (rising) {
					if (last_max_pos > 0) {
						double peak_ratio = (last_max + value) / (global_max * 2);
						double height = Math.min(last_max, value) - last_min;
						double diff = Math.abs(last_max - value);
						int lobe_width = i - last_max_pos;
						if ((peak_ratio >= 0.8) &&
							(height >= 0.3 * global_max) &&
							(diff <= 0.1 * global_max) &&
							(lobe_width <= maxLobeWidth)) {
							Double candidate = new Double(((double) samplingFrequency) / last_min_pos);
							candidates.add(candidate);
						} else {
							
							/*System.out.println("global_max: " + global_max);
							System.out.println("peak_ratio: " + peak_ratio);
							System.out.println("height:     " + height);
							System.out.println("diff:       " + diff);
							System.out.println("lobe_width: " + lobe_width);
							*/
						}
						
					}
					last_max = value;
					last_max_pos = i;
				} else {
					last_min = value;
					last_min_pos = i;
				}
					
				rising = !rising;
			}
			lastValue = value;
		}
		return candidates;
	}
	
	double selectCandidate(List<Double> candidates) {
		// always select the first candidate for the time being
		return candidates.get(0).doubleValue();
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
		if (input instanceof DoubleData) {
			double[] samples = ((DoubleData) input).getValues();
			double[] amdf = amdf(samples);
			List<Double> candidates = candidates(amdf);
			double pitch = -1.0f;
			boolean voiced = !candidates.isEmpty();
			if (voiced) {
				pitch = selectCandidate(candidates);
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
                audioFileURL = new URL("file:res/telescope.wav");
            }

            URL configURL = PitchTracker.class.getResource("config.xml");

            System.out.println("Loading Pitch Tracker...\n");

            ConfigurationManager cm = new ConfigurationManager(configURL);

            FrontEnd fe = (FrontEnd) cm.lookup("frontEnd");
            fe.initialize();
            
            System.out.println("Tracking " + audioFileURL.getFile());
            System.out.println(AudioSystem.getAudioFileFormat(audioFileURL));

            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
            
            AudioInputStream ais 
                = AudioSystem.getAudioInputStream(audioFileURL);
            
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());

            // start doing something.
            Data d;
            while ((d = fe.getData()) != null) {
            	if (d instanceof PitchedDoubleData) {
            	//System.out.println(d.toString());
            		System.out.println(((PitchedDoubleData) d).getPitch());
            	}
            }
            
        } catch (IOException e) {
            System.err.println("Problem when loading IncrementalWavFile: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

}
