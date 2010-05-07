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
import javax.sound.sampled.TargetDataLine;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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

/**
 * SimpleMonitor is kind of a "mixer" application with several 
 * input ports (microphone, OAA-goals, RTP) and several 
 * output ports (speakers or file).  
 * 
 * More or less, the software works as follows (and should probably be 
 * refactored to be self-explanatory)
 * 
 * createMicrophoneSource()
 * createDispatcherSource()
 * 
 * 
 * @author timo
 */

public class SimpleMonitor implements RtpListener {

	private static final Logger logger = Logger.getLogger(SimpleMonitor.class);
	
	/* make these variables global, so that they are accessible from sub functions */
	MonitorCommandLineParser clp;
    SourceDataLine line;
    FileOutputStream fileStream;

    SimpleMonitor(MonitorCommandLineParser clp) throws RtpException, IOException, PropertyException, InstantiationException {
		this.clp = clp;
		logger.info("Setting up output stream...\n");
		if (clp.matchesOutputMode(CommonCommandLineParser.FILE_OUTPUT)) {
			logger.info("setting up file output to file " + clp.getAudioURL().toString());
		    setupFileStream();
		}
		if (clp.matchesOutputMode(CommonCommandLineParser.SPEAKER_OUTPUT)) {
			logger.info("setting up speaker output");
			setupSpeakers();
		}
		switch (clp.getInputMode()) {
			case CommonCommandLineParser.RTP_INPUT :
				createRTPSource();
			break;
			case CommonCommandLineParser.OAA_DISPATCHER_INPUT : 
				Runnable streamDrainer = createDispatcherSource();
				new Thread(streamDrainer).run();
			break;
			case CommonCommandLineParser.MICROPHONE_INPUT:
				streamDrainer = createMicrophoneSource();
				new Thread(streamDrainer).run();
			break;
			default: throw new RuntimeException("oups in SimpleMonitor"); 
		}
	}
    
    /* setup of output streams */
    
	/** setup output to file */
	void setupFileStream() {
		fileStream = null;
		try {
			fileStream = new FileOutputStream(clp.getAudioURL().getFile());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** setup output to speakers */
	void setupSpeakers() {
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
		    logger.info("opening speaker with buffer size " + clp.getBufSize());
		    line.open(format, clp.getBufSize());
		    logger.info("speaker actually has buffer size " + line.getBufferSize());
        } catch (LineUnavailableException ex) { 
            throw new RuntimeException("Unable to open the line: " + ex);
        }
        // start the source data line
        line.start();
	}
	
	/** defines the supported audio format */
    AudioFormat getFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float rate = 16000.f;
		int sampleSize = 16;
		int channels = 1;
		boolean bigEndian = clp.isInputMode(MonitorCommandLineParser.RTP_INPUT);
		return new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize/8)*channels, rate, bigEndian);
    }
    
    /* setup of input streams */
    
	/** 
	 * returns a runnable that will continuously read data from the microphone
	 * and append the data read to the output stream(s) using newData() (see below) 
	 * @return a Runnable that continuously drains the microphone and pipes
	 *         its data to newData()
	 */
	Runnable createMicrophoneSource() {
		AudioFormat format = getFormat();
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		Runnable streamDrainer = null;
		if (!AudioSystem.isLineSupported(info)) {
			logger.error("cannot acquire line for microphone input");
		}
		try {
		    final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		    logger.info("opening microphone with buffer size " + clp.getBufSize());
		    line.open(format, clp.getBufSize());
		    logger.info("microphone actually has buffer size " + line.getBufferSize());
			streamDrainer = new Runnable() {
				@Override
				public void run() {
				    logger.info("opened microphone, now starting.");
				    line.start();
					byte[] b = new byte[320]; // that will fit 10 ms
					while (true) {
						int bytesRead = 0;
						bytesRead = line.read(b, 0, b.length);
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
		} catch (LineUnavailableException ex) {
			logger.error("cannot acquire line for microphone input");
		}
		return streamDrainer;
	}

	/**
	 * returns a runnable that will read data from an OAADispatchStream (which
	 * in turn either returns silence, sine waves, or data from audio files, 
	 * depending on how it is instructed via OAA
	 * @return a Runnable that pipes its data to newData()
	 */
	Runnable createDispatcherSource() throws IOException, PropertyException, InstantiationException {
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
		final OAADispatchStream ods = (OAADispatchStream) cm.lookup("oaaDispatchStream");
		ods.initialize();
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
							Thread.sleep(40);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
			}
		};	
		return streamDrainer;
	}

    /** creates an RTP session that piepes incoming audio to newData() */
	void createRTPSource() throws SocketException, UnknownHostException, RtpException {
		RtpSession rs = new RtpSession(InetAddress.getLocalHost(), clp.getLocalPort());
		rs.addRtpListener(this);
		rs.receiveRTPPackets();
	}
	
    /************************
     * RtpListener interface (this is used in RTP input mode)
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
	

	
    /**
     * handle incoming data: copy to lineout and/or filebuffer
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

	/*******************
	 * main
	 *******************/
	public static void main(String[] args) {
    	MonitorCommandLineParser clp = new MonitorCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
		PropertyConfigurator.configure("log4j.properties");
    	try {
			new SimpleMonitor(clp);
			logger.info("up and running");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
