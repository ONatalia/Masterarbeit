package inpro.io.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

public class XmlRpcInformerModule extends IUModule {
	
	static Logger log = Logger.getLogger(XmlRpcInformerModule.class.getName());
	
	@S4String(defaultValue = "9050")
	public final static String PORT = "port";

	@S4String(defaultValue = "inprotk")
	public final static String HANDLER = "handler";
	private String handler;
	
	@S4String(defaultValue = "http://127.0.0.1")
	public final static String HOST = "host";
	private String host;
	
	XmlRpcClient client;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
//		set up the rpc client
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		this.setHandler(ps.getString(HANDLER));
		String port = ps.getString(PORT);
	    try {
			config.setServerURL(new URL(host + port));
			client = new XmlRpcClient();
			client.setConfig(config);
			log.info("Setting up XmlRpcListener at " + host + "on port " + port);
	    } 
	    catch (MalformedURLException e) {
			e.printStackTrace();
		} 
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		for (EditMessage<? extends IU> edit : edits) {
			Object[] params = new Object[]{edit.getIU().toPayLoad()};
			try {
				client.execute(handler+".push", params);
			} 
			catch (XmlRpcException e) {
				e.printStackTrace();
			}
		}
		
	}

	/**
	 * @return handler of the module
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * @param handler which is the handler for this XmlRpc server that clients need to connect to
	 */
	public void setHandler(String handler) {
		log.debug("setting handler: " + handler);
		this.handler = handler;
	}

}
