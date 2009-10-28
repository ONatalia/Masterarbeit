package org.cocolab.inpro.incremental.listener;

import java.awt.Font;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

public class CurrentHypothesisViewer extends HypothesisChangeListener {

	JFrame f;
	JTextField textField;
	
	public CurrentHypothesisViewer() {
		textField = new JTextField("", 35);
		textField.setEditable(false);
		textField.setFont(new Font("Dialog", Font.BOLD, 24));
		f = new JFrame("current hypothesis");
		f.add(textField);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		StringBuilder sb = new StringBuilder();
		for (IU iu : ius) {
			assert (iu instanceof WordIU);
			sb.append(((WordIU) iu).getWord());
			sb.append(" ");
		}
		textField.setText(sb.toString());
	}

}
