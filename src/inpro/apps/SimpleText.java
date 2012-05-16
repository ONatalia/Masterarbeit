package inpro.apps;

import inpro.apps.util.TextCommandLineParser;
import inpro.incremental.PushBuffer;
import inpro.incremental.eyetracker.CurrentEyeTrackingPoint;
import inpro.incremental.listener.FrameAwarePushBuffer;
import inpro.incremental.processor.CurrentASRHypothesis;
import inpro.incremental.processor.TextBasedFloorTracker;
import inpro.incremental.util.IUDocument;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;

/**
 * simple interactive (and non-interactive) ASR simulator for the inpro project
 * 
 * Words are added to the IU list (and sent off to CurrentASRHypothesis'
 * HypothesisChangeListeners) as you type, revoked when you delete them
 * and committed when you press enter (or click on the "commit" button).
 * 
 * Floor management (EoT-detection) is simulated by pressing enter twice 
 * in a row (effectively: pressing enter when the textfield is empty) or
 * by clicking the "EoT" button.
 * 
 * Notice: double-pressing enter and clicking on "EoT" is actually different,
 * because in the former case, all words will have been committed, while
 * in the latter case some words may be left uncommitted. Our system should
 * be able to cope with both cases (at least with EoT slightly preceding 
 * ASR-commit), and this is a way for us to test this.
 * 
 * 
 * TODO: implement revocation of words when running non-interactively.
 * specification more or less follows Verbmobil: an exclamation mark
 * is followed by the number of words to be revoked. !2 would revoke the 
 * two words preceding the exclamation mark.
 * 
 * @author timo
 *
 */

@SuppressWarnings("serial")
public class SimpleText extends JPanel implements ActionListener {

	private static final Logger logger = Logger.getLogger(SimpleText.class);

	@S4Component(type = CurrentASRHypothesis.class)
	public final static String PROP_CURRENT_HYPOTHESIS = "currentASRHypothesis";
	
	@S4Component(type = CurrentEyeTrackingPoint.class)
	public final static String PROP_CURRENT_EYETRACKING = "currentEyeTrackingPoint";		
	
	@S4ComponentList(type = FrameAwarePushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = CurrentASRHypothesis.PROP_HYP_CHANGE_LISTENERS;
	
	@S4ComponentList(type = FrameAwarePushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS_EYE = CurrentEyeTrackingPoint.PROP_HYP_CHANGE_LISTENERS;		

	@S4Component(type = TextBasedFloorTracker.class)
	public final static String PROP_FLOOR_MANAGER = "textBasedFloorTracker";

	@S4Component(type = TextBasedFloorTracker.Listener.class)
	public final static String PROP_FLOOR_MANAGER_LISTENERS = TextBasedFloorTracker.PROP_STATE_LISTENERS;

	IUDocument iuDocument;
	JTextField textField;
	
	SimpleText(TextBasedFloorTracker textBasedFloorTracker) {
		iuDocument = new IUDocument();
		textField = new JTextField(40);
		textField.setFont(new Font("Dialog", Font.BOLD, 24));
		textField.setDocument(iuDocument);
		textField.addActionListener(this);
		JButton commitButton = new JButton("Commit");
		commitButton.addActionListener(this);
		add(textField);
		add(commitButton);
		assert textBasedFloorTracker != null;
		assert textBasedFloorTracker.signalPanel != null;
		add(textBasedFloorTracker.signalPanel);
	}
	
	public void actionPerformed(ActionEvent ae) {
		iuDocument.commit();
		// hitting enter on empty lines results in an EoT-marker
		if (iuDocument.getLength() == 0) {
//			notifyFloorAvailable();
		}
		textField.requestFocusInWindow();
	}
	
	public static void createAndShowGUI(List<PushBuffer> hypListeners, TextBasedFloorTracker textBasedFloorTracker) {
		JFrame frame = new JFrame("SimpleText");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SimpleText contentPane = new SimpleText(textBasedFloorTracker);
        contentPane.iuDocument.setListeners(hypListeners);
        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        //Display the window.
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
	}
	
	public static void runFromReader(Reader reader, List<PushBuffer> hypListeners, TextBasedFloorTracker textBasedFloorTracker) throws IOException {
		IUDocument iuDocument = new IUDocument();
		iuDocument.setListeners(hypListeners);
		BufferedReader bReader = new BufferedReader(reader);
		String line;
		while ((line = bReader.readLine()) != null) {
			try {
				iuDocument.insertString(0, line, null);
			} catch (BadLocationException e) {
				logger.error("wow, this should really not happen (I thought I could always add a string at position 0)");
				e.printStackTrace();
			}
			iuDocument.commit();
			// empty lines results in an EoT-marker
			if ("".equals(line)) {
				textBasedFloorTracker.setEOT();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
        TextCommandLineParser clp = new TextCommandLineParser(args);
    	if (!clp.parsedSuccessfully()) { System.exit(1); } // abort on error
    	final ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
    	PropertySheet ps = cm.getPropertySheet(PROP_CURRENT_HYPOTHESIS);
    	final TextBasedFloorTracker textBasedFloorTracker = (TextBasedFloorTracker) cm.lookup(PROP_FLOOR_MANAGER);
    	final List<PushBuffer> hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
    	
    	if (clp.hasTextFromReader()) { // if we already know the text:
    		logger.info("running in non-interactive mode");
    		// run non-interactively
    		runFromReader(clp.getReader(), hypListeners, textBasedFloorTracker);
    		System.exit(0); //
    		
    	} 
    	else { // run interactively
    		// add hypothesis viewer 
    		if (clp.matchesOutputMode(TextCommandLineParser.CURRHYP_OUTPUT)) {
    			hypListeners.add((PushBuffer) cm.lookup("hypViewer"));
    		}
    		logger.info("running in interactive mode");
    		SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                createAndShowGUI(hypListeners, textBasedFloorTracker);	                
	            }
	        });
    	}
	}

}
