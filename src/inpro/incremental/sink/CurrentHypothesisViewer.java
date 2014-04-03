package inpro.incremental.sink;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;

import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import demo.inpro.synthesis.TreeStructuredInstallmentIU;


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;

public class CurrentHypothesisViewer extends PushBuffer {

	@S4Boolean(defaultValue = true)
    public final static String PROP_SHOW_WINDOW = "showWindow";
	
	public final static Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 24); 
	
	JEditorPane textField;
	String lastString = "";
	boolean updateResults;
			
	public CurrentHypothesisViewer() {
		textField  = new JEditorPane("text/html", "");
		textField.setPreferredSize(new Dimension(800, 40));
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
	
	public JEditorPane getTextField() {
		return textField;
	}
	
	protected void updateFromIU()
	{
		
	}
	
	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (updateResults) {
			StringBuilder sb = new StringBuilder();
			for (IU iu : ius) {
				if (iu.isCompleted()) {
					sb.append("<strong>");
					sb.append(iu.toPayLoad());
					sb.append("</strong>");
				} else if (iu.isOngoing()) {
					sb.append("<em>");
					sb.append(iu.toPayLoad());
					sb.append("</em>");
					
				} else
					sb.append(iu.toPayLoad());
				sb.append(" ");
			}
			for (EditMessage e : edits)
			{
				
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
