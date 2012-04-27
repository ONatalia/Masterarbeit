package inpro.dm.acts;

/**
 * A dialogue act with previsions for speaking
 * @author okko
 *
 */
public class InformDialogueAct extends AbstractDialogueAct {
	
	/** The string representation of the utterance. Defaults to an empty string. */
	private String utterance = "";

	public InformDialogueAct() {}

	public InformDialogueAct(String utterance) {
		this.utterance = utterance;
	}

	public String getUtterance() {
		return this.utterance;
	}

	@Override
	public String toString() {
		return "Speak";
	}

}
