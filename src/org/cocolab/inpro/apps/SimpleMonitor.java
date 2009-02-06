package org.cocolab.inpro.apps;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.cocolab.inpro.apps.util.CommonCommandLineParser;
import org.cocolab.inpro.apps.util.MonitorCommandLineParser;
import org.cocolab.inpro.audio.OAADispatchStream;
import org.cocolab.inpro.sphinx.frontend.ConversionUtil;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import gov.nist.jrtp.RtpErrorEvent;
import gov.nist.jrtp.RtpException;
import gov.nist.jrtp.RtpListener;
import gov.nist.jrtp.RtpPacket;
import gov.nist.jrtp.RtpPacketEvent;
import gov.nist.jrtp.RtpSession;
import gov.nist.jrtp.RtpStatusEvent;
import gov.nist.jrtp.RtpTimeoutEvent;

public class SimpleMonitor implements RtpListener {

	MonitorCommandLineParser clp;
    SourceDataLine line;
    FileOutputStream fileStream;
	
	SimpleMonitor(MonitorCommandLineParser clp) throws RtpException, IOException, PropertyException, InstantiationException {
		this.clp = clp;
		System.err.println("Setting up output stream...\n");
		if (clp.matchesOutputMode(CommonCommandLineParser.FILE_OUTPUT)) {
			setupFileStream();
		}
		if (clp.matchesOutputMode(CommonCommandLineParser.SPEAKER_OUTPUT)) {
			setupAudioLine();
		}
		switch (clp.getInputMode()) {
			case MonitorCommandLineParser.RTP_INPUT :
				RtpSession rs = new RtpSession(InetAddress.getLocalHost(), clp.getLocalPort());
				rs.addRtpListener(this);
				rs.receiveRTPPackets();
			break;
			case MonitorCommandLineParser.OAA_DISPATCHER_INPUT : 
		    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
				final OAADispatchStream ods = (OAADispatchStream) cm.lookup("oaaDispatchStream");
				ods.initialize();
				ods.sendSilence(false);
				Runnable streamDrainer = new Runnable() {
					@Override
					public void run() {
						byte[] b = new byte[320]; // that will fit 10 ms
						while (true) {
							int bytesRead = 0;
							try {
								bytesRead = ods.read(b, 0, b.length);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							if (bytesRead > 0)
								// no need to sleep, because the call to the microphone will already slow us down
								newData(b, 0, bytesRead);
							else // if there is no data, then we wait a little for data to become available (instead of looping like crazy)
								try {
									Thread.sleep(5);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
						}
					}
				};
				new Thread(streamDrainer).run();
			break;
			default: throw new RuntimeException("oups in SimpleMonitor"); 
		}
	}

	void setupFileStream() {
		try {
			fileStream = new FileOutputStream(clp.getAudioURL().getFile());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void setupAudioLine() {
		AudioFormat format = getFormat();
		// define the required attributes for our line, 
        // and make sure a compatible line is supported.
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Line matching " + info + " not supported.");
        }
        // get and open the source data line for playback.
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, clp.getBufSize());
        } catch (LineUnavailableException ex) { 
            throw new RuntimeException("Unable to open the line: " + ex);
        }
        // start the source data line
        line.start();
	}
	
    AudioFormat getFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float rate = 16000.f;
		int sampleSize = 16;
		int channels = 1;
		boolean bigEndian;
		if (clp.getInputMode() == MonitorCommandLineParser.RTP_INPUT) {
			bigEndian = true;
		} else {
			bigEndian = false;
		}
		return new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize/8)*channels, rate, bigEndian);
    }
    
    /*
     * handle incoming data
     */
    void newData(byte[] bytes, int offset, int length) {
    	assert length >= 0;
    	assert offset >= 0;
    	assert offset + length <= bytes.length;
    	if (clp.verbose()) {
    		System.err.print(".");
    	}
    	if (clp.matchesOutputMode(CommonCommandLineParser.SPEAKER_OUTPUT)) {
    		line.write(bytes, offset, length);
    	}
    	if (clp.matchesOutputMode(CommonCommandLineParser.FILE_OUTPUT)) {
    		try {
				fileStream.write(bytes, offset, length);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }

    /************************
     * RtpListener interface
     ************************/
    
	public void handleRtpPacketEvent(RtpPacketEvent arg0) {
		RtpPacket rp = arg0.getRtpPacket();
		byte[] bytes = rp.getPayload();
		int offset = (clp.isSphinxMode() ? ConversionUtil.SPHINX_RTP_HEADER_LENGTH : 0); // dirty hack, oh well
		newData(bytes, offset, bytes.length - offset);
	}

	public void handleRtpStatusEvent(RtpStatusEvent arg0) {
		if (clp.verbose()) System.err.println(arg0);
	}

	public void handleRtpErrorEvent(RtpErrorEvent arg0) {
		if (clp.verbose()) System.err.println(arg0);
	}

	public void handleRtpTimeoutEvent(RtpTimeoutEvent arg0) {
		if (clp.verbose()) System.err.println(arg0);
	}
	
	/*******************
	 * main
	 *******************/
	public static void main(String[] args) {
    	MonitorCommandLineParser clp = new MonitorCommandLineParser(args);
    	try {
			new SimpleMonitor(clp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
