/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package inpro.sphinx.frontend;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Converts a stream of SpeechClassifiedData objects, marked as speech and
 * non-speech, and mark out the regions that are considered speech. This is done
 * by inserting SPEECH_START and SPEECH_END signals into the stream.
 * <p/>
 * <p>
 * The algorithm for inserting the two signals is as follows.
 * <p/>
 * <p>
 * The algorithm is always in one of two states: 'in-speech' and
 * 'out-of-speech'. If 'out-of-speech', it will read in audio until we hit audio
 * that is speech. If we have read more than 'startSpeech' amount of
 * <i>continuous</i> speech, we consider that speech has started, and insert a
 * SPEECH_START at 'speechLeader' time before speech first started. The state of
 * the algorithm changes to 'in-speech'.
 * <p/>
 * <p>
 * Now consider the case when the algorithm is in 'in-speech' state. If it read
 * an audio that is speech, it is scheduled for output. If the audio is non-speech, we read
 * ahead until we have 'endSilence' amount of <i>continuous</i> non-speech. At
 * the point we consider that speech has ended. A SPEECH_END signal is inserted
 * at 'speechTrailer' time after the first non-speech audio. The algorithm
 * returns to 'out-of-speech' state. If any speech audio is encountered
 * in-between, the accounting starts all over again.
 * 
 * While speech audio is processed delay is lowered to some minimal amount. This helps
 * to segment both slow speech with visible delays and fast speech when delays are minimal.
 */
public class QuickSpeechMarker extends BaseDataProcessor {

    /**
     * The property for the minimum amount of time in speech (in milliseconds) to be considered
     * as utterance start.
     */
    @S4Integer(defaultValue = 200)
    public static final String PROP_START_SPEECH = "startSpeech";
    private int startSpeechTime;

    /**
     * The property for the amount of time in silence (in milliseconds) to be
     * considered as utterance end.
     */
    @S4Integer(defaultValue = 500)
    public static final String PROP_END_SILENCE = "endSilence";
    private int endSilenceTime;

    /**
     * The property for the amount of time (in milliseconds) before speech start
     * to be included as speech data.
     */
    @S4Integer(defaultValue = 50)
    public static final String PROP_SPEECH_LEADER = "speechLeader";
    private int speechLeader;

    /**
     * The property for the amount of time (in milliseconds) after speech ends to be
     * included as speech data.
     */
    @S4Integer(defaultValue = 50)
    public static final String PROP_SPEECH_TRAILER = "speechTrailer";
    private int speechTrailer;
    
    /** processed frames which are ready to be released */
    private Queue<Data> outputQueue;
    /** frames that have to be kept, in order to possibly insert a signal into the stream */
    private List<Data> buffer; 
    
    private int currentSilence; // both measured in milliseconds
    private int currentSpeech;
    
    private enum State { IN_SPEECH, NON_SPEECH, DATA_END };
    
    State state;

    public QuickSpeechMarker(int startSpeechTime, int endSilenceTime, int speechLeader, int speechTrailer) {
    	this();
        initLogger();
        this.startSpeechTime = startSpeechTime;
        this.endSilenceTime = endSilenceTime;
        this.speechLeader = speechLeader;
        this.speechTrailer = speechTrailer;
        assert speechTrailer <= endSilenceTime : "speechTrailer must be <= endSilenceTime";
    }

    public QuickSpeechMarker() {
    	outputQueue = new ArrayDeque<Data>();
    	buffer = new ArrayList<Data>();
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        startSpeechTime = ps.getInt(PROP_START_SPEECH);
        endSilenceTime = ps.getInt(PROP_END_SILENCE);
        speechLeader = ps.getInt(PROP_SPEECH_LEADER);
        speechTrailer = ps.getInt(PROP_SPEECH_TRAILER);
        assert speechTrailer <= endSilenceTime : "speechTrailer must be <= endSilenceTime";
    }


