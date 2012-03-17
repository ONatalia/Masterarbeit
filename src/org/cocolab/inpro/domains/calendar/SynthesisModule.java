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
import org.cocolab.inpro.incremental.unit.EditType;
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

	IncrSysInstallmentIU currentInstallment;
	
	public SynthesisModule() {
		upcomingPhrases = new ArrayList<PhraseIU>();
		noiseDispatcher = setupDispatcher2();
		speechDispatcher = SimpleMonitor.setupDispatcher();
		// to be replaced by more realistic noise handling
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					System.out.println("sleeping for 15 seconds");
					Thread.sleep(15000);
					System.out.println("slept for 15 seconds");
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			//	noiseDispatcher.playFile("file:/home/timo/uni/experimente/050_itts+inlg/audio/pinknoise.1750ms.wav", true);
			}
		};
		t.start();
		MaryAdapter.initializeMary(); // preload mary
	}
	
	/* wow, this is ugly. but oh well ... as long as it works */
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
			if (em.getIU() instanceof PhraseIU) {
				final PhraseIU phraseIU = (PhraseIU) em.getIU();
				System.out.println("   " + em.getType() + " " + phraseIU.toPayLoad() + " (" + phraseIU.status + "; " + phraseIU.type + ")");
				
				switch (em.getType()) {
				case ADD:
					if (phraseIU.type == PhraseIU.PhraseType.UNDEFINED && currentInstallment.isOngoing()) {
						String fullPhrase = currentInstallment.toPayLoad() + phraseIU.toPayLoad();
						currentInstallment.addAlternativeVariant(fullPhrase);
					} else { // start a new installment
						currentInstallment = new IncrSysInstallmentIU(phraseIU.toPayLoad());
						currentInstallment.getFinalWord()
										  .getLastSegment()
										  .addUpdateListener(new NotifyCompletedOnOngoing(phraseIU));
						speechDispatcher.playStream(currentInstallment.getAudio(), false);
					}
					phraseIU.setProgress(Progress.ONGOING);
					break;
				case REVOKE:
					break;
				}
			} else if (em.getIU() instanceof NoiseIU){
				if (em.getType() == EditType.ADD) {
				noiseDispatcher.playFile("file:/home/timo/uni/experimente/050_itts+inlg/audio/pinknoise.1750ms.wav", true);
				// is it automatically set to committed?
				// is playing done asynchronously?
			}
		}
	}
	
	private class NotifyCompletedOnOngoing implements IUUpdateListener {
		PhraseIU completed;
		NotifyCompletedOnOngoing(PhraseIU notify) {
			completed = notify;
		}
		@Override
		public void update(IU updatedIU) {
			if (updatedIU.isOngoing()) {
				completed.setProgress(Progress.COMPLETED);
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
