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

	final IUList<DialogueActIU> dialogueActIUs = new IUList<DialogueActIU>();
	final List<RNLA> sentToDos = new ArrayList<RNLA>();
	private List<WordIU> sentence = new ArrayList<WordIU>();

	/**
	 * Sets up the DM.
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		am = (AudioActionManager) ps.getComponent(PROP_AM);
		logger.info("Started EchoDialogueManager");
	}

	/**
	 * Builds the current sentence hypothesis.
	 */
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<? extends IU> edit : edits) {
			WordIU newWord = (WordIU) edit.getIU();
			switch (edit.getType()) {
				case REVOKE:
					this.sentence.remove(newWord);
					break;
				case ADD:
					//Start-of-speech hack until Floor.Signal START works.
					if (this.sentence.isEmpty()) {
						this.am.shutUp();
					}
					this.sentence.add(newWord);
					break;
				case COMMIT:
					break;
				default: logger.fatal("Found unimplemented EditType!");
			}
		}
	}

	/**
	 * Resets the DM and its AM
	 */
	@Override
	public void reset() {
		logger.info("DM resetting.");
		this.sentence.clear();
		this.dialogueActIUs.clear();
		this.am.reset();
	}

	/**
	 * Listens for floor changes and updates the InformationState
	 */
	@Override
	public void floor(AbstractFloorTracker.Signal signal, AbstractFloorTracker floorManager) {
		logger.info("Floor signal: " + signal);
		ArrayList<EditMessage<DialogueActIU>> ourEdits = new ArrayList<EditMessage<DialogueActIU>>();
		List<IU> grin = new ArrayList<IU>(this.sentence);
		String utterance = this.sentenceToString();
		switch (signal) {
			case NO_INPUT: {
				// First prompt "mmhh"
				ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"))));
				break;
			}
			case START: {
				// Shut up
				this.am.shutUp();
				break;
			}
			case EOT_FALLING: {
				// Ok- … wordIUs
				if (!utterance.isEmpty()) {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCpf.wav"))));
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "tts: " + utterance))));
				} else {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"))));
				}
				break;
			}
			case EOT_RISING: {
				// Ok+ … wordIUs
				if (!utterance.isEmpty()) {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCpr.wav"))));
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "tts: " + utterance))));					
				} else {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"))));
				}
				break;
			}
			case EOT_ANY: {
				// Ok- … wordIUs
				if (!utterance.isEmpty()) {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCpf.wav"))));
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "tts: " + utterance))));					
				} else {
					ourEdits.add(new EditMessage<DialogueActIU>(EditType.ADD, new DialogueActIU(this.dialogueActIUs.getLast(), grin, new RNLA(RNLA.Act.PROMPT, "BCn.wav"))));
				}
				break;
			}
		}
		this.dialogueActIUs.apply(ourEdits);
		this.rightBuffer.setBuffer(this.dialogueActIUs);
		this.rightBuffer.notify(this.iulisteners);
	}
	
	/**
	 * Builds a simple string from currently understood sentence.
	 */
	private String sentenceToString() {
		String ret = "";
		for (WordIU iu : (ArrayList<WordIU>) this.sentence) {
			if (!iu.isSilence()) {
				ret += iu.getWord() + " ";				
			}
		}
		return ret.replaceAll("^ *", "").replaceAll(" *$", "");
	}

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