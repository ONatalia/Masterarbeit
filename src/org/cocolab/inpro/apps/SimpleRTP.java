package org.cocolab.inpro.apps;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

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

	private static DataProcessor getSource(ConfigurationManager cm, CommandLineParser clp) throws InstantiationException, PropertyException, UnsupportedAudioFileException, IOException {
		DataProcessor dp = null;
		switch (clp.getInputMode()) {
			case CommandLineParser.MICROPHONE_INPUT:
				Microphone mic = (Microphone) cm.lookup("microphone");
				mic.initialize();
				if (!mic.startRecording()) {
					System.err.println("Could not open microphone. Exiting...");
					System.exit(1);
				}
				dp = mic;
			break;
			case CommandLineParser.FILE_INPUT:
				StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
				sds.initialize();
				URL audioURL = clp.getAudioURL();
				AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
	            sds.setInputStream(ais, audioURL.getFile());
	            dp = sds;
			break;
			case CommandLineParser.RTP_INPUT:
				System.err.println("RTP input to RTP output? You're kidding me!");
				System.exit(1);
			break;
		}
		return dp;
	}
	
	private static void drainAndSend(DataProcessor dp) throws DataProcessingException, SocketException, UnknownHostException, IOException, RtpException {
		// setup RTP session 
		RtpSession rs = new RtpSession(InetAddress.getLocalHost(), 41000, "141.89.97.19", 42000);
		RtpPacket rp = new RtpPacket();
		// process frontend content
		Data d;
		while ((d = dp.getData()) != null) {
			if (d instanceof DoubleData) {
				DoubleData dd = (DoubleData) d;
				double[] da = dd.getValues();
				byte[] ba = ConversionUtil.doublesToBytes(da);
				rp.setPayload(ba, ba.length);
				rs.sendRtpPacket(rp);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, PropertyException, InstantiationException, DataProcessingException, RtpException, UnsupportedAudioFileException {
    	CommandLineParser clp = new CommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	System.err.println("Loading frontend...\n");
    	DataProcessor dp = getSource(cm, clp); 
    	drainAndSend(dp);
	}

}
