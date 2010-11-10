package org.cocolab.inpro.incremental.processor;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.cocolab.inpro.dm.acts.PentoDialogueAct;
import org.cocolab.inpro.incremental.listener.InstallmentHistoryViewer;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.InstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;
import org.cocolab.inpro.nlu.AVPair;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class NumbersDialogueManager extends AbstractDialogueManager implements AbstractFloorTracker.Listener, AbstractActionManager.Listener {

	/** ActionManager configuration */
	@S4Component(type = AudioActionManager.class, mandatory = true)
	public static final String PROP_AM = "actionManager";
	private AudioActionManager am;

	/** Input/State/Output tracking variables */
	private final IUList<InstallmentIU> installments = new IUList<InstallmentIU>();
	private IUList<WordIU> currentUserUtterance = new IUList<WordIU>();
	private IUList<InstallmentIU> collectedDigits = new IUList<InstallmentIU>();
	private IUList<InstallmentIU> confirmedDigits = new IUList<InstallmentIU>();
	final IUList<DialogueActIU> dialogueActIUs = new IUList<DialogueActIU>();

	/** State variables */
	private enum State {COLLECTING, CONFIRMING}
	private State state = State.COLLECTING;

	/**
	 * Installment viewer.
	 */
	private InstallmentHistoryViewer ihv = new InstallmentHistoryViewer();

	/** Sets up the DM. */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		this.am = (AudioActionManager) ps.getComponent(PROP_AM);
	}

	/** Keeps the words for grounding the current user installment. */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		this.currentUserUtterance.addAll((Collection<WordIU>)ius);
	}

	/** Listens for floor changes and updates the InformationState */
	@SuppressWarnings("unchecked")
	@Override
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
		List<EditMessage<DialogueActIU>> ourEdits = new ArrayList<EditMessage<DialogueActIU>>();
		switch (signal) {
			case START: {
				this.am.shutUp();
				break;
			}
			case EOT_RISING:
			case EOT_FALLING:
			case EOT_NOT_RISING: {
				this.installments.add(new InstallmentIU(this.currentUserUtterance));
				this.currentUserUtterance.clear();
				this.parseInstallment(); // Do the digits magic here.
				ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, this.reply("BCpf")));
				break;
			}
		}
		this.dialogueActIUs.apply(ourEdits);
		this.rightBuffer.setBuffer(this.dialogueActIUs, ourEdits);
		this.rightBuffer.notify(this.iulisteners);
		ihv.hypChange(installments, null);
	}

	/**
	 * @return String of words
	 */
	@SuppressWarnings("unchecked")
	private void parseInstallment() {
		InstallmentIU lui = this.getLastUserInstallment();
		InstallmentIU sui = this.getLastSystemInstallment(); 
		switch(this.state) {
		case COLLECTING: {
			for (WordIU word : (List<WordIU>) lui.groundedIn().get(0).groundedIn()) {
				AVPair sem = word.getAVPairs().get(0);
				if (sem.equals("boolean:true")) {
					// ignore "yes" input in collecting state
				} else if (sem.equals("boolean:false")) {
					this.getLastUnconfirmedInstallment();
				} else if (sem.getAttribute().matches("dig")) {
					
				}
			}
			break;
		}
		case CONFIRMING: {
			for (WordIU word : (List<WordIU>) lui.groundedIn().get(0).groundedIn()) {
				AVPair sem = word.getAVPairs().get(0);
				if (sem.equals("boolean:true")) {
					// ignore "yes" input in collecting state
				} else if (sem.equals("boolean:false")) {
					this.getLastUnconfirmedInstallment();
				} else if (sem.getAttribute().matches("dig")) {
					
				}
			}
			break;
		}
		}
	}

	@SuppressWarnings("unchecked")
	private DialogueActIU reply(String file) {
		return new DialogueActIU(this.dialogueActIUs.getLast(), (List<IU>) this.getLastUserInstallment().groundedIn(), new PentoDialogueAct(PentoDialogueAct.Act.PROMPT, "BCpf.wav" ));
	}
	
	private InstallmentIU getLastUserInstallment() {
		ListIterator<InstallmentIU> i = this.installments.listIterator();
		while (i.hasPrevious()) {
			if (i.previous().userProduced()) {
				return (InstallmentIU) i;
			}
		}
		return InstallmentIU.FIRST_USER_INSTALLMENT_IU;
	}
	
	private InstallmentIU getLastSystemInstallment() {
		ListIterator<InstallmentIU> i = this.installments.listIterator();
		while (i.hasPrevious()) {
			if (i.previous().systemProduced()) {
				return (InstallmentIU) i;
			}
		}
		return InstallmentIU.FIRST_SYSTEM_INSTALLMENT_IU;
	}
	
	private InstallmentIU getLastUnconfirmedInstallment() {
		ListIterator<InstallmentIU> i = this.collectedDigits.listIterator();
		while (i.hasPrevious()) {
			if (i.previous().systemProduced()) {
				return (InstallmentIU) i;
			}
		}
		return InstallmentIU.FIRST_SYSTEM_INSTALLMENT_IU;		
	}

	/** Resets the DM and its AM */
	@Override
	public void reset() {
		super.reset();
		this.am.reset();
		this.collectedDigits.clear();
	}

	public void done(DialogueActIU iu) {}

}
