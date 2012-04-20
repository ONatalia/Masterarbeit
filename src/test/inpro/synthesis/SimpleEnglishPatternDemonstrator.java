package test.inpro.synthesis;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.cocolab.inpro.incremental.unit.IncrSysInstallmentIU;

public class SimpleEnglishPatternDemonstrator extends PatternDemonstrator {

	ButtonGroup dirGroup = new ButtonGroup();
	Component dirButton(InstallmentAction a) {
		JToggleButton b = new JToggleButton(a);
		dirGroup.add(b);
		installmentActions.add(a);
		return b;
	}

	public SimpleEnglishPatternDemonstrator() {
		super();
		add(generatedText);
		add(new JButton(new StartAction("pre-syn")));
		add(new JButton(goAction));
		goAction.setEnabled(false);
		add(dirButton(new InstallmentAction("left", 3)));
		add(dirButton(new InstallmentAction("right", 3)));
	}

	@Override
	public void greatNewUtterance(String command) {
		installment = new IncrSysInstallmentIU(Arrays.asList("The car then turns <hes>", 
																	"The car then turns left.", 
																	"The car then turns right."));
		generatedText.setText("The car then turns ‹dir›");
	}
	
	/**
	 * main method for testing: creates a PentoCanvas that shows all tiles and the grid.
	 * @param args arguments are ignored
	 */
	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(new SimpleEnglishPatternDemonstrator());
			}
		});
	}
}