    /**
     * Initializes this SpeechMarker
     */
    @Override
    public void initialize() {
        super.initialize();
        reset();
    }


    /**
     * Resets this SpeechMarker to a starting state.
     */
    private void reset() {
        state = State.NON_SPEECH;
        currentSilence = 0;
        currentSpeech = 0;
        outputQueue.clear();
        buffer.clear();
    }

    /**
     * Returns the next Data object.
     *
     * @return the next Data object, or null after all data has been returned
     */
    @Override
    public Data getData() {
    	while (state != State.DATA_END && outputQueue.isEmpty()) {
    		processOneInputFrame();
    	}
    //	System.err.println("buffering " + (buffer.size() + outputQueue.size()) + " elements.");
    	return nextOutputFrame();
    }
    
    /**
     * gets one Data object from outputQueue, 
     * unwraps SpeechClassifiedData and tags DataStartSignals 
     * @return the next Data object from the outputQueue
     */ 
    public Data nextOutputFrame() {
    	Data output = outputQueue.poll();
        if (output instanceof SpeechClassifiedData) {
            SpeechClassifiedData data = (SpeechClassifiedData) output;
            output = data.getDoubleData();
        } else if (output instanceof DataStartSignal)
            DataStartSignal.tagAsVadStream((DataStartSignal) output);
    	return output;
    }
    
    /** 
     * read one frame of input with all the necessary accounting.
     * this may or may not add frames to outputBuffer 
     */
    private void processOneInputFrame() {
    	Data d = readData();
		buffer.add(d); // addLast
    	if (d instanceof SpeechClassifiedData) {
    		SpeechClassifiedData scd = (SpeechClassifiedData) d;
    		updateCounters(scd);
			switch (state) {
			case IN_SPEECH:
				handleNewFrameInSpeech(scd);
				break;
			case NON_SPEECH:
				handleNewFrameNonSpeech(scd);
				break;
			}
    	} else if (d instanceof DataEndSignal) {
    		if (state == State.IN_SPEECH) {
        		// if we're in-speech on dataEnd, 
        		// we have to insert the SpeechEndSignal before dataEnd:
    			endOfSpeech();
    		}
    		flushBuffer();
    		state = State.DATA_END;
    	}
    }
    
    /** 
     * update the counters that track speech/silence duration.
     * depending on whether incoming audio isSpeech or not, 
     * we update currentSpeech/Silence counters correspondingly. 
     */
    private void updateCounters(SpeechClassifiedData scd) {
		int duration = getAudioTime(scd);
		boolean isSpeech = scd.isSpeech();
		if (isSpeech) {
			currentSpeech += duration;
			currentSilence = 0;
		} else { // !isSpeech
			currentSpeech = 0;
			currentSilence += duration;
		}
    }
    
    /** 
     * what to do when we're in speech. If incoming audio isSpeech, we 
     * can directly release the buffer to outputQueue. If it's not speech,
     * and the silence reaches endSilenceTime, we transition to
     * end of speech {@link QuickSpeechMarker#endOfSpeech()},
     * if it's not speech and we release the buffer *if* we are still
     * within speechTrailer amount of silence (otherwise, we have to hold
     * back the remaining frames, because a DataEndSignal may have to be 
     * inserted later. 
     * 
     * @param scd the frame at this time step
     */
    private void handleNewFrameInSpeech(SpeechClassifiedData scd) {
		boolean isSpeech = scd.isSpeech();
		if (isSpeech)
			flushBuffer();
		else { // !isSpeech
			if (currentSilence <= speechTrailer) {
				flushBuffer();
			}
			if (currentSilence >= endSilenceTime) {
				endOfSpeech();
			}
		}
	}

    /**
     * what to do when we're out-of-speech: If incoming audio isSpeech, we
     * we check whether there was startSpeechTime amount of speech yet. 
     * If so, we transition to IN_SPEECH.
     * 
     * @param scd the frame at this time step
     */
	private void handleNewFrameNonSpeech(SpeechClassifiedData scd) {
		boolean isSpeech = scd.isSpeech();
		if (isSpeech) {
			if (currentSpeech >= startSpeechTime) {
				startOfSpeech();
			}
		} else { // !isSpeech
			trimBufferToLeader();
		}
    }
    
