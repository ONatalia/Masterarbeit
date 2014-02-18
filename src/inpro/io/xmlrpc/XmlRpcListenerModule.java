package inpro.io.xmlrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.io.ListenerModule;
import inpro.io.SensorIU;

public class XmlRpcListenerModule extends ListenerModule {
	
	static Logger log = Logger.getLogger(XmlRpcListenerModule.class.getName());
	
 	@S4String(defaultValue = "")
	public final static String ID = "handler";
 	
 	@S4Integer(defaultValue = 9050)
	public final static String PORT = "port";	
	
	/**
	 * Method that receives the data. 
	 * 
	 * @param data
	 * @return　boolean this can pretty much be ignored, returns false if an error is thrown and caught here
	 */
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

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		int port = ps.getInt(PORT);
		this.setID(ps.getString(ID));
		WebServer web = new WebServer(port);
		XmlRpcServer xml = web.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		try {
			phm.addHandler(this.getID(), XmlRpcListenerModule.class);
			xml.setHandlerMapping(phm);
			log.info("XmlRpc Server Running on port " + port);
			web.start();
		} 
		catch (XmlRpcException e) {
			e.printStackTrace();
		} 
		catch (IOException e){
			e.printStackTrace();
		}
	}

}
