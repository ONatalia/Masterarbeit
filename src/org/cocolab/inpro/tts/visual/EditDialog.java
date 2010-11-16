/**
 * 
 */
package org.cocolab.inpro.tts.visual;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

/**
 * an editor panel to manually edit mbrola data
 * @author timo
 */
@SuppressWarnings("serial")
public class EditDialog extends JDialog {
	
	final JEditorPane editor;
	String inputText;
	boolean aborted = false;
	
	EditDialog() {
		super((Frame) null, "Manually edit mbrola data", true);
		setMinimumSize(new Dimension(300, 300));
		setModal(true);
		editor = new JEditorPane("text/plain", null);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1; gbc.gridy = 1; 
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0f; gbc.weighty = 1.0f;
		add(new JScrollPane(editor), gbc);
		gbc.gridwidth = 1;
		gbc.gridx = 1; gbc.gridy = 2; 
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.5f; gbc.weighty = 0.0f;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.insets = new Insets(2, 2, 2, 2);
		add(new JButton(new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				aborted = true;
				setVisible(false);
			}
		}), gbc);
		gbc.gridx = 2; gbc.gridy = 2; 
		gbc.anchor = GridBagConstraints.LINE_START;
		add(new JButton(new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				aborted = false;
				setVisible(false);
			}
		}), gbc);
	}

	public void setText(String text) {
		inputText = text;
		editor.setText(text);
	}
	
	public String getText() {
		return aborted ? inputText : editor.getText();
	}

	public void setVisible() {
		aborted = true; // set this in case someone uses the window manager's close function
		setVisible(true);
	}

	public boolean aborted() {
		return aborted;
	}
}