	/** 
	 * transition from NON_SPEECH to IN_SPEECH.
	 * we insert a SpeechStartSignal into the outputQueue and release the
	 * buffer to the outputQueue
	 */
    private void startOfSpeech() {
//    	long collectTime = buffer.get(0).
    	// border case: if speech starts almost immediately, we have to 
    	// move that Signal to the outputQueue before inserting dataStart 
    	if (buffer.get(0) instanceof DataStartSignal) {
    		Data startSignal = buffer.remove(0); // removeFirst
    		outputQueue.add(startSignal); // addLast
    	}
    	long collectTime = getCollectTimeOfNextFrame();
    	outputQueue.add(new SpeechStartSignal(collectTime));
    //	System.err.println("buffer has " + buffer.size() + " elements on speech start.");
    	flushBuffer();
		state = State.IN_SPEECH;
    }
    
    /** the collection time of the next frame in the buffer */
    long getCollectTimeOfNextFrame() {
    	assert buffer.size() > 0; 
    	// we should only ever reach this code after we've just added something to the buffer
    	Data d = buffer.get(0);
    	assert d instanceof SpeechClassifiedData || d instanceof DataEndSignal : d;
    	if (d instanceof SpeechClassifiedData) {
	    	SpeechClassifiedData scd = (SpeechClassifiedData) d;
	    	return scd.getCollectTime();
    	} else {
    		DataEndSignal des = (DataEndSignal) d;
        	return des.getTime(); 
    	}
    }
    
    /** 
     * transition from IN_SPEECH to NON_SPEECH.
     * we insert a SpeechEndSignal into the outputQueue and partially release the 
     * buffer so that it contains no more than needed for the next speechLeader
     */
    private void endOfSpeech() {
    	long collectTime = getCollectTimeOfNextFrame();
    	outputQueue.add(new SpeechEndSignal(collectTime));
    	// restart silence-counting for buffer trimming in no-speech region 
    	currentSilence = 0;
    	trimBufferToLeader();
		state = State.NON_SPEECH;
    }
    
    /** flush the internal buffer to outputQueue */
    private void flushBuffer() {
    	outputQueue.addAll(buffer);
    	buffer.clear();    	
    }

    /** 
     * release the buffer so that it contains just speechLeader amount of audio
     * (and update currentSilence accordingly) 
     */
    private void trimBufferToLeader() {
		if (currentSilence >= speechLeader) {
	    	Data d = buffer.remove(0); // removeFirst
	    	outputQueue.add(d); // addLast
	    	if (d instanceof SpeechClassifiedData) {
	    		currentSilence -= getAudioTime((SpeechClassifiedData) d);
	    	}
		}
    }
    
    @Override
    public String toString() {
    	return "SpeechMarker\n\tstate is " + state + 
    			", currentSpeech " + currentSpeech + "ms, currentSilence " + currentSilence +
    			"ms\n\toutputQueue has " + outputQueue.size() + " elements: "	+ outputQueue.toString() + 
    			"\n\tbuffer has " + buffer.size() + " elements: " + buffer.toString();
    }

    /** read one data object from the predecessor in the frontend pipeline */
	private Data readData() throws DataProcessingException {
        return getPredecessor().getData();
    }

    /**
     * Returns the amount of audio data in milliseconds in the given SpeechClassifiedData object.
     *
     * @param audio the SpeechClassifiedData object
     * @return the amount of audio data in milliseconds
     */
    public int getAudioTime(SpeechClassifiedData audio) {
        return (int)
                (audio.getValues().length * 1000.0f / audio.getSampleRate());
    }
    
    public boolean inSpeech() {
        return state == State.IN_SPEECH;
    }
}
