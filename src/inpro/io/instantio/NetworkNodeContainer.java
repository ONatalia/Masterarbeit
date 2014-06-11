package inpro.io.instantio;

import org.instantreality.InstantIO.NetworkNode;

public class NetworkNodeContainer {
	
	private static NetworkNode networkNode;
	
	public static NetworkNode getNetworkNode() {
		if (networkNode == null) 
			networkNode = new NetworkNode();
		return networkNode;
	}

}
