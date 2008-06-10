package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.sphinx.frontend.RTPDataSource;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {

	private static void setupSource(ConfigurationManager cm, CommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getMode()) {
			case CommandLineParser.MICROPHONE_MODE:
				Microphone mic = (Microphone) cm.lookup("microphone");
				fe.setPredecessor(mic);
				mic.initialize();
				if (!mic.startRecording()) {
					System.err.println("Could not open microphone. Exiting...");
					System.exit(1);
				}
			break;
			case CommandLineParser.FILE_MODE:
				StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
				//fe.setPredecessor(sds);
				URL audioURL = clp.getAudioURL();
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
	            sds.setInputStream(ais, audioURL.getFile());
			break;
			case CommandLineParser.RTP_MODE:
				RTPDataSource rtp = (RTPDataSource) cm.lookup("RTPDataSource");
				fe.setPredecessor(rtp);
				// TODO: add configuration for RTP connection
			break;
		}
	}

	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
    	CommandLineParser clp = new CommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading recognizer...\n");
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
    	System.err.println("Setting up frontend...\n");
    	setupSource(cm, clp);
        Result result = recognizer.recognize();
        if (result != null) {
            System.err.println("\nRESULT: " + result.getBestToken().getWordPath() + "\n");
        } else {
            System.err.println("Result: null\n");
        }
        recognizer.deallocate();

    }

}
