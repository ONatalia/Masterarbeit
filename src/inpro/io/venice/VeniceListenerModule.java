package inpro.io.venice;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import rsb.Event;
import rsb.Handler;
import adapter.Listener;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import inpro.io.ListenerModule;

/**
 * @author casey
 *
 */
public class VeniceListenerModule extends ListenerModule implements Handler {

	static Logger log = Logger.getLogger(VeniceListenerModule.class.getName());
	
 	@S4String(defaultValue = "")
	public final static String ID_PROP = "id";	
 	
 	@S4String(defaultValue = "")
	public final static String SCOPE_PROP = "scope";	 	
	
	Listener<String> listener;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String scope = ps.getString(SCOPE_PROP);
		String id = ps.getString(ID_PROP);
		String fullScope = makeScope(scope);
		logger.info("Listening on scope: " + fullScope);
//		listener from the venice wrapper for RSB
		listener = new Listener<String>(this, fullScope);
		this.setID(id);
	}
	
	private String makeScope(String scope) {
		return "/" + scope + "/" ;
	}
	
	/* (non-Javadoc)
	 * @see rsb.Handler#internalNotify(rsb.Event)
	 * This method is called from RSB/venice when new data is received on a specified scope.
	 * 
	 */
	@Override
	public void internalNotify(Event e) {
		//example e.getScope(): /dsg/kinect
		//example e.getData():  TR
		System.out.println(e.getData().toString());
		process(e.getData().toString());
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

}
