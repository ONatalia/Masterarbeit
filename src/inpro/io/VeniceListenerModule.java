package inpro.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import rsb.Event;
import rsb.Handler;
import adapter.Listener;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;

public class VeniceListenerModule extends IUModule implements Handler {

	static Logger log = Logger.getLogger(VeniceListenerModule.class.getName());
	
 	@S4String(defaultValue = "")
	public final static String ID_PROP = "id";	
 	
 	@S4String(defaultValue = "")
	public final static String SCOPE_PROP = "scope";	 	
 
	List<EditMessage<SensorIU>> edits;

	private SensorIU prevIU;
	
	private String id;
	Listener<String> listener;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String scope = ps.getString(SCOPE_PROP);
		String id = ps.getString(ID_PROP);
		String fullScope = makeScope(scope,id);
		logger.info("Listening on scope: " + fullScope);
		listener = new Listener<String>(this, fullScope);
		this.setID(ps.getString(ID_PROP));
	}
	
	private String makeScope(String scope, String id) {
		return "/" + scope + "/" + id;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rsb.Handler#internalNotify(rsb.Event)
	 * 
	 */

	@Override
	public void internalNotify(Event e) {
		//example e.getScope(): /dsg/kinect
		//example e.getData():  TR
		System.out.println("got new data " + e.getData().toString());
		process(e.getData().toString());
		
	}
	
	private void process(String data) {
			edits = new ArrayList<EditMessage<SensorIU>>();
			//create an incremental unit and put it onto the right buffer
			SensorIU iu = new SensorIU(data, this.getID());
			iu.setSameLevelLink(prevIU);
			edits.add(new EditMessage<SensorIU>(EditType.ADD, iu));
			prevIU = iu;
			//set to right buffer for the next module's left buffer
			rightBuffer.setBuffer(edits);
			super.notifyListeners();
	}
	

	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<? extends IU> edit : edits) {
			IU iu = edit.getIU();
			if (iu instanceof SensorIU) {
				SensorIU siu = (SensorIU) iu;
				if (siu.getSource().equals(this.getID())) {
					process(siu.getData());
				}
			}
		}
	}	
	
	public ArrayList<String> parseScopedString(String line) {
		
		//sometimes it has slashes at the beginning and end
		if (line.startsWith("/") && line.endsWith("/"))
			line = line.trim().substring(1,line.length()-1);
		
		ArrayList<String> splitString = new ArrayList<String>(Arrays.asList(line.split("/")));

		return splitString;
	}

	public VeniceListenerModule() {
		this("VeniceListenerModule");
	}
	
	public VeniceListenerModule(String type) {
		this.setID(type);
	}

	public String getID() {
		return id;
	}

	public void setID(String type) {
		this.id = type;
	}



}
