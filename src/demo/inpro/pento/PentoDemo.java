package demo.inpro.pento;

import inpro.apps.SimpleText;
import inpro.incremental.PushBuffer;
import inpro.incremental.processor.TextBasedFloorTracker;
import inpro.incremental.source.CurrentASRHypothesis;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.JFrame;
import javax.xml.bind.JAXBException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.server.PropertyHandlerMapping;

import work.inpro.gui.pentomino.january.GameCanvas;
import work.inpro.system.pentomino.Setting;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4String;

public class PentoDemo {
	
	private static final Logger logger = Logger.getLogger(PentoDemo.class);

	@S4Component(type = CurrentASRHypothesis.class)
	public final static String PROP_CURRENT_HYPOTHESIS = "currentASRHypothesis";

	@S4Component(type = TextBasedFloorTracker.class)
	public final static String PROP_FLOOR_MANAGER = "textBasedFloorTracker";

	@S4Component(type = TextBasedFloorTracker.Listener.class)
	public final static String PROP_FLOOR_MANAGER_LISTENERS = TextBasedFloorTracker.PROP_STATE_LISTENERS;
	
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = CurrentASRHypothesis.PROP_HYP_CHANGE_LISTENERS;
	
 	@S4String(defaultValue = "")
	public final static String PROP_SETTING_XML = "settingXML";	
	
	static ConfigurationManager cm;
	static PropertySheet ps;
	static TextBasedFloorTracker textBasedFloorTracker;
	static GameCanvas gameCanvas;
	static Setting setting;
	private static JFrame gameFrame = new JFrame();
	
	public static void main (String[] args) {
		
		try {
			BasicConfigurator.configure();
			cm = new ConfigurationManager(new File("src/demo/inpro/pento/config.xml").toURI().toURL());
	    	ps = cm.getPropertySheet(PROP_CURRENT_HYPOTHESIS);
	    	textBasedFloorTracker = (TextBasedFloorTracker) cm.lookup(PROP_FLOOR_MANAGER);
	    	final List<PushBuffer> hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
	    	PropertyHandlerMapping phm = new PropertyHandlerMapping();
			SimpleText.createAndShowGUI(hypListeners, textBasedFloorTracker);
			
			
			
			
		} 
		catch (PropertyException e) {
			e.printStackTrace();
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		} 


	}
	
}
