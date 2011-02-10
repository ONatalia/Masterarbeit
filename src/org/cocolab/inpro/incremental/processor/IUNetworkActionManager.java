package org.cocolab.inpro.incremental.processor;

import java.util.ArrayList;
import java.util.List;

import org.cocolab.inpro.dm.acts.AbstractDialogueAct;
import org.cocolab.inpro.dm.acts.ClarifyDialogueAct;
import org.cocolab.inpro.dm.acts.GroundDialogueAct;
import org.cocolab.inpro.dm.acts.RequestDialogueAct;
import org.cocolab.inpro.incremental.processor.AbstractFloorTracker.Signal;
import org.cocolab.inpro.incremental.unit.DialogueActIU;

/**
 * Implements utterance execution for DialogueActIUs grounded-in 
 * a wider IU network.
 * Applies basic trial intonation logic for rising vs non-rising pitch
 * and throws timeouts accordingly.
 * @author okko
 *
 */
public class IUNetworkActionManager extends AudioActionManager {

	@SuppressWarnings({"unchecked"})
	@Override
	public void floor(Signal signal, AbstractFloorTracker floorManager) {
		int timeout = 0;
		switch (signal) {
		case NO_INPUT: {
			this.playNegGroundingUtterance();
			timeout = 7000;
			break;
		}
		case START: {
			this.shutUp();
			break;
		}
		case EOT_RISING: {
			// Just a ground quickly if necessary, else grunt.
			timeout = 5000;
			boolean ground = false;
			for (DialogueActIU iu : this.toPerform) {
				if (iu.getAct() instanceof GroundDialogueAct) {
					ground = true;
				}
			}
			if (ground) {
				this.playShortPosGroundingUtterance();
			} else {
				this.playShortNegGroundingUtterance();
			}
			break;
		}
		case EOT_FALLING:
		case EOT_NOT_RISING: {
			boolean clarify = false;
			boolean request = false;
			boolean ground = false;
			List<DialogueActIU> toPerformCopy = (ArrayList<DialogueActIU>) this.toPerform.clone();
			List<DialogueActIU> performed = new ArrayList<DialogueActIU>();
			for (DialogueActIU iu : toPerformCopy) {
				AbstractDialogueAct act = iu.getAct();
				if (act instanceof GroundDialogueAct) {
					ground = true;
					this.signalListeners(iu);
//					iu.commit();
					performed.add(iu);
				} else  if (act instanceof ClarifyDialogueAct) {
					clarify = true;
					performed.add(iu);
//					iu.commit();
				} else if (act instanceof RequestDialogueAct) {
					request = true;
					performed.add(iu);
//					iu.commit();
				}
			}
			if (ground) {
				// play all grounding acts…
				this.playPosGroundingUtterance();
				timeout = 5000;
			}
			if (clarify) {
				// …then clarification act or (if none)…
				this.playClarificationUtterance();
				timeout = 8000;
			} else if (request) {
				// …then the last request act.
				this.playRequestUtterance();
				timeout = 8000;
			} else {
				 // or a quick Q if there was no new DA to perform.
				this.playNegGroundingUtterance();
				timeout = 8000;
			}
			logger.info("Clearing todo…");
			this.toPerform.clear();
			this.toPerform.removeAll(performed);
			break;
		}
		}
		if (timeout != 0) {
			logger.info("Setting new noinput timeout of " + timeout + "ms…");
			this.floorTracker.installInputTimeout(timeout);
		}
	}

	protected void playNoInputUtterance() {
		// Ja? Hallo?
		this.playSystemUtterance("Ja? Hallo?");
	}

	
	protected void playShortPosGroundingUtterance() {
		// Gut!
		this.playSystemUtterance("Gut!");
	}
	
	protected void playShortNegGroundingUtterance() {
		// Und?
		this.playSystemUtterance("Und?");
	}
	
	protected void playPosGroundingUtterance() {
		// Verstehe.
		this.playSystemUtterance("Verstehe.");
	}

	protected void playNegGroundingUtterance() {
		// Wie bitte?
		this.playSystemUtterance("Wie bitte?");
	}
	
	protected void playClarificationUtterance() {
		// Was meintene Sie damit?
		this.playSystemUtterance("Was meintene Sie damit?");
	}

	protected void playRequestUtterance() {
		// Wie kann ich Ihnen noch helfen?
		this.playSystemUtterance("Ich benoetige noch mehr Informationen!");
	}
	
	protected void playSystemUtterance(String string) {
		if (utteranceMap.containsKey(string)) {
			logger.info("Playing from file " + string);
			this.audioDispatcher.playFile(utteranceMap.get(string).toString(), false);
		} else {
			logger.info("Playing via tts " + string);
			this.audioDispatcher.playTTS(string, false);					
		}
	}

}
