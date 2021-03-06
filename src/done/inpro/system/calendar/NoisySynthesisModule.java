package done.inpro.system.calendar;

import java.io.InputStream;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import inpro.apps.SimpleMonitor;
import inpro.apps.util.CommonCommandLineParser;
import inpro.apps.util.MonitorCommandLineParser;
import inpro.audio.DispatchStream;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.unit.SysInstallmentIU;

public class NoisySynthesisModule extends SynthesisModule {

	protected DispatchStream noiseDispatcher;

	public NoisySynthesisModule(DispatchStream speechDispatcher) {
		super(speechDispatcher);
		noiseDispatcher = setupDispatcher2();
		// preheat mary symbolic processing, HMM optimization and vocoding
		speechDispatcher.playStream(new SysInstallmentIU("Neuer Stimulus:").getAudio());
		speechDispatcher.waitUntilDone();
	}
	
	protected synchronized boolean noisy() {
		return noiseDispatcher.isSpeaking();
	}
	
	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoiseSmart(InputStream noiseFile) {
		noiseDispatcher.playStream(noiseFile, true);
		sleepy(300);
		// stop after ongoing word, 
		currentInstallment.stopAfterOngoingWord();
		// (no need to keep reference to the ongoing utterance as we'll start a new one anyway)
		currentInstallment = null;
	}

	/** any ongoing noise is replaced with this one */
	protected synchronized void playNoiseDumb(InputStream noiseFile) {
		noiseDispatcher.playStream(noiseFile, true);
		sleepy(300);
		speechDispatcher.interruptPlayback();
		// wait until noiseDispatcher is done
		noiseDispatcher.waitUntilDone();
		sleepy(100);
		speechDispatcher.continuePlayback();
	}
	
	public synchronized void playNoiseDeaf(InputStream noiseFile) {
		noiseDispatcher.playStream(noiseFile, true);
	}

	void sleepy(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
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
