package done.inpro.system.calendar;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import inpro.apps.SimpleMonitor;
import inpro.apps.util.CommonCommandLineParser;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.audio.DispatchStream;
import inpro.incremental.processor.SynthesisModule;

public class NoisySynthesisModule extends SynthesisModule {

	protected DispatchStream noiseDispatcher;

	public NoisySynthesisModule(DispatchStream speechDispatcher) {
		super(speechDispatcher);
		noiseDispatcher = setupDispatcher2();
	}
	
	protected synchronized boolean noisy() {
		return noiseDispatcher.isSpeaking();
	}
	
	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoiseSmart(String file) {
		noiseDispatcher.playFile(file, true);
		sleepy(300);
		// TODO: interrupt ongoing utterance 
		// stop after ongoing word, 
		currentInstallment.stopAfterOngoingWord();
		// (no need to keep reference to the ongoing utterance as we'll start a new one anyway)
		currentInstallment = null;
	}

	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoiseDumb(String file) {
		noiseDispatcher.playFile(file, true);
		sleepy(300);
		speechDispatcher.interruptPlayback();
		// wait until noiseDispatcher is done
		noiseDispatcher.waitUntilDone();
		sleepy(100);
		speechDispatcher.continuePlayback();
	}
	
	public synchronized void playNoiseDeaf(String file) {
		noiseDispatcher.playFile(file, true);
	}

	void sleepy(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* wow, this is ugly. but oh well ... as long as it works */
	@SuppressWarnings("unused")
	public static DispatchStream setupDispatcher2() {
		ConfigurationManager cm = new ConfigurationManager(SimpleMonitor.class.getResource("config.xml"));
		MonitorCommandLineParser clp = new MonitorCommandLineParser(new String[] {
				"-S", "-M" // -M is just a placeholder here, it's immediately overridden in the next line:
			});
		clp.setInputMode(CommonCommandLineParser.DISPATCHER_OBJECT_2_INPUT);
		try {
			new SimpleMonitor(clp, cm);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return (DispatchStream) cm.lookup("dispatchStream2");
	}


}
