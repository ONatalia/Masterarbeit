package org.cocolab.inpro.training;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * MetaData about speaker and recording conditions to be used with DataCollector.
 * 
 * Asks for a bunch of meta data in a modal dialogue and returns a string representation
 * 
 * after creation call getData() for the meta data. the object should be stored
 * in the calling application so that the meta data supplied by the user can be
 * stored between calls and be used as defaults for the next call. (That way,
 * the user can just press OK for subsequent calls.  
 * 
 * the collected meta data is modelled after VoxForge's meta data for German
 * 
 * @author timo
 */
@SuppressWarnings("serial")
public class MetaData extends JDialog implements ActionListener {

	private static String[] GENDER = { "weiblich", "männlich" };
	private static String[] AGE = { "Jugendlicher", "Erwachsener", "Senior" };
	private static String[] REGION = { 
		"Westdeutschland", "Norddeutschland", "Großraum Berlin", 
		"Südostdeutschland", "Bayern", "Südwestdeutschland", 
		"Schweiz", "Österreich", "andere Region" 
	};
	private static String[] MIC = {
		"Headset", "USB-Headset", "Tischmikro", "USB-Tischmikro", 
		"Laptop-Mikro", "Webcam-Mikro", "Studio-Mikro", "anderes Mikro"
	};
	/** this option is added to the string arrays above by addComboBoxWithDefault() */
	private static String noSelection = new String("--");

	JTextField pseudonym;
	ComboBoxModel gender; 
	ComboBoxModel age;
	ComboBoxModel region;
	ComboBoxModel microphone;

	/**
	 * Create a new meta data dialogue. 
	 * once created, getData() should be called in order to get the
	 * textual representation of the meta data the user supplies
	 * @param owner the owning frame (so that modality works as advertised)
	 */
	public MetaData(JFrame owner) {
		super(owner, "Meta Data", true);
		setResizable(false); // the dialog looks stupid when resized
		setLayout(new GridBagLayout());
		GridBagConstraints gbs = new GridBagConstraints();
		gbs.gridx = 1;
		gbs.gridy = 1;
		gbs.fill = GridBagConstraints.HORIZONTAL;
		gbs.insets = new Insets(5, 5, 0, 5); // a little slack around the components
		// add the pseudonym text field:
		add(new JLabel("Pseudonym:", JLabel.TRAILING), gbs);
		pseudonym = new JTextField(10);
		gbs.gridx = 2;
		add(pseudonym, gbs);
		// add combo boxes for fixed-valued data types
		gender = addComboBoxWithDefault("Geschlecht:", GENDER, gbs);
		age = addComboBoxWithDefault("Altersgruppe:", AGE, gbs);
		region = addComboBoxWithDefault("Sprachregion:", REGION, gbs);
		microphone = addComboBoxWithDefault("Mikro-Typ:", MIC, gbs);
		// add OK button
		JButton button = new JButton("OK");
		button.addActionListener(this);
		gbs.gridx = 1;
		gbs.gridwidth = 2; // the button shall span both columns
		gbs.gridy++;
		gbs.insets = new Insets(5, 0, 0, 0); // the button shall expand to the side of the window
		add(button, gbs);
	}
	
	/**
	 * utility method to add both a label and a combo box for a given list of choices.
	 * The label is shown on the left (right-aligned) and the combo box to the right,
	 * other options are taken from the given gridbag constraints
	 * 
	 * @param label text for the label 
	 * @param choices an array of choices for the combo box. The default option
	 * noSelection will automatically be added as the first choice.
	 * @param gbs layouting constraints. This object will be changed so that the
	 * next call will not lead to overlapping components.
	 * @return the model of the combo box, from which the currently displayed
	 * option can be inferred
	 */
	private ComboBoxModel addComboBoxWithDefault(String label, String[] choices, GridBagConstraints gbs) {
		gbs.gridx = 1;
		gbs.gridy++;
		add(new JLabel(label, JLabel.TRAILING), gbs);
		JComboBox choice = new JComboBox(choices);
		choice.insertItemAt(noSelection, 0);
		ComboBoxModel model = choice.getModel(); 
		model.setSelectedItem(noSelection);
		gbs.gridx = 2;
		add(choice, gbs);
		return model;
	}	
	
	/**
	 * action handler for the OK button.
	 * hides the dialogue which makes getData() return 
	 * the currently displayed meta data.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			this.setVisible(false);
		} else {
			assert false; // this should not happen, but it's not grave enough to throw a RuntimeException
		}
	}

	/**
	 * displays the dialogue and returns a string representation of the meta data.
	 * @return meta data, via toString() method
	 */
	public String getData() {
		pack();
		setVisible(true);
		return toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Meta Data:\n");
		sb.append("Pseudonym:\t");
		sb.append(pseudonym.getText());
		sb.append("\nGender:\t");
		sb.append(gender.getSelectedItem());
		sb.append("\nAge group:\t");
		sb.append(age.getSelectedItem());
		sb.append("\nDialect region:\t");
		sb.append(region.getSelectedItem());
		sb.append("\nMicrophone type:\t");
		sb.append(microphone.getSelectedItem());
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * used in the demo application.
	 */
	public static void createAndShowGUI() {
		MetaData dialog = new MetaData((JFrame) null);
		// get dialog data (twice)
		System.err.println(dialog.getData());
		System.err.println(dialog.getData());
		System.exit(0);
	}	

	/**
	 * main method used for debugging.
	 * @param args arguments are ignored
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();	
			}
		});

	}

}
