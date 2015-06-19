package inpro.apps.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class CommonCommandLineParser {

	public static final int UNSPECIFIED_INPUT = 0;
	public static final int FILE_INPUT = 1;
	public static final int MICROPHONE_INPUT = 2;
	public static final int RTP_INPUT = 3;
	public static final int OAA_DISPATCHER_INPUT = 4;
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
	
	public CommonCommandLineParser(String[] args) {
		success = false;
		try {
			verbose = false;
			inputMode = UNSPECIFIED_INPUT;
			configURL = CommonCommandLineParser.class.getResource("../config.xml");
			audioURL = anyToURL("file:res/DE_1234.wav");
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
	
	/**
	 * getURLForPath try to use the given path to either make
	 * directly an URL or use it as path to a file and take
	 * the URL from there
	 * @param path URL or normal path
	 * @return URL if it is able to build one directly or out of path
	 */
	protected URL anyToURL(String path)
	{
		URL result;
		//first try to read the given string as an URL
		try
		{
			result = new URL(path);
			return result;
		}
		catch(MalformedURLException e)
		{
			System.err.println(path + " is no URL - I'll try to use it as path.");
		}
		/*if it wasn't a string try to read it as file path
		 *the catching part should be useless since there will be a file not found exception
		 */
		try {
			result = new File(path).toURI().toURL();
			return result;
		} catch (MalformedURLException e) {
			System.err.println("The Argument " + path + " is no path.");
		}
		System.exit(1);
		return null;
		
	}

	
}
