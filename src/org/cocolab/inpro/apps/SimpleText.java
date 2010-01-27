package org.cocolab.inpro.apps;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.cocolab.inpro.apps.util.TextCommandLineParser;
import org.cocolab.inpro.incremental.CurrentASRHypothesis;
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.listener.HypothesisChangeListener;
import org.cocolab.inpro.incremental.unit.AtomicWordIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;


@SuppressWarnings("serial")
public class SimpleText extends JPanel implements ActionListener {

	private static final Logger logger = Logger.getLogger(SimpleText.class);

	@S4Component(type = CurrentASRHypothesis.class)
	public final static String PROP_CURRENT_HYPOTHESIS = "currentASRHypothesis";
	
	@S4ComponentList(type = HypothesisChangeListener.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";

	IUDocument iuDocument;
	JTextField textField;
	
	SimpleText() {
		iuDocument = new IUDocument();
		iuDocument.listeners = new ArrayList<PushBuffer>();
		textField = new JTextField(60);
		textField.setFont(new Font("Dialog", Font.BOLD, 24));
		textField.setDocument(iuDocument);
		textField.addActionListener(this);
		JButton commitButton = new JButton("Commit");
		commitButton.addActionListener(this);
		add(textField);
		add(commitButton);
		// add(new JLabel("you can only edit at the right"));
	}
	
	public void actionPerformed(ActionEvent arg0) {
		iuDocument.commit();
		textField.requestFocusInWindow();
	}
	
	public static void createAndShowGUI(List<PushBuffer> listeners) {
		JFrame frame = new JFrame("SimpleType");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SimpleText contentPane = new SimpleText();
        contentPane.iuDocument.setListeners(listeners);
        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        //Display the window.
        frame.pack();
        frame.setVisible(true);

	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
        TextCommandLineParser clp = new TextCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); }
    	ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	PropertySheet ps = cm.getPropertySheet(PROP_CURRENT_HYPOTHESIS);
    	@SuppressWarnings("unchecked")
    	final List<PushBuffer> listeners = (List<PushBuffer>) ps.getComponentList(PROP_HYP_CHANGE_LISTENERS);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                 createAndShowGUI(listeners);
            }
        });
	}

	/**
	 * An IUDocument stores a list of current IUs, 
	 * the string for the (partial) next IU
	 * 
	 * also, it handles HypothesisChangeListeners
	 * which are notified, whenever the IUList changes
	 * 
	 * @author timo
	 */
	static class IUDocument extends PlainDocument {

		List<PushBuffer> listeners;
		IUList<AtomicWordIU> wordIUs = new IUList<AtomicWordIU>();
		List<EditMessage<AtomicWordIU>> edits = new ArrayList<EditMessage<AtomicWordIU>>();
		String currentWord = "";
		
		int currentFrame = 0;
		
		public void setListeners(List<PushBuffer> listeners) {
			this.listeners = listeners;
		}
		
		public void notifyListeners() {
			if (edits.size() > 0) {
				logger.debug("notifying about" + edits);
				currentFrame += 100;
				for (PushBuffer listener : listeners) {
					if (listener instanceof HypothesisChangeListener) {
						((HypothesisChangeListener) listener).setCurrentFrame(currentFrame);
					}
					// notify
					if (wordIUs != null && edits != null)
						listener.hypChange(wordIUs, edits);
					
				}
				edits = new ArrayList<EditMessage<AtomicWordIU>>();
			}
		}
		
		public void commit() {
			// handle last word (if there is one)
			if (!"".equals(currentWord)) {
				addCurrentWord();
			}
			// add commit messages
			for (AtomicWordIU iu : wordIUs) {
				edits.add(new EditMessage<AtomicWordIU>(EditType.COMMIT, iu));
				iu.update(EditType.COMMIT);
			}
			// notify
			notifyListeners();
			// reset
			try {
				super.remove(0,getLength());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			wordIUs = new IUList<AtomicWordIU>();
			edits = new ArrayList<EditMessage<AtomicWordIU>>();			
		}
		
		/* 
		 * only allow removal at the right end
		 * and correctly handle removals beyond the current word
		 */
		@Override
		public void remove(int offs, int len) throws BadLocationException {
			if (offs + len == getLength()) { // only allow removal at the right end
				super.remove(offs, len);
//				if (getText(getLength() - 1, 1).)
				while (len > currentWord.length()) { // +1 because the whitespace has to be accounted for
					len -= currentWord.length();
					len--; // to account for whitespace
					AtomicWordIU iu = wordIUs.remove(wordIUs.size() - 1);
					EditMessage<AtomicWordIU> edit = new EditMessage<AtomicWordIU>(EditType.REVOKE, iu);
					edits.add(edit);
					logger.debug(edit.toString());
					currentWord = iu.getWord();
				}
				currentWord = currentWord.substring(0, currentWord.length() - len);
				logger.debug("now it's " + currentWord);
				notifyListeners();
			}
		}

		/* 
		 * only allow insertion at the right end
		 */
		@Override
		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			if (offs == getLength()) {
				/* character by character: 
				 *    check validity (for example, we don't like multiple whitespace)
				 *    add them to the current word, if it's not whitespace
				 *    add current word to IUList if it is whitespace
				 * finally, add the (possibly changed characters) to the superclass's data handling
				 */
				char[] chars = str.toCharArray();
				String outStr = "";
				for (char ch : chars) {
					if (Character.isWhitespace(ch)) {
						if (currentWord.length() > 0) {
							addCurrentWord();
							outStr += " "; // add a space, no matter what whitespace was added
						} else {
							logger.debug("ignoring additional whitespace");
						}
					} else {
						logger.debug("appending to currentWord");
						currentWord += ch;
						logger.debug("now it's " + currentWord);
						outStr += ch;
					}
					
				}
				super.insertString(offs, outStr, a);
			}
			notifyListeners();
		}
		
		private void addCurrentWord() {
			logger.debug("adding " + currentWord);
			AtomicWordIU sll = (wordIUs.size() > 0) ? wordIUs.get(wordIUs.size() - 1) : AtomicWordIU.FIRST_ATOMIC_WORD_IU;
			AtomicWordIU iu = new AtomicWordIU(currentWord, sll);
			EditMessage<AtomicWordIU> edit = new EditMessage<AtomicWordIU>(EditType.ADD, iu);
			edits.add(edit);
			wordIUs.add(iu);
			logger.debug(edit.toString());
			currentWord = "";
		}


	}
	
}
