package org.cocolab.inpro.sphinx.frontend;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import gov.nist.jrtp.RtpErrorEvent;
import gov.nist.jrtp.RtpListener;
import gov.nist.jrtp.RtpPacket;
import gov.nist.jrtp.RtpPacketEvent;
import gov.nist.jrtp.RtpSession;
import gov.nist.jrtp.RtpStatusEvent;
import gov.nist.jrtp.RtpTimeoutEvent;

/*
 * our new implementation of an RTP reader using NIST JRtp , deprecates RTPDataSource
 */

public class RtpRecvProcessor extends BaseDataProcessor {

	@S4Integer(defaultValue = 42000)
	public final static String PROP_RTP_RECV_PORT = "recvPort";
	
	private int recvPort = 42000;
	
	// temporarily store used when RTP data flows in more quickly than can be processed
	private BlockingQueue<Data> q;
	
	// 
	private long sampleNumber;
	
	private void resetFrameState() {
        sampleNumber = 0;
		q.add(new DataStartSignal(ConversionUtil.SAMPLING_RATE, sampleNumber));
	}

	/*
	 * this is called by the next data processor in the frontend
	 */
	@Override
	public Data getData() throws DataProcessingException{
		Data d = null;
		while (d == null) {
			try {
				d = q.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return d;
	}
	
	@Override
	public void initialize() {
		super.initialize();
        q = new LinkedBlockingQueue<Data>();
        resetFrameState();
        try {
			RtpSession rs = new RtpSession(InetAddress.getLocalHost(), recvPort);
			rs.addRtpListener(new RtpHandler());
			rs.receiveRTPPackets();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error setting up RtpSession in RtpRecvProcessor");
		}   
	}

	/*
	 * make sure, that nobody sets a predecessor to this data processor
	 */
	@Override
	public void setPredecessor(DataProcessor predecessor){
		if (predecessor != null) {
			throw new RuntimeException("can't set predecessor of an RtpRecvProcessor.");
		}
	}
	
	/* * configurable interface  **/

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		recvPort = ps.getInt(PROP_RTP_RECV_PORT);
	}

	/* * the handler for incoming RTP packets * */
	
	private class RtpHandler implements RtpListener {

	private void stop() {
		q.add(new DataEndSignal(sampleNumber));
		resetFrameState();
	}
	
	public void handleRtpPacketEvent(RtpPacketEvent e) {
		RtpPacket rp = e.getRtpPacket();
		byte[] payload = rp.getPayload();
		DoubleData data = ConversionUtil.bytesToDoubleData(payload);
		q.add(data);
		sampleNumber = data.getFirstSampleNumber() + data.getValues().length;
	}

	public void handleRtpErrorEvent(RtpErrorEvent e) {
		stop();
	}
	
	public void handleRtpStatusEvent(RtpStatusEvent e) {
		stop();
	}

	public void handleRtpTimeoutEvent(RtpTimeoutEvent e) {
		stop();
	}

	}
}
