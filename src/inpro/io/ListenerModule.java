package inpro.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;

public class ListenerModule extends IUModule {
	
	List<EditMessage<SensorIU>> edits;

	private SensorIU prevIU;
	
	private String id;
	
	/**
	 * Take the data from the scope, put it into a SensorIU, and put it onto the right buffer.
	 * 
	 * @param data data to be put onto the right buffer
	 */
	protected void process(String data) {
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

	/* (non-Javadoc)
	 * @see inpro.incremental.IUModule#leftBufferUpdate(java.util.Collection, java.util.List)
	 * Putting something on this left buffer results in it being passed along to the right buffer if it is a SensorIU
	 */
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
	
	
	/**
	 * @return id of the module, this tells the SensorIU where the data came from
	 */
	public String getID() {
		return id;
	}

	
	/**
	 * Sets the id of the module.
	 * 
	 * @param id
	 */
	public void setID(String id) {
		this.id = id;
	}

}
