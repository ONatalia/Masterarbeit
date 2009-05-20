package org.cocolab.inpro.pitch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.pitch.util.PitchOptimizer;
import org.cocolab.inpro.pitch.util.PitchUtils;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

public class LookAheadPitchTracker extends PitchTracker implements Resetable {

	@S4Integer(defaultValue = 0)
	public final static String PROP_LOOK_AHEAD = "lookAhead";

	int lookAhead;
	
	PitchOptimizer pitchOptimizer;
	
	Queue<Data> localQueue;
	int framesInQueue; // number of frames in queue, DataSignals are not counted
	int currListPos; // this is the current position in the voicing/pitch lists 
	
	public void initialize() {
		super.initialize();
		reset();
	}
	
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		lookAhead = ps.getInt(PROP_LOOK_AHEAD);
		assert (lookAhead >= 0);
	}
	
	@Override
	public void reset() {
		pitchOptimizer = new PitchOptimizer();
		localQueue = new LinkedList<Data>();
		framesInQueue = 0;
		currListPos = 0;
	}
	
	private void fillQueue() {
		while (framesInQueue <= lookAhead) {
			Data data = super.getData();
			if (data == null)
				break;
			if (data instanceof PitchedDoubleData) {
				PitchedDoubleData pddata = (PitchedDoubleData) data;
				pitchOptimizer.addCandidates(pddata.getCandidates());
				framesInQueue++;
			}
			localQueue.add(data);
		} 
	}
	
	public Data getData() throws DataProcessingException {
		fillQueue();
		Data data = localQueue.poll();	
		if (data instanceof PitchedDoubleData) {
			PitchedDoubleData pddata = (PitchedDoubleData) data;
			List<Boolean> voicingList = new LinkedList<Boolean>();
			List<Double> pitchList = new LinkedList<Double>();
			pitchOptimizer.optimize(voicingList, pitchList);
			pddata.voiced = voicingList.get(currListPos);
			pddata.pitchHz = PitchUtils.centToHz(pitchList.get(currListPos));
			currListPos++;
			framesInQueue--;
		}
		return data;
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
            URL configURL = LookAheadPitchTracker.class.getResource("config.xml");

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
