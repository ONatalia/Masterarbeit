package inpro.io.rsb;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import venice.lib.AbstractSlot;
import venice.lib.AbstractSlotListener;
import venice.lib.networkRSB.RSBNamespaceBuilder;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import inpro.io.ListenerModule;

/**
 * @author casey
 *
 */
public class RsbListenerModule extends ListenerModule implements AbstractSlotListener {

	static Logger log = Logger.getLogger(RsbListenerModule.class.getName());
	
 	@S4String(defaultValue = "")
	public final static String ID_PROP = "id";	
 	
 	@S4String(defaultValue = "")
	public final static String SCOPE_PROP = "scope";	 
 	
 	private String fullScope;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String scope = ps.getString(SCOPE_PROP);
		String id = ps.getString(ID_PROP);
		fullScope = makeScope(scope);
		logger.info("Listening on scope: " + fullScope);
//		listener from the venice wrapper for RSB
		
		AbstractSlot[] slots = new AbstractSlot[1];
		slots[0] = new AbstractSlot(fullScope, String.class);
		RSBNamespaceBuilder.initializeProtobuf();
		RSBNamespaceBuilder.initializeInSlots(slots);
		RSBNamespaceBuilder.setMasterInSlotListener(this);
		
		this.setID(id);
	}
	
	private String makeScope(String scope) {
		return scope;
	}
	
	/**
	 * Take data from the scope and split it up into an ArrayList
	 * 
	 * @param line
	 * @return list of data from the scope
	 */
	public ArrayList<String> parseScopedString(String line) {
		
		//sometimes it has slashes at the beginning and end
		if (line.startsWith("/") && line.endsWith("/"))
			line = line.trim().substring(1,line.length()-1);
		
		ArrayList<String> splitString = new ArrayList<String>(Arrays.asList(line.split("/")));

		return splitString;
	}

	/* (non-Javadoc)
	 * This method is called from RSB/venice when new data is received on a specified scope.
	 * 
	 */
	@Override
	public void newData(Object arg0, Class<?> arg1, String arg2) {
		process(arg0.toString());
	}

}
