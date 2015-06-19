package demo.inpro.synthesis;

import inpro.gui.Canvas;
import inpro.gui.pentomino.PentoIcon;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;




/**
 * this prototype will allow the user to generate utterances like
 * "&lt;Action> bitte das &lt;Color> &lt;Piece>" (for now, later maybe as below)
 * <p>
 * first, something like:
 * <pre>
 *           ╱  nimm  ╲             ╱ rote  ╲  ╱   Kreuz    ╲
 * (Jetzt?) ❬          ❭ bitte das ❬  grüne  ❭❬      T       ❭ </s>
 *           ╲ lösche ╱             ╲ blaue ╱  ╲ lange Teil ╱
 *            ╲ äh ...               ╲ äh ...   ╲ äh ...
 * </pre>
 * <p>
 * "Nimm bitte das CCCC Kreuz und lege es nach LLLL."
 * with CCCC being a color (such as "rote", "grüne", "blaue"
 * and LLLL being a location (such as "oben links", or "unten rechts")
 * <p>
 * 
 * @author timo
 */
public class GreatEnglishPatternDemonstrator extends PatternDemonstrator {
	
	public static final int COLOR_POSITION = 2;
	public static final int PIECE_POSITION = 3;
	
	/** a start action that also executes goAction */
	class ImmediateStartAction extends StartAction {
		public ImmediateStartAction(String name, Icon icon) { super(name, icon); }		
		@Override
		public void actionPerformed(ActionEvent ae) {
			super.actionPerformed(ae);
			goAction.actionPerformed(ae);
		}
	}
	
	ButtonGroup actionGroup = new ButtonGroup();
	JButton actionButton(AbstractAction aa) {
		JButton b = new JButton(aa);
		actionGroup.add(b);
		return b;
	}

	ButtonGroup colorGroup = new ButtonGroup();
	JToggleButton colorButton(final Color c, final String name) {
		InstallmentAction ia = new InstallmentAction(name, COLOR_POSITION);
		installmentActions.add(ia);
		JToggleButton b = new JToggleButton(ia) {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (this.isSelected()) {
					int w = getWidth();
					int h = getHeight();
					g.setColor(c); // selected color
					g.fillRect(3, 3, w - 6, h - 6);
					g.setColor(Color.darkGray); // selected foreground color
					g.drawString(name,
							(w - g.getFontMetrics().stringWidth(name)) / 2 + 1,
							(h + g.getFontMetrics().getAscent()) / 2 - 1);
				}
			}

		};
		colorGroup.add(b);
		b.setBackground(c);
		return b;
	}
	
	ButtonGroup pieceGroup = new ButtonGroup();
	JToggleButton pieceButton(char type, String name) {
		InstallmentAction ia = new InstallmentAction(name, PIECE_POSITION);
		installmentActions.add(ia);
		JToggleButton b = new JToggleButton(ia);
		b.setIcon(new PentoIcon(7, Color.GRAY, type));
		pieceGroup.add(b);
		return b;
	}
		
	GreatEnglishPatternDemonstrator() {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 4;
		gbc.gridheight = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		add(generatedText, gbc);
		gbc.gridwidth = 1;
		gbc.gridy++;
		gbc.gridx = 2;
		add(actionButton(new ImmediateStartAction("take", new ImageIcon(Canvas.class.getResource("dragging.png")))), gbc);
		gbc.gridx++;
		add(actionButton(new ImmediateStartAction("delete", new ImageIcon(Canvas.class.getResource("cross.png")))), gbc);
		gbc.gridy++;
		gbc.gridx = 1;
		/* add(new JButton(new AbstractAction("reset") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				colorGroup.clearSelection();
			}
		}), gbc); */
		gbc.gridx++;
		add(colorButton(Color.RED, "red"), gbc);
		gbc.gridx++;
		add(colorButton(Color.BLUE, "blue"), gbc);
		gbc.gridx++;
		add(colorButton(Color.GREEN, "green"), gbc);
		gbc.gridy++;
		gbc.gridx = 1;
		/* add(new JButton(new AbstractAction("reset") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				pieceGroup.clearSelection();
			}
		}), gbc); */
		gbc.gridx++;
		add(pieceButton('X', "cross"), gbc);
		gbc.gridx++;
		add(pieceButton('T', "T"), gbc);
		gbc.gridx++;
		add(pieceButton('I', "long piece"), gbc);
	}
	
	
   /**
	 *  Nimm  ╲             ╱ rote  ╲  ╱    Kreuz    ╲
	 *         ❭ bitte das ❬  grüne  ❭❬       T       ❭ </s>
	 * Lösche ╱             ╲ blaue ╱  ╲ gerade Teil ╱
	 *                       ╲ äh ...   ╲ äh ...
	 */
	@Override
	public void greatNewUtterance(String command) {
		installment = new TreeStructuredInstallmentIU(Arrays.asList(
				"please " + command + " the, uh?", 
				"please " + command + " the red, uh?", 
				"please " + command + " the red cross",
				"please " + command + " the red T",
				"please " + command + " the red long piece",
				"please " + command + " the green, uh?",
				"please " + command + " the green cross",
				"please " + command + " the green T",
				"please " + command + " the green long piece",
				"please " + command + " the blue, uh?",
				"please " + command + " the blue cross",
				"please " + command + " the blue T",
				"please " + command + " the blue long piece"
				));
		generatedText.setText("please " + command + " the ‹color› ‹piece›");
	}

	/**
	 * main method for testing: creates a PentoCanvas that shows all tiles and the grid.
	 * @param args arguments are ignored
	 */
	public static void main(String args[]) {
	//	PropertyConfigurator.configure("log4j.properties");
		System.setProperty("inpro.tts.voice", "cmu-slt-hsmm");
		System.setProperty("inpro.tts.language", "en-US");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(new GreatEnglishPatternDemonstrator());
			}
		});
	}
	
	
}
