package inpro.io.instantio;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.OutSlot;

import venice.lib.AbstractSlot;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.networkIIO.IIONamespaceBuilder.Task;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

/**
 * @author casey
 *
 */
public class InstantIOInformerModule extends IUModule {

	static Logger log = Logger.getLogger(InstantIOInformerModule.class.getName());
	
 	@S4String(defaultValue = "InproTK")
	public final static String ID_PROP = "id";	

 	@S4String(defaultValue = "outslot")
	public final static String OUTSLOT_PROP = "outslot";
 	
 	private String fullScope;

	/* (non-Javadoc)
	 * @see inpro.incremental.IUModule#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		log.info("Setting up InstantIOInformerModule");
		String id = ps.getString(ID_PROP);
		String outslot = ps.getString(OUTSLOT_PROP);
		fullScope = id +"/"+ outslot;
		
		IIONamespaceBuilder.setTask(Task.READ);
		IIONamespaceBuilder.prepareNamespace("");
		
		AbstractSlot[] slots = new AbstractSlot[1];
		slots[0] = new AbstractSlot(fullScope, String.class);
		
		IIONamespaceBuilder.initializeOutSlots(slots);
	
	}
	
	/* (non-Javadoc)
	 * @see inpro.incremental.IUModule#leftBufferUpdate(java.util.Collection, java.util.List)
	 * 
	 * The left buffer just takes whatever comes onto the left buffer and pushes it to the 
	 * defined outslot.
	 * 
	 */
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		for (EditMessage<? extends IU> edit : edits) {
//			System.out.println("PUSHING TO INSTANTIO: " + edit);
			IIONamespaceBuilder.write(fullScope, edit.getIU().toPayLoad(), "");
		}
	}

}
