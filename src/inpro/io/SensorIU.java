package inpro.io;

import inpro.incremental.unit.IU;

/**
 * IU that carries data from InstantIO or venice/RSB data sources. 
 * 
 * @author casey
 *
 */
public class SensorIU extends IU {
	
	private String data;
	private String source;
	private String scope;
	
	/**
	 * @param data actual payload
	 * @param source where the data/payload came from (name of namespace or scope)
	 */
	public SensorIU(String data, String source) {
		this.data = data;
		this.setSource(source);
	}
	
	
	public SensorIU(String data, String source, String scope) {
		this(data, source);
		this.setScope(scope);
	}
	
	

	@Override
	public String toPayLoad() {
		return data.toString();
	}

	public String getData() {
		return this.data;
	}


	public String getSource() {
		return source;
	}


	public void setSource(String source) {
		this.source = source;
	}


	public String getScope() {
		return scope;
	}


	public void setScope(String scope) {
		this.scope = scope;
	}

}
