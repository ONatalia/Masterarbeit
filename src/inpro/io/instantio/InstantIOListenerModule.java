package inpro.io.instantio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.io.ListenerModule;
import inpro.io.SensorIU;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.InSlot;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * @author casey
 *
 */
public class InstantIOListenerModule extends ListenerModule implements InSlot.Listener {
	
	static Logger log = Logger.getLogger(InstantIOListenerModule.class.getName());
	
	@S4String(defaultValue = "")
	public final static String INSLOT_PROP = "inslot";
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		log.info("Setting up InstantIOListenerModule");
		String inslot = ps.getString(INSLOT_PROP);
		this.setID(inslot);
		InstantIOListener.getInstance().addInSlotListener(inslot, this);
		System.out.println("instantIO listener listening...");
	}

	/* (non-Javadoc)
	 * @see org.instantreality.InstantIO.InSlot.Listener#newData(org.instantreality.InstantIO.InSlot)
	 * 
	 * Anything received on the InstantIO network on the namespace for this object is sent through this method.
	 * 
	 */
	@Override
	public void newData(InSlot data) {
		try {
			process(data.pop().toString());
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void startInSlot(InSlot arg0) {
		
	}

	@Override
	public void stopInSlot(InSlot arg0) {
		
	}

}
