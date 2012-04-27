package inpro.incremental.processor;

import inpro.audio.DispatchStream;
import inpro.dm.acts.InformDialogueAct;
import inpro.incremental.listener.InstallmentHistoryViewer;
import inpro.incremental.unit.DialogueActIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IUList;
import inpro.incremental.unit.InstallmentIU;
import inpro.incremental.unit.WordIU;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * Echo DM that sends prompts on timeouts repeating last input preceded by a short
 * confirmation with prosodic mirroring of input.
 * @author timo, okko
 */
public class EchoDialogueManager extends AbstractDialogueManager implements AbstractFloorTracker.Listener {

	@S4Boolean(defaultValue = true)
	public static final String PROP_ECHO = "echo";
	private boolean echo;

	@S4Component(type = DispatchStream.class)
	public static final String PROP_DISPATCHER = "dispatchStream";
	private DispatchStream audioDispatcher;

	@S4Component(type = IUBasedFloorTracker.class)
	public static final String PROP_FLOOR_TRACKER = "floorTracker";
	private IUBasedFloorTracker floorTracker;

	private final IUList<InstallmentIU> installments = new IUList<InstallmentIU>();	
	private final IUList<DialogueActIU> dialogueActIUs = new IUList<DialogueActIU>();
	private List<WordIU> currentInstallment = new ArrayList<WordIU>();
	private InstallmentHistoryViewer ihv = new InstallmentHistoryViewer();

	/** Sets up the DM. */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		this.echo = ps.getBoolean(PROP_ECHO);
		this.audioDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
		this.floorTracker = (IUBasedFloorTracker) ps.getComponent(PROP_FLOOR_TRACKER);
		this.reply("Hallo! Bitte nennen Sie mir Ihre Kontonummer!");
		this.floorTracker.installInputTimeout(12000);
		logger.info("Started EchoDialogueManager");
	}

	/** Keeps the current installment hypothesis. */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		this.currentInstallment = new ArrayList<WordIU>((Collection<WordIU>) ius);
	}

	/** Resets the DM */
	@Override
	public void reset() {
		logger.info("Reset EchoDialogueManager");
		this.dialogueActIUs.clear();
		this.currentInstallment.clear();
	}

	/** Listens for floor changes and updates the InformationState */
	@Override
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
		switch (signal) {
		case START: {
			this.audioDispatcher.clearStream();
			break;
		}
		case NO_INPUT: {
			this.reply(null);
			this.floorTracker.installInputTimeout(12000);
			break;
		}
		case EOT_RISING: {
			this.reply("Ja?");
			this.floorTracker.installInputTimeout(2500);
			break;
		}
		case EOT_FALLING:
		case EOT_NOT_RISING: {
			if (WordIU.wordsToString(currentInstallment).isEmpty()) {
				// Only recognized silence words
				this.reply("hm");
			} else {
				this.reply("OK!");				
			}
			this.floorTracker.installInputTimeout(5000);
			break;	
		}
		}
	}
	
	/** 
	 * Convenience method that tracks system installments and dialogue acts
	 * and produces audio output.
	 * @param the system utterance
	 */
	private void reply(String systemUtterance) {
		List<IU> grin = new ArrayList<IU>(this.currentInstallment);
		String userUtterance = WordIU.wordsToString(currentInstallment);
		ArrayList<EditMessage<DialogueActIU>> ourEdits = new ArrayList<EditMessage<DialogueActIU>>();
		if (!userUtterance.isEmpty()) {
			System.err.println(userUtterance);
			// Something to echo
			this.installments.add(new InstallmentIU(new ArrayList<WordIU>(currentInstallment)));
			DialogueActIU daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new InformDialogueAct(systemUtterance));
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
			this.installments.add(new InstallmentIU(daiu, systemUtterance));
			this.audioDispatcher.playTTS(systemUtterance, false);
			if (echo) { 
				daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new InformDialogueAct(userUtterance));
				ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
				installments.add(new InstallmentIU(daiu, userUtterance));
				this.audioDispatcher.playTTS(userUtterance, false);
			}
		} else if (systemUtterance != null && userUtterance.isEmpty() ) {
			// Nothing to echo, but something to say
			DialogueActIU daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, null);
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
			installments.add(new InstallmentIU(daiu, systemUtterance));
			this.audioDispatcher.playTTS(systemUtterance, false);
		} else {
			// Nothing new to say, nothing to echo
			DialogueActIU daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new InformDialogueAct("Hallo, sind sie noch da?"));
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
			installments.add(new InstallmentIU(daiu, "Hallo, sind sie noch da?"));
			this.audioDispatcher.playTTS("Hallo, sind sie noch da?", false);
		}
		this.dialogueActIUs.apply(ourEdits);
		this.ihv.hypChange(this.installments, null);
		this.currentInstallment.clear();
	}

}