package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.sphinx.frontend.RtpRecvProcessor;
import org.cocolab.inpro.sphinx.instrumentation.NewWordNotifierAgent;


import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class SimpleReco {

	protected static void setupSource(ConfigurationManager cm, CommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
    	FrontEnd fe = (FrontEnd) cm.lookup("frontend");
		switch (clp.getInputMode()) {
			case CommandLineParser.MICROPHONE_INPUT:
				Microphone mic = (Microphone) cm.lookup("microphone");
				FrontEnd endpoint = (FrontEnd) cm.lookup("endpointing");
				endpoint.setPredecessor(mic);
				endpoint.initialize();
				fe.setPredecessor(endpoint);
				if (!mic.startRecording()) {
					System.err.println("Could not open microphone. Exiting...");
					System.exit(1);
				}
			break;
			case CommandLineParser.RTP_INPUT:
				RtpRecvProcessor rtp = (RtpRecvProcessor) cm.lookup("RTPDataSource");
				cm.setProperty("RTPDataSource",	"recvPort", "" + clp.rtpPort);
				rtp.initialize();
				endpoint = (FrontEnd) cm.lookup("endpointing");
				endpoint.setPredecessor(rtp);
				endpoint.initialize();
				fe.setPredecessor(endpoint);
			break;
			case CommandLineParser.FILE_INPUT:
				StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
				sds.initialize();
				fe.setPredecessor(sds);
				URL audioURL = clp.getAudioURL();
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
	            sds.setInputStream(ais, audioURL.getFile());
			break;
		}
	}

	private static void setupMonitors(ConfigurationManager cm, CommandLineParser clp) throws InstantiationException, PropertyException {
		if ((clp.getOutputMode() & CommandLineParser.OAA_OUTPUT) == CommandLineParser.OAA_OUTPUT) {
			@SuppressWarnings("unused")
			NewWordNotifierAgent nwna = (NewWordNotifierAgent) cm.lookup("newWordNotifierAgent");
		}
	}

	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, UnsupportedAudioFileException {
    	CommandLineParser clp = new CommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading recognizer...\n");
    	Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
    	recognizer.allocate();
    	System.err.println("Setting up source...\n");
    	setupSource(cm, clp);
    	System.err.println("Setting up monitors...\n");
    	setupMonitors(cm, clp);
    	System.err.println("Starting recognition, use Ctrl-C to stop...\n");
    	Result result;
    	do {
	    	result = recognizer.recognize();
	        if (result != null) {
	            System.err.println("RESULT: " + result.toString() + "\n");
	        } else {
	            System.err.println("Result: null\n");
	        }
    	} while (result.getDataFrames().length > 4);
    }

}
