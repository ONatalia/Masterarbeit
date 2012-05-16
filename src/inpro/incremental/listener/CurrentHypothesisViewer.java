package inpro.incremental.listener;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

import java.awt.Font;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;

public class CurrentHypothesisViewer extends PushBuffer {

	@S4Boolean(defaultValue = true)
    public final static String PROP_SHOW_WINDOW = "showWindow";
	
	public final static Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 24); 
	
	JTextField textField;
	String lastString = "";
	boolean updateResults;
	
	public CurrentHypothesisViewer() {
		textField = new JTextField("", 35);
		textField.setEditable(false);
		textField.setFont(DEFAULT_FONT);
		updateResults = true;
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		if (ps.getBoolean(PROP_SHOW_WINDOW)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JFrame f = new JFrame("current hypothesis");
					f.add(textField);
					f.pack();
					f.setVisible(true);
				}
			});
		}
	}
	
	public JTextField getTextField() {
		return textField;
	}
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (updateResults) {
			StringBuilder sb = new StringBuilder();
			for (IU iu : ius) {
				sb.append(iu.toPayLoad());
				sb.append(" ");
			}
			final String text = sb.toString();
			if (!text.equals(lastString)) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						textField.setText(text);
					}
				});
				lastString = text;
			}
		}
	}
	
	public void updateResults(boolean ur) {
		updateResults = ur;
	}
	
	public void reset() {
		textField.setText("");
	}

}
