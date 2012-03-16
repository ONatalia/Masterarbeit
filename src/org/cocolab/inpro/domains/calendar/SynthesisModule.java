package org.cocolab.inpro.domains.calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.apps.SimpleMonitor;
import org.cocolab.inpro.apps.util.CommonCommandLineParser;
import org.cocolab.inpro.apps.util.MonitorCommandLineParser;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;
import org.cocolab.inpro.incremental.unit.IU.Progress;
import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;
import org.cocolab.inpro.tts.MaryAdapter;

import edu.cmu.sphinx.util.props.ConfigurationManager;

public class SynthesisModule extends IUModule {

	DispatchStream speechDispatcher;
	DispatchStream noiseDispatcher;
	
	ArrayList<PhraseIU> upcomingPhrases;

	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		noiseDispatcher = setupDispatcher2();
		speechDispatcher = SimpleMonitor.setupDispatcher();

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					System.out.println("sleeping for 5 seconds");
					Thread.sleep(10000);
					System.out.println("slept for 5 seconds");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				noiseDispatcher.playFile("file:/home/timo/uni/experimente/050_itts+inlg/audio/pinknoise.1750ms.wav", true);
			}
		};
		t.start();
		System.out.println("done setting up sleeper");
		MaryAdapter.initializeMary(); // preload mary
	}
	
	/* wow, this is ugly. but oh well... */
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
	

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<?> em : edits) {
			final PhraseIU phraseIU = (PhraseIU) em.getIU();
			switch (em.getType()) {
			case ADD:
				IncrSysInstallmentIU instIU = new IncrSysInstallmentIU(phraseIU.toPayLoad());
				instIU.getFinalWord().getLastSegment().addUpdateListener(new IUUpdateListener() {
					@Override
					public void update(IU updatedIU) {
						if (updatedIU.isOngoing()) {
							phraseIU.setProgress(Progress.COMPLETED);
						}
					}
				});
				speechDispatcher.playStream(instIU.getAudio(), false);
				phraseIU.setProgress(Progress.ONGOING);
				System.err.println("ADD " + phraseIU.toPayLoad() + " (" + phraseIU.status + ")");
				break;
			case REVOKE:
				System.out.println("   REVOKE " + phraseIU.toPayLoad() + " (" + phraseIU.status + ")");
				System.err.println("   REVOKE " + phraseIU.toPayLoad() + " (" + phraseIU.status + ")");
				break;
			}
		}
	}
	
	/**
	 * @param args
	 *
	public static void main(String[] args) {
		SynthesisModule sm = new SynthesisModule();
		List<PhraseIU> phrases = new ArrayList<PhraseIU>();
		phrases.add(new PhraseIU("Hallo Timo", PhraseIU.PhraseStatus.NORMAL));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.add(new PhraseIU("Wie geht's?", PhraseIU.PhraseStatus.PROJECTED));
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);
		phrases.remove(1);
		sm.rightBuffer.setBuffer(phrases);
		sm.rightBuffer.notify(sm);		
	}*/

}
