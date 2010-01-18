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
import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.listener.HypothesisChangeListener;
import org.cocolab.inpro.incremental.unit.AtomicWordIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IUList;

import edu.cmu.sphinx.util.props.S4ComponentList;


@SuppressWarnings("serial")
public class SimpleType extends JPanel {

	private static final Logger logger = Logger.getLogger(SimpleType.class);

	@S4ComponentList(type = HypothesisChangeListener.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";

	SimpleType() {
		final JTextField textField = new JTextField(60);
		textField.setFont(new Font("Dialog", Font.BOLD, 24));
		textField.setDocument(new IUDocument());
		JButton commitButton = new JButton("Commit");
		commitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ("Commit".equals(arg0.getActionCommand())) {
					// TODO: commit logic
					textField.setText("");
					textField.requestFocusInWindow();
				}
			}
		});
		add(textField);
		add(commitButton);
		// add(new JLabel("you can only edit at the right"));
	}
	
	public static void createAndShowGUI() {
		BasicConfigurator.configure();
		JFrame frame = new JFrame("SimpleType");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SimpleType contentPane = new SimpleType();
        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        //Display the window.
        frame.pack();
        frame.setVisible(true);

	}
	
	public static void main(String[] args) {
		new SimpleType();
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                 createAndShowGUI();
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
		boolean hasWhitespace = true;
		
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
							logger.debug("adding " + currentWord);
							AtomicWordIU sll = (wordIUs.size() > 0) ? wordIUs.get(wordIUs.size() - 1) : AtomicWordIU.FIRST_ATOMIC_WORD_IU;
							AtomicWordIU iu = new AtomicWordIU(currentWord, sll);
							EditMessage<AtomicWordIU> edit = new EditMessage<AtomicWordIU>(EditType.ADD, iu);
							edits.add(edit);
							wordIUs.add(iu);
							logger.debug(edit.toString());
							currentWord = "";
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
		}


	}
	
}
