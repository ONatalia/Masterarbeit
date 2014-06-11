package inpro.io.venice;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import adapter.Informer;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

/**
 * @author casey
 *
 */
public class VeniceInformerModule extends IUModule {
	
	static Logger log = Logger.getLogger(VeniceInformerModule.class.getName());
	
 	@S4String(defaultValue = "")
	public final static String ID_PROP = "id";	
 	
 	@S4String(defaultValue = "")
	public final static String SCOPE_PROP = "scope";

	private Informer<String> informer;
 	private String id;	 	

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		String scope = ps.getString(SCOPE_PROP);
		String id = ps.getString(ID_PROP);
		String fullScope = makeScope(scope,id);
		logger.info("Informing on scope: " + fullScope);
//		this Informer comes from the DSG venice wrapper over RSB. 
		informer = new Informer<String>(fullScope);
		this.setID(ps.getString(ID_PROP));
	}
 	
	
	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
		//Simply put the payload of any IU onto the scope
		for (EditMessage<? extends IU> edit : edits) {
			informer.send(edit.getIU().toPayLoad());
		}
		
	}
	
	/**
	 * @param scope
	 * @param id
	 * @return scope used for RSB/venice
	 */
	private String makeScope(String scope, String id) {
		return "/" + scope + "/" + id;
	}
	
	/**
	 * @return id of the scope / module, not really used in the informer module
	 */
	public String getID() {
		return id;
	}

	/**
	 * @param id set the id of the scope / module
	 */
	public void setID(String id) {
		this.id = id;
	}


}
