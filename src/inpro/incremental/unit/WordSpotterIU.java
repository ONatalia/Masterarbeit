package inpro.incremental.unit;

import inpro.incremental.unit.IU;

public class WordSpotterIU extends IU {

	private String scope;

	public WordSpotterIU(String scope) {
		this.setScope(scope);
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public String toPayLoad() {
		return this.getScope();
	}

}
