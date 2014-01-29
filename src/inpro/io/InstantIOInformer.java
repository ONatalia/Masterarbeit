package inpro.io;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.Namespace;
import org.instantreality.InstantIO.NetworkNode;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;

public class InstantIOInformer {

	static Logger log = Logger.getLogger(InstantIOInformer.class.getName());
	
	private static InstantIOInformer instance;
	private NetworkNode irNetworkNode;
    private HashMap<String,Namespace> namespaceMaps;	
    
    private boolean started = false;
	
	public static InstantIOInformer getInstance(){
		if (instance == null) {
			instance = new InstantIOInformer();
		}
		return instance;
	}
	
	public InstantIOInformer() {
		namespaceMaps = new HashMap<String,Namespace>();
		irNetworkNode = new NetworkNode();
		this.setPrefix("InproTK");
		this.setPort(54445);
		this.setServers("127.0.0.1:54444");
	}
	
	public void setPrefix(String prefix) {
		if (started) {
			log.warn("Cannot set prefix after InstantIO has been started!");
		}
		
		log.info("Setting InstantIO prefix: " + prefix);
		irNetworkNode.setPrefix(String.format("%s/{SlotLabel}", prefix));
	}
	
	public void setPort(int port) {
		if (started) {
			log.warn("Cannot set port after InstantIO has been started!");
		}
		log.info("Setting InstantIO port: " + port);
		irNetworkNode.setPort(port);
	}
	
	public void setServers(String servers) {
		if (started) {
			log.warn("Cannot set servers after InstantIO has been started!");
		}
		log.info("Setting InstantIO servers: " + servers);
		irNetworkNode.setServers(servers);
	}
	
	public void start() {
		if (started) return;
		log.info("Starting InstantIO...");
		irNetworkNode.setExportSlots(true);
		irNetworkNode.setImportSlots(false);
		Root.the().addNamespace(irNetworkNode);
		started = true;
	}
	
	/*
	 * Keeps track of the namespaces.
	 */
	public void addNamespace(String namespace) {
		log.info("Adding namespace: " + namespace);
		Namespace irNamespace = new Namespace();
		if (!namespaceMaps.containsKey(namespace)) {
			Root.the().addNamespace(namespace, irNamespace);
			namespaceMaps.put(namespace, irNamespace);
		}
	}
	
	/*
	 * This adds an outslot to a namespace, and returns an OutSlot object.
	 * It checks if the namespace exists, then adds the outslot to that namespace.
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
	
	
	/* 
	 * for testing
	 */
	public static void main(String[] args) {
		InstantIOInformer informer = InstantIOInformer.getInstance();
		informer.start();
		//informer.addNamespace("Comprehension");
		OutSlot s1 = informer.addOutSlot("QA", "Comprehension");
		OutSlot s2 = informer.addOutSlot("Thing", "Comprehension");
		s1.push("QA stuff pushed");
		s2.push("Thing stuff pushed");
	}
}
