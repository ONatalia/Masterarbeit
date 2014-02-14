package inpro.io.instantio;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.Namespace;
import org.instantreality.InstantIO.NetworkNode;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;

/**
 * @author Casey Kennington
 */
public class InstantIOInformer {

	static Logger log = Logger.getLogger(InstantIOInformer.class.getName());
	
	private static InstantIOInformer instance;
	private NetworkNode irNetworkNode;
    private HashMap<String,Namespace> namespaceMaps;	
    private boolean started = false;
	
	/**
	 * Always use this method to get the singleton instance.
	 * 
	 * @return InstantIOInformer the singleton instance
	 */
	public static InstantIOInformer getInstance(){
		if (instance == null) {
			instance = new InstantIOInformer();
		}
		return instance;
	}
	
	/*
	 * Constructor should be private; access via the getInstance() singleton
	 * <p>
	 * sets the default server values initially; changed when set in the config file
	 */
	private InstantIOInformer() {
		namespaceMaps = new HashMap<String,Namespace>();
		irNetworkNode = new NetworkNode();
		this.setPrefix("InproTK");
		this.setPort(54445);
		this.setServers("127.0.0.1:54444");
	}
	
	/**
	 * 
	 * 
	 * @param prefix
	 */
	public void setPrefix(String prefix) {
		if (started) {
			log.warn("Cannot set prefix after InstantIO has been started!");
			return;
		}
		
		log.info("Setting InstantIO prefix: " + prefix);
		irNetworkNode.setPrefix(String.format("%s/{SlotLabel}", prefix));
	}
	
	
	/**
	 * Set the port of the InstantIO server.
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		if (started) {
			log.warn("Cannot set port after InstantIO has been started!");
			return;
		}
		log.info("Setting InstantIO port: " + port);
		irNetworkNode.setPort(port);
	}
	
	
	/**
	 * Set the InstantIO servers. 
	 * 
	 * @param servers
	 */
	public void setServers(String servers) {
		if (started) {
			log.warn("Cannot set servers after InstantIO has been started!");
			return;
		}
		log.info("Setting InstantIO servers: " + servers);
		irNetworkNode.setServers(servers);
	}
	
	
	/**
	 * Start the InstantIO service
	 */
	public void start() {
		if (started) {
			log.warn("Server already started.");
			return;
		}
		log.info("Starting InstantIO...");
		irNetworkNode.setExportSlots(true);
		irNetworkNode.setImportSlots(false);
		Root.the().addNamespace(irNetworkNode);
		started = true;
	}
	
	/**
	 * This will add a namespace to the set of already existing namespaces. Ignored if the namespace has already been used.
	 * 
	 * @param namespace
	 */
	public void addNamespace(String namespace) {
		log.info("Adding namespace: " + namespace);
		Namespace irNamespace = new Namespace();
		if (!namespaceMaps.containsKey(namespace)) {
			Root.the().addNamespace(namespace, irNamespace);
			namespaceMaps.put(namespace, irNamespace);
		}
	}
	
	/**
	 * This adds an outslot to a namespace, and returns an OutSlot object.
	 * It checks if the namespace exists, then adds the outslot to that namespace.
	 * 
	 * @param outSlot
	 * @param namespace
	 * @return OutSlot which is an InstantIO object; call .push() to send the data
	 */
	public OutSlot addOutSlot(String outSlot, String namespace) {
		log.info("Adding outslot: " + outSlot + " with namespace " + namespace);
		//first, add namespace if it doesn't exist
		if (!namespaceMaps.containsKey(namespace)) {
			addNamespace(namespace);
		}
		OutSlot irOut = new OutSlot(String.class);
		//add the outslot to the namespace
		namespaceMaps.get(namespace).addOutSlot(outSlot, irOut);
		namespaceMaps.get(namespace).addExternalRoute("*", "{NamespaceLabel}/{SlotLabel}");
		return irOut;
	}
	
}
