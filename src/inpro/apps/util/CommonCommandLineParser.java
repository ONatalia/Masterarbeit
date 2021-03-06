package inpro.apps.util;

import inpro.util.PathUtil;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class CommonCommandLineParser {

	public static final int UNSPECIFIED_INPUT = 0;
	public static final int FILE_INPUT = 1;
	public static final int MICROPHONE_INPUT = 2;
	public static final int RTP_INPUT = 3;
	public static final int DISPATCHER_OBJECT_INPUT = 64;
	public static final int DISPATCHER_OBJECT_2_INPUT = 128;
	public static final int STREAM_INPUT = 256;
	
	public static final int NO_OUTPUT = 0; // NO_UCD (unused code): keep for the sake of completeness
	//public static final int OAA_OUTPUT = 1; // we don't support OAA output anymore
	public static final int TED_OUTPUT = 2;
	public static final int LABEL_OUTPUT = 4;
	public static final int SPEAKER_OUTPUT = 8;
	public static final int FILE_OUTPUT = 16;
	public static final int CURRHYP_OUTPUT = 32;
	public static final int DISPATCHER_OBJECT_OUTPUT = 64;
	
	URL configURL;
	boolean verbose;
	boolean success;
	URL audioURL;
	int inputMode;
	int outputMode;
	boolean serverMode = false;
	String labelFilename;

	public CommonCommandLineParser(String[] args) {
		success = false;
		try {
			verbose = false;
			inputMode = UNSPECIFIED_INPUT;
			configURL = CommonCommandLineParser.class.getResource("../config.xml");
			audioURL = PathUtil.anyToURL("file:res/DE_1234.wav");
			parse(args);
			success = checkConfiguration();
		} catch (Exception e) {
			printUsage();
			e.printStackTrace();
		}
	}

	abstract void printUsage();
	
	abstract boolean checkConfiguration();
	
	abstract void parse(String[] args) throws MalformedURLException;
	
	public boolean parsedSuccessfully() {
		return success;
	}
	
	public boolean verbose() {
		return verbose;
	}
	
	public URL getConfigURL() {
		return configURL;
	}
	
	public URL getAudioURL() {
		return audioURL;
	}
	
	public int getInputMode() {
		return inputMode;
	}
	
	public boolean isInputMode(int inputMode) {
		return this.inputMode == inputMode;
	}

	public boolean matchesOutputMode(int mode) {
		return (outputMode & mode) == mode;
	}

	public String getLabelPath() {
		return labelFilename;
	}

	public void setLabelPath(String labelPath) {
		this.labelFilename = labelPath;
	}	

}
