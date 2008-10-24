package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.cocolab.inpro.apps.util.RTPCommandLineParser;
import org.cocolab.inpro.audio.AudioUtils;
import org.cocolab.inpro.sphinx.frontend.ConversionUtil;


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

public class SimpleRTP {

	private static DataProcessor getSource(ConfigurationManager cm, RTPCommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
		DataProcessor dp = null;
		switch (clp.getInputMode()) {
			case RTPCommandLineParser.MICROPHONE_INPUT:
				Microphone mic = (Microphone) cm.lookup("microphone");
				mic.initialize();
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
				byte[] ba = ConversionUtil.doubleDataToBytes(dd);
				rp.setPayload(ba, ba.length);
				rs.sendRtpPacket(rp);
				try {
					Thread.sleep(5); // sleep for half a frame
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, DataProcessingException, RtpException, UnsupportedAudioFileException {
    	RTPCommandLineParser clp = new RTPCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading frontend...\n");
    	DataProcessor dp = getSource(cm, clp); 
    	System.err.println("Now sending data...\n");
    	drainAndSend(dp, clp);
	}

}
