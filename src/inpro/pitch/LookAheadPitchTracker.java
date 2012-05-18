/* 
 * Copyright 2007, 2008, 2009, Timo Baumann and the Inpro project
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
package inpro.pitch;

import inpro.pitch.util.PitchOptimizer;
import inpro.pitch.util.PitchUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

/** 
 * A pitch tracker that uses lookahead and dynamic programming to determine pitch tracks.
 */
public class LookAheadPitchTracker extends PitchTracker implements Resetable {

	@S4Integer(defaultValue = 0)
	public final static String PROP_LOOK_AHEAD = "lookAhead";

	int lookAhead;
	List<Boolean> voicingList;
	List<Double> pitchList;
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
		localQueue = new ArrayDeque<Data>();
		framesInQueue = 0;
		currListPos = 0;
	}
	
	private void fillQueue() {
		boolean dirty = false;
		while (framesInQueue <= lookAhead) {
			Data data = super.getData();
			if (data == null)
				break;
			if (data instanceof PitchedDoubleData) {
				PitchedDoubleData pddata = (PitchedDoubleData) data;
				pitchOptimizer.addCandidates(pddata.getCandidates());
				dirty = true;
				framesInQueue++;
			}
			localQueue.add(data);
		}
		if (dirty) {
			voicingList = new ArrayList<Boolean>();
			pitchList = new ArrayList<Double>();
			pitchOptimizer.optimize(voicingList, pitchList);
		}
	}
	
	public Data getData() throws DataProcessingException {
		fillQueue();
		Data data = localQueue.poll();	
		if (data instanceof PitchedDoubleData) {
			PitchedDoubleData pddata = (PitchedDoubleData) data;
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
