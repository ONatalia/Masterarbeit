package org.cocolab.inpro.incremental.processor;

import org.cocolab.inpro.dm.acts.AbstractDialogueAct;
import org.cocolab.inpro.dm.acts.ClarifyDialogueAct;
import org.cocolab.inpro.dm.acts.GroundDialogueAct;
import org.cocolab.inpro.dm.acts.RequestDialogueAct;
import org.cocolab.inpro.incremental.processor.AbstractFloorTracker.Signal;
import org.cocolab.inpro.incremental.unit.DialogueActIU;

public class IUNetworkActionManager extends AudioActionManager {

	@Override
	public void floor(Signal signal, AbstractFloorTracker floorManager) {
		int timeout = 0;
		switch (signal) {
		case NO_INPUT: {
			this.playSystemUtterance("Ja? Hallo?");
			timeout = 7000;
			break;
		}
		case START: {
			logger.info("Shutting up");
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
				this.playSystemUtterance("Verstehe.");
			} else {
				this.playSystemUtterance("Und?");
			}
			break;
		}
		case EOT_FALLING:
		case EOT_NOT_RISING: {
			String clarify = "";
			String request = "";
			String ground = "";
			for (DialogueActIU iu : this.toPerform) {
				// ignore undo acts,
				AbstractDialogueAct act = iu.getAct();
				if (act instanceof GroundDialogueAct) {
					ground += " " + iu.getUtterance();
					this.signalListeners(iu);
				} else  if (act instanceof ClarifyDialogueAct) {
					clarify = iu.getUtterance();
				} else if (act instanceof RequestDialogueAct) {
					request = iu.getUtterance();
				}
			}
			if (!ground.isEmpty()) {
				// play all grounding acts,
				this.playSystemUtterance("Gut!" + ground);
				timeout = 5000;
			}
			if (!clarify.isEmpty()) {
				// then the last clarification act or (if none),
				this.playSystemUtterance(clarify);
				timeout = 8000;
			} else if (!request.isEmpty()) {
				// then the last request act.
				this.playSystemUtterance(request);
				timeout = 8000;
			}
			logger.info("Clearing todo…");
			this.toPerform.clear();
			break;
		}
		}
		if (timeout != 0) {
			logger.info("Setting new noinput timeout of " + timeout + "ms…");
			this.floorTracker.installInputTimeout(timeout);
		}
	}
	
	private void playSystemUtterance(String string) {
		if (utteranceMap.containsKey(string)) {
			logger.info("Playing from file " + string);
			this.audioDispatcher.playFile(utteranceMap.get(string).toString(), false);
		} else {
			logger.info("Playing via tts " + string);
			this.audioDispatcher.playTTS(string, false);					
		}
	}

}
