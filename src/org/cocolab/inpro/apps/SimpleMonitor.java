package org.cocolab.inpro.apps;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.cocolab.inpro.apps.util.CommonCommandLineParser;
import org.cocolab.inpro.apps.util.MonitorCommandLineParser;

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
	
	SimpleMonitor(MonitorCommandLineParser clp) throws SocketException, UnknownHostException, RtpException {
		this.clp = clp;
		if (clp.matchesOutputMode(CommonCommandLineParser.FILE_OUTPUT)) {
			setupFileStream();
		}
		if (clp.matchesOutputMode(CommonCommandLineParser.SPEAKER_OUTPUT)) {
			setupAudioLine();
		}
		RtpSession rs = new RtpSession(InetAddress.getLocalHost(), clp.getLocalPort());
		rs.addRtpListener(this);
		rs.receiveRTPPackets();
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
	
    static AudioFormat getFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float rate = 16000.f;
		int sampleSize = 16;
		int channels = 1;
		boolean bigEndian = true;
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
		int offset = (clp.isSphinxMode() ? 20 : 0); // dirty hack, oh well
		newData(bytes, offset, bytes.length - offset);
		 
	}

	public void handleRtpStatusEvent(RtpStatusEvent arg0) {
		if (clp.verbose()) { 
			System.err.println(arg0);
		}
	}

	public void handleRtpErrorEvent(RtpErrorEvent arg0) {
		if (clp.verbose()) { 
			System.err.println(arg0);
		}
	}

	public void handleRtpTimeoutEvent(RtpTimeoutEvent arg0) {
		if (clp.verbose()) { 
			System.err.println(arg0);
		}
	}
	
	/*******************
	 * main
	 *******************/
	public static void main(String[] args) {
    	MonitorCommandLineParser clp = new MonitorCommandLineParser(args);
    	try {
			new SimpleMonitor(clp);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
