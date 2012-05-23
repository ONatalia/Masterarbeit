package done.inpro.system.completion;

import inpro.incremental.IUModule;
import inpro.incremental.processor.AbstractFloorTracker;
import inpro.incremental.processor.AbstractFloorTracker.Signal;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class CompletionHypothesisListener extends IUModule implements AbstractFloorTracker.Listener {

	public final static Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 24); 
	
	private JLabel label;
	private String lastDisplayedString = "";
	private String lastQueryText = "";
	private List<String> completeStrings = new ArrayList<String>();
	private List<String> discardedStrings = new ArrayList<String>();
	private List<String> yesWords = new ArrayList<String>();
	private List<String> noWords = new ArrayList<String>();
	private List<WordIU> processedNoWords = new ArrayList<WordIU>();

	public CompletionHypothesisListener() {
        label = new JLabel();
        label.setSize(35, 15);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setFont(DEFAULT_FONT);
		label.setPreferredSize(new Dimension(700,40));
		label.setText("<html><span style=\"color:gray\">Speak your query…</span></html>");
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame f = new JFrame("Search…");
				f.add(label);
				f.pack();
				f.setVisible(true);
			}
		});
		completeStrings.add("Berlin Hauptbahnhof");
		completeStrings.add("Berlin Ostbahnhof");
		completeStrings.add("Berlin Bernau");
		completeStrings.add("Berlin Zoologischer Garten");
		completeStrings.add("Potsdam Platz der Einheit Ost");
		completeStrings.add("Potsdam Platz der Einheit West");
		completeStrings.add("Brandenburger Tor Berlin");
		completeStrings.add("Brandenburger Tor Potsdam");
		yesWords.add("ja");
		noWords.add("nein");
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		// build new query text from input, ignoring yes/no/silence words
		// determine if last displayed string was confirmed/discarded/neither
		StringBuilder sb = new StringBuilder();
		boolean confirmDisplayed = false;
		boolean discardDisplayed = false;
		for (IU iu : ius) {
			if (iu instanceof WordIU) {
				if (((WordIU) iu).isSilence())
					continue;
				if (yesWords.contains(((WordIU) iu).getWord())) {
					confirmDisplayed = true;
					discardDisplayed = false;
					processedNoWords.add((WordIU) iu);						
				} else if (noWords.contains(((WordIU) iu).getWord())) {
					if (!processedNoWords.contains(iu)) {
						confirmDisplayed = false;
						discardDisplayed = true;
						processedNoWords.add((WordIU) iu);							
					}
				} else {
					confirmDisplayed = false;
					discardDisplayed = false;
					sb.append(((WordIU) iu).getWord());
					sb.append(" ");						
				}
			}
		}
		String queryText = sb.toString();
		// select the last displayed string if confirmed
		// remove displayed string if discarded
		// build prefix and suffix for display
		boolean update = true;
		String pf = "";
		String sf = "";
		if (queryText.isEmpty()) {
			sf = "Speak your query…";
			lastDisplayedString = "";
		} else if (confirmDisplayed) {
			pf = lastDisplayedString;
		} else if (discardDisplayed) {
			this.discardedStrings.add(lastDisplayedString);
			for (String s : completeStrings) {
				if ((s.toLowerCase().startsWith(queryText) ||
						s.toLowerCase().equals(queryText)) &&
						!discardedStrings.contains(s)) {
					pf = s.substring(0, queryText.length());
					sf = s.substring(queryText.length());
					lastDisplayedString = s;
					break;
				}
			}
		} else if (!queryText.equals(lastQueryText)) {
			for (String s : completeStrings) {
				if ((s.toLowerCase().startsWith(queryText) ||
						s.toLowerCase().equals(queryText)) &&
						!discardedStrings.contains(s)) {
					pf = s.substring(0, queryText.length());
					sf = s.substring(queryText.length());
					lastDisplayedString = s;
					break;
				}
			}
			lastQueryText = queryText;
		} else {
			update = false;
		}
		// update display if necessary
		if (update) {
			final String prefix = pf;
			final String suffix = sf;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					label.setText("<html>" + prefix + "<span style=\"color:gray\">" + suffix + "</span></html>");
				}
			});			
		}
	}

	public void reset() {
		label.setText("");
		discardedStrings.clear();
		lastDisplayedString = "";
	}

	@Override
	public void floor(Signal signal, AbstractFloorTracker floorManager) {
		switch (signal) {
		case NO_INPUT: {
			Toolkit.getDefaultToolkit().beep();
			break;
		}
		case START: {			
			break;
		}
		case EOT_FALLING:
		case EOT_RISING:
		case EOT_NOT_RISING: {
			discardedStrings.clear();
			processedNoWords.clear();
			lastDisplayedString = "";
			lastQueryText = "";
			Toolkit.getDefaultToolkit().beep();
			break;
		}
		default: break;
		}
	}
}
