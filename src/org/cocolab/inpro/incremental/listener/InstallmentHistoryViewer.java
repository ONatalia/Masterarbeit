package org.cocolab.inpro.incremental.listener;

import java.awt.Dimension;
import java.util.Collection;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.InstallmentIU;

public class InstallmentHistoryViewer extends PushBuffer {

	JEditorPane htmlPane;
	
	private static final int visibleLines = 20;
	
	public InstallmentHistoryViewer() {
		htmlPane = new JEditorPane("text/html", "");
		htmlPane.setEditable(false);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame f = new JFrame("installment history");
				htmlPane.setPreferredSize(new Dimension(400, 372));
				JScrollPane scrollPane = new JScrollPane(htmlPane);
				f.add(scrollPane);
				f.pack();
				f.setVisible(true);
			}
		});
	}
	
	@Override
	public synchronized void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (ius == null) return;
		assert (ius instanceof List<?>); 
		@SuppressWarnings("unchecked")
		List<InstallmentIU> iuList = (List<InstallmentIU>) ius;
		final StringBuilder html = new StringBuilder();
		// iterate only the visibleLines newest entries in ius 
		for (int i = Math.max(0, ius.size() - visibleLines); i < ius.size(); i++) {
			InstallmentIU iiu = iuList.get(i);
			if (iiu.systemProduced()) {
				html.append("<tt>SYS:  </tt>");
			} else {
				html.append("<tt>USER: </tt>");
			}
			html.append(iiu.toPayLoad());
			html.append("<br>");
		}
		htmlPane.setText(html.toString()); // setText is threadsafe
	}
	
	public static void main(String[] args) {
		new InstallmentHistoryViewer();
	}

}
