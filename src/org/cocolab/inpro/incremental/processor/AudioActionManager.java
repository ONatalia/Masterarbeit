package org.cocolab.inpro.incremental.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.incremental.IUModule;
import org.cocolab.inpro.incremental.processor.AbstractFloorTracker.Signal;
import org.cocolab.inpro.incremental.unit.DialogueActIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.IUList;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

public class AudioActionManager extends IUModule implements AbstractFloorTracker.Listener {

	/** Configuration for reading utterance audio files from an external file. */
	@S4String(mandatory = false)
	public static final String PROP_UTTERANCE_MAP = "utteranceMap";
	private static String utteranceMapFile;

	@S4ComponentList(type = Listener.class)
	public final static String PROP_STATE_LISTENERS = "amListeners";
	public List<Listener> amListeners;

	@S4Component(type = DispatchStream.class)
	public static final String PROP_DISPATCHER = "dispatchStream";
	protected DispatchStream audioDispatcher;

	@S4String(mandatory = false)
	public static final String PROP_AUDIO_PATH = "audioPath";
	protected static String audioPath;

	/** A map of utterance strings and corresponding audio files. */
	private static Map<String, File> utteranceMap = new HashMap<String, File>();

	/** List of dialogue act IUs to perform whenever possible. */
	private IUList<DialogueActIU> toPerform = new IUList<DialogueActIU>();

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		audioDispatcher = (DispatchStream) ps.getComponent(PROP_DISPATCHER);
		amListeners = ps.getComponentList(PROP_STATE_LISTENERS, Listener.class);
		audioPath = ps.getString(PROP_AUDIO_PATH);
		utteranceMapFile = ps.getString(PROP_UTTERANCE_MAP);
		if (utteranceMapFile != null) {
			this.loadUtteranceMap();
		} else {
			logger.info("Not loading utterance files.");
		}

		logger.info("Started AudioActionManager");
	}

	/**
	 * Reads file names corresponding to utterances strings from a file
	 * and adds them to a local map if they can be found.
	 */
	private void loadUtteranceMap() {
		try {
			URL url = new URL(utteranceMapFile);
			File file = new File(url.toURI());
			if (file.exists()) {
				this.logger.info("Loading utterance map.");
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line;
				while ((line = br.readLine()) != null) {
					String utterance = line.split(",")[0];
					File audio = new File(line.split(",")[1]);
					if (audio.exists()) {
						utteranceMap.put(utterance, audio);
					} else if (audioPath != null) {
						audio = new File(audioPath + audio.toString());
						if (audio.exists()) {
							utteranceMap.put(utterance, new File(audioPath + audio.toString()));
						} else {
							this.logger.warn("Cannot find and won't add audio file " + audio.toString());
						}
					} else {
						this.logger.warn("Cannot find and won't add audio file " + audio.toString());
					}
			    }
			} else {
				this.logger.warn("Cannot find file " + utteranceMapFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remembers what the current left buffer IUs are.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<? extends IU> edit : edits) {
			this.toPerform.apply((EditMessage<DialogueActIU>) edit);
			// TODO: DM ISU may have revoked some of these without sending a message.
		}
	}

	/**
	 * When the floor changes, react:</ br>
	 * If floor is taken, be quiet</ br>
	 * If floor is available, speak queued IUs' utterance (from file if one is available, via TTS otherwise).</ br>
	 */
	@Override
	public void floor(Signal signal, AbstractFloorTracker floorManager) {
		switch (signal) {
		case NO_INPUT: {
			break;
		}
		case START: {
			logger.info("Shutting up");
			this.shutUp();
			break;
		}
		case EOT_FALLING:
		case EOT_RISING:
		case EOT_NOT_RISING: {
			for (DialogueActIU iu : this.toPerform) {
				logger.info("Playing " + iu.getUtterance());
				if (utteranceMap.containsKey(iu.getUtterance())) {
					logger.info("…from file");
					this.audioDispatcher.playFile(utteranceMap.get(iu.getUtterance()).toString(), false);
				} else {
					logger.info("…from tts");
					this.audioDispatcher.playTTS(iu.getUtterance(), false);					
				}
//				logger.info("Playing silence");
//				this.audioDispatcher.playSilence(500, false);
			}
			logger.info("Clearing todo…");
			this.toPerform.clear();
			logger.info(this.toPerform.toString());
			break;
		}
		}
	}
	
	/**
	 * Notifies listeners that a DialogueActIU has been performed.
	 * @param iu the performed IU.
	 */
	protected void signalListeners(DialogueActIU iu) {
		logger.debug("Notifying listeners about " + iu.toString());
		for (Listener l : amListeners) {
			l.done(iu);
		}
	}
	
	/**
	 * Shuts up current audio by clearing dispatcher's audio stream.
	 */
	public void shutUp() {
		this.audioDispatcher.clearStream();
	}

	/**
	 * Interface for dialogue manager (or other previous modules) to implement.
	 * Gets called when a dialogue act from a DialogueActIU was performed successfully.
	 * @author okko
	 *
	 */
	public interface Listener extends Configurable {
		public void done(DialogueActIU iu);
	}

}
