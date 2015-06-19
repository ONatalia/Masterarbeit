package inpro.io.instantio;

import java.util.ArrayList;

import inpro.io.ListenerModule;

import org.apache.log4j.Logger;

import venice.lib.AbstractSlot;
import venice.lib.AbstractSlotListener;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.networkIIO.SlotFlags;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * @author casey
 *
 */
public class InstantIOListenerModule extends ListenerModule implements AbstractSlotListener {
	
	static Logger log = Logger.getLogger(InstantIOListenerModule.class.getName());
	
	@S4String(defaultValue = "")
	public final static String INSLOT_PROP = "inslot";
	
 	@S4String(defaultValue = "")
	public final static String ID_PROP = "id";
 	
 	private String inSlot;
 	private String id;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		log.info("Setting up InstantIOListenerModule");
		inSlot = ps.getString(INSLOT_PROP);
		id = ps.getString(ID_PROP);
		IIONamespaceBuilder.setSlotFlags(new SlotFlags(true, true));
		IIONamespaceBuilder.prepareNamespace(id);
		
		ArrayList<AbstractSlot> slots = new ArrayList<AbstractSlot>();
		AbstractSlot slot = new AbstractSlot();
		slot.setLabel(inSlot);
		slot.setType(String.class);
		slot.setNamespace(id);
		slots.add(slot);
		IIONamespaceBuilder.initializeInSlots(slots);
		
		IIONamespaceBuilder.setMasterInSlotListener(this);
		
		this.setID(ps.getString(ID_PROP));
	}

	/* (non-Javadoc)
	 * Anything received on the InstantIO network on the namespace for this object is sent through this method.
	 * 
	 */
	@Override
	public void newData(Object arg0, Class<?> arg1, String arg2) {
		process(arg0.toString());		
	}

}
