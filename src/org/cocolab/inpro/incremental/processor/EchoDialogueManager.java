package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.listener.InstallmentHistoryViewer;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
import org.cocolab.inpro.incremental.unit.InstallmentIU;
import org.cocolab.inpro.incremental.unit.WordIU;

import org.cocolab.inpro.dm.RNLA;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * Echo DM that sends prompts on timeouts repeating last input.
 * 
 * @author timo, okko
 */
public class EchoDialogueManager extends IUModule implements AbstractFloorTracker.Listener, PentoActionManager.Listener {

	@S4Component(type = AudioActionManager.class, mandatory = true)
	public static final String PROP_AM = "actionManager";
	private AudioActionManager am;
	
	@S4Boolean(defaultValue = true)
	public static final String PROP_ECHO = "echo";
	private boolean echo;

	final IUList<InstallmentIU> installments = new IUList<InstallmentIU>();
	
	final IUList<DialogueActIU> dialogueActIUs = new IUList<DialogueActIU>();
	final List<RNLA> sentToDos = new ArrayList<RNLA>();
	
	private List<WordIU> currentInstallment = new ArrayList<WordIU>();

	InstallmentHistoryViewer ihv = new InstallmentHistoryViewer();
	
	/** Sets up the DM. */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		am = (AudioActionManager) ps.getComponent(PROP_AM);
		echo = ps.getBoolean(PROP_ECHO);
		logger.info("Started EchoDialogueManager");
	}

	/** Keeps the current installment hypothesis. */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// faster and clearer way of saying "we keep a copy of the wordlist" 
		currentInstallment = new ArrayList<WordIU>((Collection<WordIU>)ius);
	}

	/** Resets the DM and its AM */
	@Override
	public void reset() {
		super.reset();
		logger.info("DM resetting.");
		this.dialogueActIUs.clear();
		this.am.reset();
		this.currentInstallment.clear();
	}

	/** Listens for floor changes and updates the InformationState */
	@Override
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
		logger.info("Floor signal: " + signal);
		List<EditMessage<DialogueActIU>> ourEdits = null;
		switch (signal) {
			case START: {
				// Shut up	
				this.am.shutUp();
				break;
			}
			case EOT_RISING: {
				installments.add(new InstallmentIU(currentInstallment));
				// Ok+ … wordIUs
				ourEdits = reply("BCpr.wav");
				break;
			}
			case EOT_FALLING:
			case EOT_NOT_RISING: {
				installments.add(new InstallmentIU(currentInstallment));
				// Ok- … wordIUs
				ourEdits = reply("BCpf.wav");
			}
		}
		if (ourEdits != null) {
			this.dialogueActIUs.apply(ourEdits);
			this.rightBuffer.setBuffer(this.dialogueActIUs, ourEdits);
			this.rightBuffer.notify(this.iulisteners);			
		}
		ihv.hypChange(installments, null);
	}
	
	/** 
	 * convenience method against code-duplication.
	 * @return a list of added DialogueActIUs
	 */
	private List<EditMessage<DialogueActIU>> reply(String filename) {
		List<IU> grin = new ArrayList<IU>(this.currentInstallment);
		String tts = WordIU.wordsToString(currentInstallment);
		ArrayList<EditMessage<DialogueActIU>> ourEdits = new ArrayList<EditMessage<DialogueActIU>>();
		if (!tts.isEmpty()) {
			DialogueActIU daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, filename));
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
			if (filename.equals("BCpr.wav")) {
				installments.add(new InstallmentIU(daiu, "OK+"));
			} else {
				installments.add(new InstallmentIU(daiu, "OK-"));
			}
			if (echo) { 
				daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "tts: " + tts));
				ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
				installments.add(new InstallmentIU(daiu, tts));
			}
		} else {
			DialogueActIU daiu = new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"));
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, daiu));
			installments.add(new InstallmentIU(daiu, "hm"));
		}
		return ourEdits;
	}
	
	/** call this to notify the DM when a dialogueAct has been performed */
	@Override
	public void done(DialogueActIU iu) {
		RNLA r = iu.getAct();
		logger.info("Was notified about performed act " + r.toString());
		this.sentToDos.remove(r);
		if (this.sentToDos.isEmpty()) {
			this.reset();
		}
	}
	
}