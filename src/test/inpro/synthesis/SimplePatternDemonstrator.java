package test.inpro.synthesis;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;

public class SimplePatternDemonstrator extends PatternDemonstrator {

	ButtonGroup dirGroup = new ButtonGroup();
	Component dirButton(InstallmentAction a) {
		JToggleButton b = new JToggleButton(a);
		dirGroup.add(b);
		installmentActions.add(a);
		return b;
	}

	public SimplePatternDemonstrator() {
		super();
		add(generatedText);
		add(new JButton(new StartAction("Kreuzigung?")));
		add(new JButton(goAction));
		goAction.setEnabled(false);
		add(dirButton(new InstallmentAction("links", 3)));
		add(dirButton(new InstallmentAction("rechts", 3)));
	}

	@Override
	public void greatNewUtterance(String command) {
		installment = new IncrSysInstallmentIU(Arrays.asList("Zur Kreuzigung bitte nach <hes>", 
																	"Zur Kreuzigung bitte nach links.", 
																	"Zur Kreuzigung bitte nach rechts."));
		generatedText.setText("Zur Kreuzigung bitte nach ‹dir›");
	}
	
	/**
	 * main method for testing: creates a PentoCanvas that shows all tiles and the grid.
	 * @param args arguments are ignored
	 */
	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(new SimplePatternDemonstrator());
			}
		});
	}
}