package inpro.io;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.OutSlot;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

public class InstantIOInformerModule extends IUModule {

	static Logger log = Logger.getLogger(InstantIOInformerModule.class.getName());
	
 	@S4String(defaultValue = "InproTK")
	public final static String PREFIX_PROP = "prefix";	
 	
 	@S4Integer(defaultValue = 54445)
	public final static String PORT_PROP = "port";
 	
 	@S4String(defaultValue = "127.0.0.1:54444")
	public final static String SERVERS_PROP = "servers";
 	
 	@S4String(defaultValue = "namespace")
	public final static String NAMESPACE_PROP = "namespace";
 	
 	@S4String(defaultValue = "outslot")
	public final static String OUTSLOT_PROP = "outslot";

	private OutSlot outSlot;
	
 	
 	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		log.info("Setting up InstantIOInformerModule");
		String prefix = ps.getString(PREFIX_PROP);
		int port = ps.getInt(PORT_PROP);
		String servers = ps.getString(SERVERS_PROP);
		String outslot = ps.getString(OUTSLOT_PROP);
		String namespace = ps.getString(NAMESPACE_PROP);
		InstantIOInformer instance = InstantIOInformer.getInstance();
		instance.setPort(port);
		instance.setPrefix(prefix);
		instance.setServers(servers);
		instance.start();
		outSlot = instance.addOutSlot(outslot, namespace);
	}
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		for (EditMessage<? extends IU> edit : edits) {
			System.out.println("sending instantio data " + edit.getIU().toPayLoad());
			outSlot.push(edit.getIU().toPayLoad());
		}
		
	}

}
