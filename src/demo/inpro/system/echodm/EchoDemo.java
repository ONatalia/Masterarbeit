package demo.inpro.system.echodm;

import inpro.apps.SimpleReco;
import inpro.apps.util.CommonCommandLineParser;
import inpro.apps.util.RecoCommandLineParser;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;


public class EchoDemo {
	
	private static final Logger logger = Logger.getLogger(EchoDemo.class);
	
	static ConfigurationManager cm;
	
	public static void main (String[] args) {
		
		try {
			System.setProperty("mary.host","bramaputra.ling.uni-potsdam.de");
			System.setProperty("mary.port","59125");
			//System.setProperty("mary.base","/home/kjettka/MARY_TTS");
			System.setProperty("mary.version","3");
			PropertyConfigurator.configure("log4j.properties");
			
			String [] args1 = new String[5];
			args1[0] = "-M";					//inputMode = MICROPHONE_INPUT;
		    args1[1] = "-O";					//outputMode |= DISPATCHER_OBJECT_OUTPUT;
		    args1[2] = "-Is";					//incrementalMode = SMOOTHED_INCREMENTAL;
		    args1[3] = "7";						//incrementalModifier = 7;
		    args1[4] = "-C";					//outputMode |= CURRHYP_OUTPUT;
		    
		    URL configURL = new URL("file:src/demo/inpro/system/echodm/prosody-config.xml");
		    ConfigurationManager cm=new ConfigurationManager(configURL);
			RecoCommandLineParser clp = new RecoCommandLineParser(args1);
	    	SimpleReco simpleReco = new SimpleReco(cm, clp);
	    	if (clp.isInputMode(RecoCommandLineParser.MICROPHONE_INPUT)) {
	    		System.err.println("Starting recognition, use Ctrl-C to stop...\n");
	    		simpleReco.recognizeInfinitely();
	    	} else {
	    		simpleReco.recognizeOnce();
	    	}
	    	simpleReco.getRecognizer().deallocate();
	    	System.exit(0);
		} 
		catch (PropertyException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
}