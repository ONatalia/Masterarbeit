package inpro.incremental.unit;

import sium.nlu.stat.Distribution;
import inpro.incremental.unit.IU;

public class SlotIU extends IU {
	
	private Distribution<String> distribution;
	private String name;
	private boolean head;
	
	public SlotIU(String name, Distribution<String> dist) {
		setName(name);
		setDistribution(dist);
	}

	public SlotIU() {
	}

	@Override
	public String toPayLoad() {
		if (getDistribution().isEmpty()) return String.format("[%s]", getName());
		return String.format("[%s %s]", getName(), getDistribution().getArgMax().getEntity());
	}
	
	public String toString() {
		return toPayLoad();
	}

	public Distribution<String> getDistribution() {
		return distribution;
	}


	public void setDistribution(Distribution<String> distribution) {
		this.distribution = distribution;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public void setIsHead(boolean head) {
		this.head = head;
	}
	
	public boolean isHead() {
		return this.head;
	}

}
