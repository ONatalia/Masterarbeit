package inpro.apps;

import inpro.apps.util.RTPCommandLineParser;
import inpro.audio.AudioUtils;
import inpro.sphinx.frontend.ConversionUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import gov.nist.jrtp.RtpException;
import gov.nist.jrtp.RtpPacket;
import gov.nist.jrtp.RtpSession;

// TODO from UCDetector: Class "SimpleRTP" has 0 references --> this should at least be referenced by a unit test!! 
public class SimpleRTP { // NO_UCD (unused code)

	private static DataProcessor getSource(ConfigurationManager cm, RTPCommandLineParser clp) throws PropertyException, UnsupportedAudioFileException, IOException {
		DataProcessor dp = null;
		switch (clp.getInputMode()) {
			case RTPCommandLineParser.MICROPHONE_INPUT:
				final Microphone mic = (Microphone) cm.lookup("microphone");
				Runnable micInitializer = new Runnable() {
					public void run() {
						mic.initialize();
					}
				};
				new Thread(micInitializer, "microphone initializer").start();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} // allow the microphone 3 seconds to initialize
				if (!mic.startRecording()) {
					System.err.println("Could not open microphone. Exiting...");
					System.exit(1);
				}
				dp = mic;
			break;
			case RTPCommandLineParser.FILE_INPUT:
				StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
				sds.initialize();
				URL audioURL = clp.getAudioURL();
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
	            sds.setInputStream(ais, audioURL.getFile());
	            dp = sds;
			break;
		}
		return dp;
	}
	
	private static void drainAndSend(DataProcessor dp, RTPCommandLineParser clp) throws DataProcessingException, SocketException, UnknownHostException, IOException, RtpException {
		// setup RTP session 
		RtpSession rs = new RtpSession(InetAddress.getLocalHost(), 
									   clp.getLocalPort(),
									   clp.getDestinationAddress(), 
									   clp.getDestinationPort());
		RtpPacket rp = new RtpPacket();
		// process frontend content
		Data d;
		while ((d = dp.getData()) != null) {
			if (d instanceof DoubleData) {
				DoubleData dd = (DoubleData) d;
				// ba as in byte array
				byte[] ba = ConversionUtil.doubleDataToBytes(dd);
				rp.setPayload(ba, ba.length);
				rs.sendRtpPacket(rp);
				try {
					Thread.sleep(5); // sleep for half a frame
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException, PropertyException, DataProcessingException, RtpException, UnsupportedAudioFileException {
    	RTPCommandLineParser clp = new RTPCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading frontend...\n");
    	DataProcessor dp = getSource(cm, clp); 
    	System.err.println("Now sending data...\n");
    	drainAndSend(dp, clp);
	}

}
