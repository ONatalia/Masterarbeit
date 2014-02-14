package inpro.io.instantio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
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
public class InstantIOListenerModule extends IUModule implements InSlot.Listener {
	
	static Logger log = Logger.getLogger(InstantIOListenerModule.class.getName());
	
	@S4String(defaultValue = "")
	public final static String INSLOT_PROP = "inslot";
	
	private String id;
	List<EditMessage<SensorIU>> edits;
	private SensorIU prevIU;

	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		log.info("Setting up InstantIOListenerModule");
		String inslot = ps.getString(INSLOT_PROP);
		this.setID(inslot);
		InstantIOListener.getInstance().addInSlotListener(inslot, this);
		System.out.println("instantIO listener listening...");
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		log.warn("This module does not accept anything on the left buffer.");
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

	/**
	 * Processes the data received from the InstantIO network on the specific namespace.
	 * 
	 * @param data which is the String received from the InstantIO network.
	 */
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
	public void startInSlot(InSlot arg0) {
		
	}

	@Override
	public void stopInSlot(InSlot arg0) {
		
	}

	/**
	 * @return String, the ID is the namespace ID, it is also copied into the SensorIU so one knows the source. 
	 */
	public String getID() {
		return id;
	}

	/**
	 * One can set the ID here. See getID() for other comments. 
	 * 
	 * @param id
	 */
	public void setID(String id) {
		this.id = id;
	}

}
