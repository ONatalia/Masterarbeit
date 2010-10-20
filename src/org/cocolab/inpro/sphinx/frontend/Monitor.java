package org.cocolab.inpro.sphinx.frontend;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

public class Monitor extends BaseDataProcessor {

    SourceDataLine line;
	
	@Override
	public void initialize() {
		initLogger();
		setupSpeakers();
	}
	
	/** setup output to speakers */
	private void setupSpeakers() {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, false);
		// define the required attributes for our line, 
        // and make sure a compatible line is supported.
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Line matching " + info + " not supported.");
        }
        // get and open the source data line for playback.
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            int bufferSize = 4096; 
		    logger.info("opening speaker with buffer size " + bufferSize);
		    line.open(format, bufferSize);
		    logger.info("speaker actually has buffer size " + line.getBufferSize());
        } catch (LineUnavailableException ex) { 
            throw new RuntimeException("Unable to open the line: " + ex);
        }
        // start the source data line
        line.start();
        logger.info("monitoring to speakers has started");
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();
		if (d instanceof DoubleData) {
			DoubleData dd = (DoubleData) d;
			addData(dd.getValues());
		}
		return d;
	}

    /**
     * handle incoming data: copy to lineout and/or filebuffer
     */
    void addData(double[] values) {
    	byte[] bValues = new byte[values.length * 2];
    	for (int i = 0; i < values.length; i++) {
    		int value = (int) values[i];
    		bValues[i * 2] = (byte) (value & 0xff);
    		bValues[i * 2 + 1] = (byte) (value >> 8 & 0xff);
    	}
   		line.write(bValues, 0, bValues.length);
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		URL audioURL = new URL("file:/home/timo/inpro/inpro/res/DE_1234.wav");
		AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
		StreamDataSource sds = new StreamDataSource(16000, 320, 16, false, true);
		sds.initialize();
		sds.setInputStream(ais, audioURL.getFile());
		Monitor monitor = new Monitor();
		monitor.initialize();
		monitor.setPredecessor(sds);
		Data d = null;
		do {
			d = monitor.getData();
		} while (d != null);
	}

}
