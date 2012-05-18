package inpro.sphinx.frontend;

import java.io.PrintWriter;
import java.net.Socket;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.WavWriter;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

/**
 * Tells TEDview about audio that has passed by.
 * A processor that captures all audio passing by into a temporary audio file 
 * and notifies TEDview about adding this audio to the given track.
 * @author timo
 */
public class WavTEDLogger extends WavWriter {
    @S4Integer(defaultValue = 2000)
    public final static String PROP_TED_PORT = "tedPort";
    private int tedPort;
    
    @S4String(mandatory = true)
    public final static String PROP_TED_TRACK = "tedTrack";
    private String tedTrack;
    
    private boolean tedOutput = true;

    private long chunkStartTime = -1;
    
    private String mostRecentFilename;
    
    public WavTEDLogger() {
    	dumpFilePath = "";
    	final WavTEDLogger self = this;
		Runnable shutdownHook = new Runnable() {
			public void run() {
				try {
					self.finalize();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
    }
    
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		tedPort = ps.getInt(PROP_TED_PORT);
		tedTrack = ps.getString(PROP_TED_TRACK);
		captureUtts = ps.getBoolean(PROP_CAPTURE_UTTERANCES);
		dumpFilePath = ps.getString(PROP_OUT_FILE_NAME_PATTERN);
	}
	
	public String getMostRecentFilename() {
		return mostRecentFilename;
	}
	
	public String getDumpFilePath() {
		return dumpFilePath;
	}
	
	public void setDumpFilePath(String s) {
		dumpFilePath = s;
	}

	@Override
	protected void writeFile(String wavName) {
		mostRecentFilename = wavName;
		super.writeFile(wavName);
		logger.info("saving audio as: " + wavName);
		if (tedOutput) {
			StringBuilder sb = new StringBuilder("<event time='");
// MUSING: it's probably irrelevant, when this file was stored to disk, so we don't keep that information.
			sb.append(Long.toString(Math.round((chunkStartTime) / 16.0)));
			sb.append("' originator='");
			sb.append(tedTrack);
			sb.append("'><sound path='");
			sb.append(wavName);
			sb.append("' /></event>");
			Socket sock;
			try {
				sock = new Socket("localhost", tedPort);
				PrintWriter writer = new PrintWriter(sock.getOutputStream());
		    	writer.print(sb.toString());
		    	writer.close();
		    	sock.close();
			} catch (Exception e) {
				logger.warning("[WavTEDLogger]: Can't connect to TEDview on port " + tedPort + ". I will not try again.");
				tedOutput = false;
			}
		}
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data data = super.getData();
		if ((chunkStartTime == -1) && (data instanceof DoubleData)) {
			chunkStartTime = ((DoubleData) data).getFirstSampleNumber();
		}
		if ((data instanceof DataStartSignal) && (!captureUtts)) {
			chunkStartTime = -1;
		} else if ((data instanceof SpeechStartSignal) && (captureUtts)) {
			chunkStartTime = -1;
		}
		return data;
	}

}
