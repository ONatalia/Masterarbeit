package inpro.io.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

public class XmlRpcInformer extends IUModule {
	
	XmlRpcClient client;
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	    try {
			config.setServerURL(new URL("http://127.0.0.1:9050"));
		
	    client = new XmlRpcClient();
	    client.setConfig(config);
	    
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
				client.execute("inprotk.push", params);
			} 
			catch (XmlRpcException e) {
				e.printStackTrace();
			}
		}
		
	}

}
