package org.cocolab.inpro.incremental.listener;

import java.awt.Font;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;

public class CurrentHypothesisViewer extends HypothesisChangeListener {

	@S4Boolean(defaultValue = true)
    public final static String PROP_SHOW_WINDOW = "showWindow";
	
	JTextField textField;
	
	public CurrentHypothesisViewer() {
		textField = new JTextField("", 35);
		textField.setEditable(false);
		textField.setFont(new Font("Dialog", Font.BOLD, 24));
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		if (ps.getBoolean(PROP_SHOW_WINDOW)) {
			JFrame f = new JFrame("current hypothesis");
			f.add(textField);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.pack();
			f.setVisible(true);
		}
	}
	
	public JTextField getTextField() {
		return textField;
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
