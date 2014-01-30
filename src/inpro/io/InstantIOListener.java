package inpro.io;


import org.apache.log4j.Logger;
import org.instantreality.InstantIO.BufferedInSlot;
import org.instantreality.InstantIO.InSlot;
import org.instantreality.InstantIO.NetworkNode;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;
import org.instantreality.InstantIO.Namespace;


public class InstantIOListener implements Namespace.Listener {

	static Logger log = Logger.getLogger(InstantIOListener.class.getName());
	
	private NetworkNode node;
	private static InstantIOListener instance;
	
	/*
	 * ALWAYS access this via the singleton instance.
	 */
	public static InstantIOListener getInstance() {
		if (instance == null) {
			instance = new InstantIOListener();
		}
		return instance;
	}

	/* 
	 * Constructor should be private.
	 */
	private InstantIOListener() {
		node = new NetworkNode();
		node.setExportSlots(false);
		node.setImportSlots(true);
		Root.the().addNamespace(node);
		Root.the().addListener(this);
		
		//keep the listener alive, just in case InproTK doesn't have anything else to do. For the most part, this just sleeps. 
		new Thread() {
			public void run() {
				while (true) {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}		
			}
		}.start();
		
	}
	
	public void addInSlotListener(String inSlotName, InSlot.Listener listener) {
		InSlot inSlot = new BufferedInSlot(String.class, null, 80);
		Root.the().addInSlot(inSlotName, inSlot);
		inSlot.addListener(listener);
	}

	@Override
	public void inSlotAdded(Namespace arg0, String arg1, InSlot arg2) {
		
	}

	@Override
	public void inSlotRemoved(Namespace arg0, String arg1, InSlot arg2) {
		
	}

	@Override
	public void outSlotAdded(Namespace namespace, String outname, OutSlot out) {
		log.info("InstantIO Namespace detected: " + namespace + " with outname " 
				  + outname  + " using type " + out.getType());
	}

	@Override
	public void outSlotRemoved(Namespace arg0, String arg1, OutSlot arg2) {
		
	}

	@Override
	public void routeAdded(Namespace arg0, String arg1, String arg2) {
		
	}

	@Override
	public void routeRemoved(Namespace arg0, String arg1, String arg2) {
		
	}
	
}
