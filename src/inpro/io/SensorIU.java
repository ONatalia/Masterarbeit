package inpro.io;

import inpro.incremental.unit.IU;

public class SensorIU extends IU {
	
	private String data;
	private String source;
	
	public SensorIU(String data, String source) {
		this.data = data;
		this.setSource(source);
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

}
