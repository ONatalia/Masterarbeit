package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;
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

	final IUList<DialogueActIU> dialogueActIUs = new IUList<DialogueActIU>();
	final List<RNLA> sentToDos = new ArrayList<RNLA>();
	
	private List<WordIU> installment = new ArrayList<WordIU>();

	/** Sets up the DM. */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		am = (AudioActionManager) ps.getComponent(PROP_AM);
		echo = ps.getBoolean(PROP_ECHO);
		logger.info("Started EchoDialogueManager");
	}

	/**
	 * Keeps the current installment hypothesis.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// faster and clearer way of saying "we keep a copy of the wordlist" 
		installment = new ArrayList<WordIU>((Collection<WordIU>)ius);
	}

	/**
	 * Resets the DM and its AM
	 */
	@Override
	public void reset() {
		super.reset();
		logger.info("DM resetting.");
		this.dialogueActIUs.clear();
		this.am.reset();
		this.installment.clear();
	}

	/**
	 * Listens for floor changes and updates the InformationState
	 */
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
				// Ok+ … wordIUs
				ourEdits = reply("BCpr.wav");
				break;
			}
			case EOT_FALLING:
			case EOT_NOT_RISING: {
				// Ok- … wordIUs
				ourEdits = reply("BCpf.wav");
			}
		}
		this.dialogueActIUs.apply(ourEdits);
		this.rightBuffer.setBuffer(this.dialogueActIUs);
		this.rightBuffer.notify(this.iulisteners);
	}
	
	/** 
	 * convenience method against code-duplication.
	 * @return a list of added DialogueActIUs
	 */
	private List<EditMessage<DialogueActIU>> reply(String filename) {
		List<IU> grin = new ArrayList<IU>(this.installment);
		String tts = this.sentenceToString();
		ArrayList<EditMessage<DialogueActIU>> ourEdits = new ArrayList<EditMessage<DialogueActIU>>();
		if (!tts.isEmpty()) {
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCpr.wav"))));
			if (echo) 
				ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "tts: " + tts))));					
		} else {
			ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"))));
		}
		return ourEdits;
	}
	
	/** Builds a simple string from currently understood installment. */
	private String sentenceToString() {
		String ret = "";
		for (WordIU iu : (ArrayList<WordIU>) this.installment) {
			if (!iu.isSilence()) {
				ret += iu.getWord() + " ";				
			}
		}
		return ret.replaceAll("^ *", "").replaceAll(" *$", "");
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