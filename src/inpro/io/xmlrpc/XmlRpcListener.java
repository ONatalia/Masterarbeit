package inpro.io.xmlrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.io.SensorIU;

public class XmlRpcListener extends IUModule {
	
	List<EditMessage<SensorIU>> edits;

	private SensorIU prevIU;
	
	private String id;
	
	public boolean push(String data) {
		try { 
			process(data);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	

	private void process(String data) {
		edits = new ArrayList<EditMessage<SensorIU>>();
		//create an incremental unit and put it onto the right buffer
		SensorIU iu = new SensorIU(data, this.getID());
		iu.setSameLevelLink(prevIU);
		edits.add(new EditMessage<SensorIU>(EditType.ADD, iu));
		prevIU = iu;
		//set to right buffer for the next module's left buffer
		System.out.println("Sending data via XMLRPCLISTENER " + edits);
		rightBuffer.setBuffer(edits);
		super.notifyListeners();
	}


	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		WebServer web = new WebServer(9050);
		XmlRpcServer xml = web.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		try 
		{
			phm.addHandler("inprotk", XmlRpcListener.class);
			xml.setHandlerMapping(phm);
			System.out.println("Server running.");
			web.start();
		} 
		catch (XmlRpcException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
	}


	public String getID() {
		return id;
	}


	public void setID(String id) {
		this.id = id;
	}
	

}
